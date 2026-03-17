package com.sonature.auth.infrastructure.config

import com.sonature.auth.api.v1.auth.dto.SignupRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.sonature.auth.infrastructure.security.RateLimitFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OAuth2AuthorizationServerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var rateLimitFilter: RateLimitFilter

    @BeforeEach
    fun setUp() {
        rateLimitFilter.clearBuckets()
        // Create a test user for login
        val request = SignupRequest(
            email = "oauth-test@example.com",
            password = "password123",
            name = "OAuth Test User"
        )
        try {
            mockMvc.post("/api/v1/auth/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
            }
        } catch (_: Exception) {
            // User may already exist from previous test
        }
    }

    @Test
    fun `OAuth2 well-known endpoint should be accessible`() {
        mockMvc.get("/.well-known/openid-configuration")
            .andExpect {
                status { isOk() }
                jsonPath("$.issuer") { isNotEmpty() }
                jsonPath("$.authorization_endpoint") { isNotEmpty() }
                jsonPath("$.token_endpoint") { isNotEmpty() }
                jsonPath("$.jwks_uri") { isNotEmpty() }
            }
    }

    @Test
    fun `JWKS endpoint should return keys`() {
        mockMvc.get("/oauth2/jwks")
            .andExpect {
                status { isOk() }
                jsonPath("$.keys") { isArray() }
                jsonPath("$.keys[0].kty") { value("RSA") }
            }
    }

    @Test
    fun `authorize endpoint without params should return error`() {
        mockMvc.get("/oauth2/authorize")
            .andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `token endpoint without params should return 400`() {
        mockMvc.post("/oauth2/token") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `token endpoint with invalid client_id should return 401`() {
        mockMvc.post("/oauth2/token") {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            param("grant_type", "authorization_code")
            param("client_id", "non-existent-client")
            param("code", "fake-code")
            param("redirect_uri", "http://localhost:3000/callback")
            param("code_verifier", "test-verifier-that-is-long-enough-to-be-valid-43-chars")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `existing API endpoints should still work`() {
        mockMvc.get("/actuator/health")
            .andExpect {
                status { isOk() }
            }
    }

    @Test
    fun `auth signup endpoint should still work alongside OAuth2`() {
        val request = SignupRequest(
            email = "coexist-${System.nanoTime()}@example.com",
            password = "password123"
        )

        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.accessToken") { isNotEmpty() }
        }
    }
}
