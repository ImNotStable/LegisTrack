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
import org.springframework.cache.interceptor.KeyGenerator

class StructuredKeyGenerator : KeyGenerator {
    override fun generate(target: Any, method: java.lang.reflect.Method, vararg params: Any?): Any {
        val className = target.javaClass.simpleName
        val methodName = method.name
        val serializedParams =
            params.joinToString(separator = "|") { param ->
                when (param) {
                    null -> "null"
                    is Iterable<*> -> param.joinToString(prefix = "[", postfix = "]", separator = ",") { it?.toString() ?: "null" }
                    is Array<*> -> param.joinToString(prefix = "[", postfix = "]", separator = ",") { it?.toString() ?: "null" }
                    else -> param.toString()
                }
            }
        return "$className.$methodName:$serializedParams"
    }
}

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
                // Congress caches
                "congress-bills" to configuration.entryTtl(Duration.ofMinutes(30)),
                "congress-bill-details" to configuration.entryTtl(Duration.ofHours(2)),
                "congress-cosponsors" to configuration.entryTtl(Duration.ofHours(6)),
                "congress-actions" to configuration.entryTtl(Duration.ofHours(6)),
                "congress-amendments" to configuration.entryTtl(Duration.ofHours(6)),
                "congress-amendment-details" to configuration.entryTtl(Duration.ofHours(12)),
                "congress-summaries" to configuration.entryTtl(Duration.ofHours(12)),
                "congress-members" to configuration.entryTtl(Duration.ofHours(6)),
                "congress-member-details" to configuration.entryTtl(Duration.ofHours(12)),
                "congress-reports" to configuration.entryTtl(Duration.ofHours(6)),
                "congress-nominations" to configuration.entryTtl(Duration.ofHours(6)),
                "congress-treaties" to configuration.entryTtl(Duration.ofHours(6)),
                "congress-text-versions" to configuration.entryTtl(Duration.ofHours(6)),
                // GovInfo caches
                "govinfo-collections" to configuration.entryTtl(Duration.ofHours(24)),
                "govinfo-packages" to configuration.entryTtl(Duration.ofHours(6)),
                "govinfo-package-details" to configuration.entryTtl(Duration.ofHours(12)),
                "govinfo-granules" to configuration.entryTtl(Duration.ofHours(6)),
                "govinfo-granule-details" to configuration.entryTtl(Duration.ofHours(12)),
                "govinfo-published" to configuration.entryTtl(Duration.ofHours(6)),
                "govinfo-related" to configuration.entryTtl(Duration.ofHours(6)),
                "govinfo-search" to configuration.entryTtl(Duration.ofMinutes(15)),
                "govinfo-bills" to configuration.entryTtl(Duration.ofHours(6)),
                "govinfo-bill-status" to configuration.entryTtl(Duration.ofHours(6)),
            )

        return RedisCacheManager
            .builder(redisConnectionFactory)
            .cacheDefaults(configuration)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build()
    }

    @Bean
    fun keyGenerator(): KeyGenerator = StructuredKeyGenerator()
}
