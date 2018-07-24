package forex.domain

import io.circe._
import io.circe.generic.extras.wrapped._
import io.circe.java8.time._
import java.time.{Instant, OffsetDateTime, ZoneId}

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val encoder: Encoder[Timestamp] =
    deriveUnwrappedEncoder[Timestamp]

  implicit def fromEpoch(epoch: Long) =
    Timestamp(OffsetDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault()))
}
