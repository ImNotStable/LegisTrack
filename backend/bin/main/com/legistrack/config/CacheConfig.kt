package com.legistrack.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory): CacheManager {
        val configuration =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // Default TTL of 1 hour
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues()

        val cacheConfigurations =
            mapOf(
                "congress-bills" to configuration.entryTtl(Duration.ofMinutes(30)),
                "congress-bill-details" to configuration.entryTtl(Duration.ofHours(2)),
                "congress-cosponsors" to configuration.entryTtl(Duration.ofHours(6)),
                "congress-actions" to configuration.entryTtl(Duration.ofHours(6)),
            )

        return RedisCacheManager
            .builder(redisConnectionFactory)
            .cacheDefaults(configuration)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }
}
