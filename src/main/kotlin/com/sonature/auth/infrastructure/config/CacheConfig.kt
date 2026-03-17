package com.sonature.auth.infrastructure.config

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/**
 * Caffeine-based cache configuration (ADR-005).
 *
 * Caches:
 *   - tenantSlugCache: slug → TenantEntity, TTL 5 min, max 100 entries
 */
@EnableCaching
@Configuration
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val manager = CaffeineCacheManager(TENANT_SLUG_CACHE)
        manager.setCaffeine(
            Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
        )
        return manager
    }

    companion object {
        const val TENANT_SLUG_CACHE = "tenantSlugCache"
    }
}
