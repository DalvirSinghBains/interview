package forex.services.oneforge

import forex.domain.Currency._
import forex.domain._
import forex.services.oneforge.Error.System
import io.circe
import org.http4s.circe._
import io.circe.Json
import monix.eval.Task._
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import org.http4s.Uri
import org.http4s.client.blaze.PooledHttp1Client


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
    fromTask(deferFuture(fetchPrice(pair).unsafeRunAsyncFuture()))
  }
}

object OneForgeLiveImpl {

  import io.circe.generic.semiauto._

  case class ServiceConfig(baseUrl: String, apiUrl: String, apiKey: String)

  // todo inject from config
  implicit val conf: ServiceConfig = ServiceConfig("https://forex.1forge.com", "/1.0.3/convert", "iLdNdCDYiLD2Z7Kgg6FbBtmtHD7iPUQd")

  case class Response(value: Double, text: String, timestamp: Long)

  implicit val decoder: circe.Decoder[Response] = deriveDecoder[Response]

  import cats.implicits._

  def uri(pair: Rate.Pair)(implicit conf: ServiceConfig) =
    Uri.unsafeFromString(conf.baseUrl).withPath(conf.apiUrl)
      .withQueryParam("from",     pair.from.show)
      .withQueryParam("to",       pair.to.show)
      .withQueryParam("quantity", 100.show)
      .withQueryParam("api_key",  conf.apiKey)

  def fetchPrice(pair: Rate.Pair): fs2.Task[Either[Error, Rate]] =
    PooledHttp1Client().expect[Json](uri(pair)).map(_.as[Response])
      .map {
        _.toTry.toEither.map {
          case Response(value, _, timestamp) => Rate(pair, Price(value), timestamp)
        }.left.map { e => System(e) }
      }
}
