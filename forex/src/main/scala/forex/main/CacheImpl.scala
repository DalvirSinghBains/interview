package forex.main

import forex.config.{ApplicationConfig, CacheConfig}
import forex.domain.Rate
import org.zalando.grafter.macros.readerOf
import scalacache.caffeine.CaffeineCache

import scala.concurrent.duration.FiniteDuration

@readerOf[ApplicationConfig]
case class CacheImpl(cacheConfig: CacheConfig) {
  implicit val ttl: FiniteDuration = cacheConfig.ttl
  implicit val cache : scalacache.Cache[Map[Rate.Pair,Rate]] =
    CaffeineCache[Map[Rate.Pair, Rate]]
}
