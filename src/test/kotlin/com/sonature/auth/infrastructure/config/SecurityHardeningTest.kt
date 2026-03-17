package com.sonature.auth.infrastructure.config

import com.sonature.auth.infrastructure.security.RateLimitFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * Sprint 3.6 Security Hardening Integration Tests
 *
 * T001: Verifies that /api/v1/tenants/[slug] requires authentication
 * T002/T003: Verifies issuer and JWK settings from configuration
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var authorizationServerSettings: AuthorizationServerSettings

    @Autowired
    private lateinit var rateLimitFilter: RateLimitFilter

    @Value("\${auth.oauth2.issuer}")
    private lateinit var configuredIssuer: String

    @BeforeEach
    fun resetRateLimits() {
        rateLimitFilter.clearBuckets()
    }

    // T001: Tenant API authentication enforcement

    @Nested
    inner class TenantApiAuthEnforcement {

        @Test
        fun `GET tenants without auth should return 401`() {
            mockMvc.get("/api/v1/tenants/some-slug")
                .andExpect {
                    status { isUnauthorized() }
                }
        }

        @Test
        fun `GET tenants members without auth should return 401`() {
            mockMvc.get("/api/v1/tenants/some-slug/members")
                .andExpect {
                    status { isUnauthorized() }
                }
        }

        @Test
        fun `POST tenants without auth should return 401`() {
            mockMvc.post("/api/v1/tenants") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":"Test","slug":"test-slug"}"""
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `POST jwt endpoints should remain publicly accessible`() {
            // JWT endpoints are public: a valid request reaches the handler (200, not 401)
            mockMvc.post("/api/v1/jwt/issue") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"subject":"user-id"}"""
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        fun `auth endpoints should remain publicly accessible`() {
            // Auth signup with invalid email returns 400 (handler reached, not blocked by Spring Security)
            mockMvc.post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"email":"not-an-email","password":"short"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    // T002 / T003: Externalized configuration

    @Nested
    inner class ExternalizedConfiguration {

        @Test
        fun `authorization server settings should use configured issuer`() {
            assertEquals(configuredIssuer, authorizationServerSettings.issuer)
        }

        @Test
        fun `JWK endpoint should be accessible`() {
            mockMvc.get("/.well-known/oauth-authorization-server")
                .andExpect {
                    status { isOk() }
                }
        }

        @Test
        fun `configured issuer should not be blank`() {
            assertNotNull(configuredIssuer)
            assertEquals(true, configuredIssuer.isNotBlank())
        }
    }
}
