package forex.services.client

import forex.config.OneForgeConfig
import forex.domain.{Currency, Price, Rate}
import io.circe
import io.circe.Decoder
import org.http4s.Uri
import cats.implicits._

object Protocol {
  private val validCurrencyPairs: List[Rate.Pair] = {
    for {
      a <- Currency.supportedCurrencies.toList
      b <- Currency.supportedCurrencies.toList if a != b
    } yield Rate.Pair(a, b)
  }

  def toQueryParam(list: List[Rate.Pair]) = list.map { p => p.from.show + p.to.show }.mkString(",")

  def quotesUri(implicit conf: OneForgeConfig) =
    Uri.unsafeFromString(conf.baseUrl).withPath(conf.quotesUrl)
      .withQueryParam("pairs", toQueryParam(validCurrencyPairs))
      .withQueryParam("api_key", conf.apiKey)


  case class QuotesResponse(pair: Rate.Pair, price: BigDecimal, timestamp: Long) {
    def toRate = Rate(pair, Price(price), timestamp)
  }

  implicit val qDecoder: circe.Decoder[QuotesResponse] =
    Decoder.forProduct3[String, BigDecimal, Long, QuotesResponse]("symbol", "price", "timestamp") {
      case (symbolStr, price, timeStamp) =>
        QuotesResponse(Rate.Pair(Currency.fromString(symbolStr.take(3)), Currency.fromString(symbolStr.drop(3))), price, timeStamp)
    }

}
