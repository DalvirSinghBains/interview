package forex.services.oneforge

import forex.domain.Currency._
import forex.domain._
import forex.services.oneforge.Error.System
import fs2.Task
import io.circe
import org.http4s.circe._
import io.circe.{Decoder, Json}
import monix.eval.Task._
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import org.http4s.Uri
import org.http4s.client.blaze.PooledHttp1Client
import scalacache.caffeine.CaffeineCache

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Success, Try}



object Interpreters {
  def dummy[R](implicit m1: _task[R]): Algebra[Eff[R, ?]] = new Dummy[R]

  def live[R](implicit m1: _task[R]): Algebra[Eff[R, ?]] = new Live[R]
}

final class Dummy[R] private[oneforge](implicit m1: _task[R]) extends Algebra[Eff[R, ?]] {
  override def get(pair: Rate.Pair): Eff[R, Error Either Rate] = for {
    result ← fromTask(monix.eval.Task.now(Rate(pair, Price(BigDecimal(100)), Timestamp.now)))
  } yield Right(result)
}

final class Live[R] private[oneforge](implicit m1: _task[R]) extends Algebra[Eff[R, ?]] {
  override def get(pair: Rate.Pair): Eff[R, Error Either Rate] = {
    import OneForgeLiveImpl._
    implicit val cahceDuration: Duration = 5.minutes
    fromTask(deferFuture(fetchRate(pair))).map[Error Either Rate](Right(_))
  }
}

object OneForgeLiveImpl {

  import io.circe.generic.semiauto._

  case class ServiceConfig(baseUrl: String, convertUrl: String, quotesUrl: String, apiKey: String)
  import scalacache._

  // todo inject from config
  implicit val conf: ServiceConfig =
    ServiceConfig("https://forex.1forge.com", "/1.0.3/convert", "/1.0.3/quotes", "iLdNdCDYiLD2Z7Kgg6FbBtmtHD7iPUQd")

  case class Response(value: Double, timestamp: Long)

  implicit val decoder: circe.Decoder[Response] = deriveDecoder[Response]

  case class QuotesResponse(pair: Rate.Pair, price: BigDecimal, timestamp: Long)
  {
    def toRate = Rate(pair, Price(price), timestamp)
  }

  implicit val qDecoder: circe.Decoder[QuotesResponse] =
    Decoder.forProduct3[String, BigDecimal, Long, QuotesResponse]("symbol", "price", "timestamp") {
      case (symbolStr, price, timeStamp) =>
        QuotesResponse(Rate.Pair(Currency.fromString(symbolStr.take(3)), Currency.fromString(symbolStr.drop(3))), price, timeStamp)
    }

  import cats.implicits._

  implicit val cache: scalacache.Cache[Map[Rate.Pair, Rate]] = CaffeineCache[Map[Rate.Pair, Rate]]

  val validCurrencyPairs: List[Rate.Pair] = {
    for {
      a <- Currency.supportedCurrencies.toList
      b <- Currency.supportedCurrencies.toList if a != b
    } yield Rate.Pair(a, b)
  }

  def toQueryParam(list: List[Rate.Pair]) = list.map { p => p.from.show + p.to.show }.mkString(",")

  def quotesUri(pairs: List[Rate.Pair])(implicit config: ServiceConfig) =
    Uri.unsafeFromString(conf.baseUrl).withPath(conf.quotesUrl)
      .withQueryParam("pairs", toQueryParam(pairs))
      .withQueryParam("api_key",  conf.apiKey)

  def convertUri(pair: Rate.Pair)(implicit conf: ServiceConfig) =
    Uri.unsafeFromString(conf.baseUrl).withPath(conf.convertUrl)
      .withQueryParam("from",     pair.from.show)
      .withQueryParam("to",       pair.to.show)
      .withQueryParam("quantity", 1.show)
      .withQueryParam("api_key",  conf.apiKey)

  def fetchRate(pair: Rate.Pair)(implicit duration: Duration): Future[Rate] = {
    import scalacache.modes.scalaFuture._
    import scala.concurrent.ExecutionContext.Implicits.global

    cachingF[Future, Map[Rate.Pair, Rate]]("fetch-all")(Some(duration)) {
      fetchAllQuotes().flatMap {
        case Right(e) => Task.now(e)
        case Left(t) => Task.fail(t)
      }.unsafeRunAsyncFuture()
    } map (_.apply(pair))
  }

  def fetchAllQuotes(): Task[Either[Error, Map[Rate.Pair, Rate]]] = {
    PooledHttp1Client().expect[Json](quotesUri(validCurrencyPairs)).map(_.as[List[QuotesResponse]])
      .map {
        _.toTry.map {
          quotes => quotes.map(_.toRate).groupBy(_.pair).mapValues(_.head)
        }.toEither.left.map(System)
      }
  }

  def fetchConversionRate(pair: Rate.Pair): fs2.Task[Either[Error, Rate]] =
    PooledHttp1Client().expect[Json](convertUri(pair)).map(_.as[Response])
      .map {
        _.toTry.toEither.map {
          case Response(value, timestamp) => Rate(pair, Price(value), timestamp)
        }.left.map(System)
      }

}
