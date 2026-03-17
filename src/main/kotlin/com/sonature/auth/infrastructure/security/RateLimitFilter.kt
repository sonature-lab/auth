package com.sonature.auth.infrastructure.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * IP-based rate limiting filter using Bucket4j in-memory buckets.
 * ADR-004: Per-IP limits for sensitive endpoints.
 */
@Component
class RateLimitFilter : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI
        val limit = resolveLimit(path)

        if (limit != null) {
            val ip = resolveClientIp(request)
            val key = ip + "::" + limit.pathKey
            val bucket = buckets.computeIfAbsent(key) { createBucket(limit.capacity, limit.periodSeconds) }

            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded: ip={}, path={}", ip, path)
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.setHeader("Retry-After", limit.periodSeconds.toString())
                response.contentType = "application/json"
                val body = "{\"status\":429,\"error\":\"Too Many Requests\"," +
                    "\"message\":\"Rate limit exceeded. Retry after ${limit.periodSeconds}s.\"}"
                response.writer.write(body)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    /** Clears all bucket state. Intended for testing only. */
    internal fun clearBuckets() {
        buckets.clear()
    }

    private fun resolveLimit(path: String): RateLimit? = LIMITS.firstOrNull { path.matches(it.pattern) }

    private fun resolveClientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        return if (!forwardedFor.isNullOrBlank()) {
            forwardedFor.split(",").first().trim()
        } else {
            request.remoteAddr ?: "unknown"
        }
    }

    private fun createBucket(capacity: Long, periodSeconds: Long): Bucket {
        val bandwidth = Bandwidth.builder()
            .capacity(capacity)
            .refillGreedy(capacity, Duration.ofSeconds(periodSeconds))
            .build()
        return Bucket.builder().addLimit(bandwidth).build()
    }

    private data class RateLimit(
        val pathKey: String,
        val pattern: Regex,
        val capacity: Long,
        val periodSeconds: Long
    )

    companion object {
        private val LIMITS = listOf(
            RateLimit(
                pathKey = "jwt-issue",
                pattern = Regex("/api/v1/jwt/issue-pair"),
                capacity = 10L,
                periodSeconds = 60L
            ),
            RateLimit(
                pathKey = "jwt-refresh",
                pattern = Regex("/api/v1/jwt/refresh"),
                capacity = 20L,
                periodSeconds = 60L
            ),
            RateLimit(
                pathKey = "auth",
                pattern = Regex("/api/v1/auth/.*"),
                capacity = 10L,
                periodSeconds = 60L
            )
        )
    }
}
