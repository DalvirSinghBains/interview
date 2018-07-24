package forex.services.oneforge

import forex.config.OneForgeConfig
import forex.domain._
import forex.services.oneforge.Error.System
import fs2.Task
import org.http4s.circe._
import io.circe.Json
import monix.eval.Task._
import org.atnos.eff._
import org.atnos.eff.addon.monix.task._
import org.http4s.client.blaze.PooledHttp1Client
import scalacache._
import OneForgeLiveImpl._
import forex.main.CacheImpl

import scala.concurrent.Future
import scala.concurrent.duration._
import scalacache.modes.scalaFuture._
import scala.concurrent.ExecutionContext.Implicits.global



object Interpreters {
  def dummy[R](implicit m1: _task[R]): Algebra[Eff[R, ?]] = new Dummy[R]

  def live[R](forgeConfig: OneForgeConfig, cache: CacheImpl)(implicit m1: _task[R]): Algebra[Eff[R, ?]] =
    new OneForgeLiveImpl[R](forgeConfig, cache)
}

final class Dummy[R] private[oneforge](implicit m1: _task[R]) extends Algebra[Eff[R, ?]] {
  override def get(pair: Rate.Pair): Eff[R, Error Either Rate] = for {
    result ← fromTask(monix.eval.Task.now(Rate(pair, Price(BigDecimal(100)), Timestamp.now)))
  } yield Right(result)
}

final class OneForgeLiveImpl[R] private[oneforge](forgeConfig: OneForgeConfig, cacheImpl: CacheImpl)(implicit m1: _task[R]) extends Algebra[Eff[R, ?]] {
  override def get(pair: Rate.Pair): Eff[R, Error Either Rate] = {
    import cacheImpl._
    implicit val fC: OneForgeConfig = forgeConfig

    fromTask(deferFuture(fetchRateCached(pair))).map[Error Either Rate](Right(_))
  }
}

object OneForgeLiveImpl {
  import forex.services.client.Protocol._

 def fetchRateCached(pair: Rate.Pair)(implicit ttl: Duration,
                                      cache: scalacache.Cache[Map[Rate.Pair, Rate]],
                                      forgeConfig: OneForgeConfig): Future[Rate] = {


    cachingF[Future, Map[Rate.Pair, Rate]]("fetch-all")(Some(ttl)) {
      fetchAllQuotes().flatMap {
        case Right(e) => Task.now(e)
        case Left(t) => Task.fail(t)
      }.unsafeRunAsyncFuture()
    } map (_.apply(pair))
  }

  def fetchAllQuotes()(implicit conf: OneForgeConfig): Task[Either[Error, Map[Rate.Pair, Rate]]] = {
    PooledHttp1Client().expect[Json](quotesUri).map(_.as[List[QuotesResponse]])
      .map {
        _.toTry.map {
          quotes => quotes.map(_.toRate).groupBy(_.pair).mapValues(_.head)
        }.toEither.left.map(System)
      }
  }

}
