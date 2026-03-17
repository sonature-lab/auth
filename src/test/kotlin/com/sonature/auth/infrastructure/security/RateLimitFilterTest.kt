package com.sonature.auth.infrastructure.security

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import jakarta.servlet.FilterChain

class RateLimitFilterTest {

    private lateinit var filter: RateLimitFilter
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        filter = RateLimitFilter()
        filterChain = mockk(relaxed = true)
    }

    @Test
    fun `should pass requests under the rate limit`() {
        val request = MockHttpServletRequest("POST", "/api/v1/jwt/issue-pair").apply {
            remoteAddr = "192.168.0.1"
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, filterChain)

        assertEquals(200, response.status)
        verify(exactly = 1) { filterChain.doFilter(request, response) }
    }

    @Test
    fun `should return 429 when issue-pair limit exceeded (10 per min)`() {
        val ip = "10.0.0.1"
        val path = "/api/v1/jwt/issue-pair"

        // Exhaust all 10 tokens
        repeat(10) { seq ->
            val req = MockHttpServletRequest("POST", path).apply { remoteAddr = ip }
            val res = MockHttpServletResponse()
            filter.doFilter(req, res, filterChain)
            assertEquals(200, res.status, "Request $seq should pass")
        }

        // 11th request should be rejected
        val req = MockHttpServletRequest("POST", path).apply { remoteAddr = ip }
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, filterChain)

        assertEquals(429, res.status)
        assertEquals("60", res.getHeader("Retry-After"))
    }

    @Test
    fun `should return 429 when auth limit exceeded (10 per min)`() {
        val ip = "10.0.0.2"
        val path = "/api/v1/auth/login"

        repeat(10) { seq ->
            val req = MockHttpServletRequest("POST", path).apply { remoteAddr = ip }
            val res = MockHttpServletResponse()
            filter.doFilter(req, res, filterChain)
            assertEquals(200, res.status, "Request $seq should pass")
        }

        val req = MockHttpServletRequest("POST", path).apply { remoteAddr = ip }
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, filterChain)

        assertEquals(429, res.status)
    }

    @Test
    fun `should return 429 when refresh limit exceeded (20 per min)`() {
        val ip = "10.0.0.3"
        val path = "/api/v1/jwt/refresh"

        repeat(20) { seq ->
            val req = MockHttpServletRequest("POST", path).apply { remoteAddr = ip }
            val res = MockHttpServletResponse()
            filter.doFilter(req, res, filterChain)
            assertEquals(200, res.status, "Request $seq should pass")
        }

        val req = MockHttpServletRequest("POST", path).apply { remoteAddr = ip }
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, filterChain)

        assertEquals(429, res.status)
    }

    @Test
    fun `should track rate limits independently per IP`() {
        val path = "/api/v1/jwt/issue-pair"
        val ip1 = "172.16.0.1"
        val ip2 = "172.16.0.2"

        // Exhaust ip1
        repeat(10) {
            val req = MockHttpServletRequest("POST", path).apply { remoteAddr = ip1 }
            filter.doFilter(req, MockHttpServletResponse(), filterChain)
        }

        // ip2 should still pass on first request
        val req2 = MockHttpServletRequest("POST", path).apply { remoteAddr = ip2 }
        val res2 = MockHttpServletResponse()
        filter.doFilter(req2, res2, filterChain)

        assertEquals(200, res2.status, "Different IP should have independent bucket")

        // ip1 should be blocked
        val req1 = MockHttpServletRequest("POST", path).apply { remoteAddr = ip1 }
        val res1 = MockHttpServletResponse()
        filter.doFilter(req1, res1, filterChain)

        assertEquals(429, res1.status)
    }

    @Test
    fun `should pass requests to non-rate-limited paths without restriction`() {
        val ip = "10.0.0.10"
        val path = "/api/v1/tenants/my-tenant/members"

        repeat(50) { seq ->
            val req = MockHttpServletRequest("GET", path).apply { remoteAddr = ip }
            val res = MockHttpServletResponse()
            filter.doFilter(req, res, filterChain)
            assertEquals(200, res.status, "Non-limited path request $seq should always pass")
        }
    }

    @Test
    fun `should use X-Forwarded-For header when present`() {
        val path = "/api/v1/jwt/issue-pair"
        val forwardedIp = "203.0.113.5"

        repeat(10) {
            val req = MockHttpServletRequest("POST", path).apply {
                remoteAddr = "10.0.0.1"
                addHeader("X-Forwarded-For", "$forwardedIp, 10.0.0.1")
            }
            filter.doFilter(req, MockHttpServletResponse(), filterChain)
        }

        // Should be limited based on the forwarded IP
        val req = MockHttpServletRequest("POST", path).apply {
            remoteAddr = "10.0.0.1"
            addHeader("X-Forwarded-For", "$forwardedIp, 10.0.0.1")
        }
        val res = MockHttpServletResponse()
        filter.doFilter(req, res, filterChain)

        assertEquals(429, res.status)
    }
}
