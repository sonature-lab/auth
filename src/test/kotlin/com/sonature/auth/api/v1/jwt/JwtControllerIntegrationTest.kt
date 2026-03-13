package com.sonature.auth.api.v1.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.sonature.auth.api.v1.jwt.dto.JwtIssueRequest
import com.sonature.auth.api.v1.jwt.dto.JwtVerifyRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
class JwtControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `POST issue should return JWT token`() {
        val request = JwtIssueRequest(
            subject = "user-123",
            audience = "test-client"
        )

        mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { exists() }
            jsonPath("$.data.tokenType") { value("Bearer") }
            jsonPath("$.data.expiresIn") { isNumber() }
            jsonPath("$.data.issuedAt") { isNumber() }
        }
    }

    @Test
    fun `POST verify should validate token and return claims`() {
        val issueRequest = JwtIssueRequest(subject = "user-123")
        val issueResult = mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(issueRequest)
        }.andReturn()

        val issueResponse = objectMapper.readTree(issueResult.response.contentAsString)
        val token = issueResponse["data"]["accessToken"].asText()

        val verifyRequest = JwtVerifyRequest(token = token)

        mockMvc.post("/api/v1/jwt/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(verifyRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.valid") { value(true) }
            jsonPath("$.data.subject") { value("user-123") }
            jsonPath("$.data.issuer") { value("sonature-auth") }
            jsonPath("$.data.tokenType") { value("ACCESS") }
        }
    }

    @Test
    fun `POST verify should return error for invalid token`() {
        val request = JwtVerifyRequest(token = "invalid.token.here")

        mockMvc.post("/api/v1/jwt/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { exists() }
        }
    }

    @Test
    fun `POST issue should return error when subject is blank`() {
        val request = JwtIssueRequest(subject = "")

        mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("MISSING_PARAMETER") }
        }
    }

    @Test
    fun `POST issue with custom claims should preserve them`() {
        val request = JwtIssueRequest(
            subject = "user-123",
            customClaims = mapOf("role" to "admin", "orgId" to "org-456")
        )

        val issueResult = mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andReturn()

        val token = objectMapper.readTree(issueResult.response.contentAsString)
            .get("data").get("accessToken").asText()

        val verifyRequest = JwtVerifyRequest(token = token)
        mockMvc.post("/api/v1/jwt/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(verifyRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.customClaims.role") { value("admin") }
            jsonPath("$.data.customClaims.orgId") { value("org-456") }
        }
    }

    @Test
    fun `POST issue with custom expiration should work`() {
        val request = JwtIssueRequest(
            subject = "user-123",
            expiresIn = 1800
        )

        mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.expiresIn") { value(1800) }
        }
    }

    @Test
    fun `POST issue with audience should preserve it`() {
        val request = JwtIssueRequest(
            subject = "user-123",
            audience = "my-client-app"
        )

        val issueResult = mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andReturn()

        val token = objectMapper.readTree(issueResult.response.contentAsString)
            .get("data").get("accessToken").asText()

        val verifyRequest = JwtVerifyRequest(token = token)
        mockMvc.post("/api/v1/jwt/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(verifyRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.audience") { value("my-client-app") }
        }
    }

    // ==================== RS256 Tests ====================

    @Test
    fun `POST issue with RS256 should return JWT token`() {
        val request = JwtIssueRequest(
            subject = "user-rs256",
            algorithm = "RS256",
            audience = "test-client"
        )

        mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { exists() }
            jsonPath("$.data.tokenType") { value("Bearer") }
            jsonPath("$.data.expiresIn") { isNumber() }
        }
    }

    @Test
    fun `POST verify RS256 token should return claims`() {
        val issueRequest = JwtIssueRequest(
            subject = "user-rs256",
            algorithm = "RS256"
        )
        val issueResult = mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(issueRequest)
        }.andReturn()

        val issueResponse = objectMapper.readTree(issueResult.response.contentAsString)
        val token = issueResponse["data"]["accessToken"].asText()

        val verifyRequest = JwtVerifyRequest(token = token, algorithm = "RS256")

        mockMvc.post("/api/v1/jwt/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(verifyRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.valid") { value(true) }
            jsonPath("$.data.subject") { value("user-rs256") }
            jsonPath("$.data.issuer") { value("sonature-auth") }
        }
    }

    @Test
    fun `RS256 token should preserve custom claims`() {
        val request = JwtIssueRequest(
            subject = "user-rs256",
            algorithm = "RS256",
            customClaims = mapOf("role" to "admin", "permission" to "write")
        )

        val issueResult = mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andReturn()

        val token = objectMapper.readTree(issueResult.response.contentAsString)
            .get("data").get("accessToken").asText()

        val verifyRequest = JwtVerifyRequest(token = token, algorithm = "RS256")
        mockMvc.post("/api/v1/jwt/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(verifyRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.customClaims.role") { value("admin") }
            jsonPath("$.data.customClaims.permission") { value("write") }
        }
    }

    @Test
    fun `HS256 and RS256 tokens should be independent`() {
        // Issue HS256 token
        val hs256Request = JwtIssueRequest(subject = "user-hs256", algorithm = "HS256")
        val hs256Result = mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(hs256Request)
        }.andReturn()
        val hs256Token = objectMapper.readTree(hs256Result.response.contentAsString)
            .get("data").get("accessToken").asText()

        // Issue RS256 token
        val rs256Request = JwtIssueRequest(subject = "user-rs256", algorithm = "RS256")
        val rs256Result = mockMvc.post("/api/v1/jwt/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(rs256Request)
        }.andReturn()
        val rs256Token = objectMapper.readTree(rs256Result.response.contentAsString)
            .get("data").get("accessToken").asText()

        // Verify HS256 token with HS256
        mockMvc.post("/api/v1/jwt/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtVerifyRequest(token = hs256Token, algorithm = "HS256"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.subject") { value("user-hs256") }
        }

        // Verify RS256 token with RS256
        mockMvc.post("/api/v1/jwt/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtVerifyRequest(token = rs256Token, algorithm = "RS256"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.subject") { value("user-rs256") }
        }
    }
}
