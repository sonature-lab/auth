package com.sonature.auth.api.v1.paseto

import com.fasterxml.jackson.databind.ObjectMapper
import com.sonature.auth.api.v1.paseto.dto.PasetoIssueRequest
import com.sonature.auth.api.v1.paseto.dto.PasetoVerifyRequest
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasetoControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `POST issue should return PASETO v4 local token`() {
        val request = PasetoIssueRequest(
            subject = "user-123",
            audience = "test-client"
        )

        mockMvc.post("/api/v1/paseto/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.token") { exists() }
            jsonPath("$.data.token") { value(Matchers.startsWith("v4.local.")) }
            jsonPath("$.data.tokenType") { value("paseto") }
            jsonPath("$.data.expiresIn") { isNumber() }
            jsonPath("$.data.issuedAt") { isNumber() }
        }
    }

    @Test
    fun `POST verify should validate token and return claims`() {
        val issueRequest = PasetoIssueRequest(subject = "user-456")
        val issueResult = mockMvc.post("/api/v1/paseto/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(issueRequest)
        }.andReturn()

        val issueResponse = objectMapper.readTree(issueResult.response.contentAsString)
        val token = issueResponse["data"]["token"].asText()

        val verifyRequest = PasetoVerifyRequest(token = token)

        mockMvc.post("/api/v1/paseto/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(verifyRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.valid") { value(true) }
            jsonPath("$.data.subject") { value("user-456") }
            jsonPath("$.data.issuer") { value("sonature-auth") }
            jsonPath("$.data.tokenType") { value("ACCESS") }
        }
    }

    @Test
    fun `POST verify should return error for invalid token`() {
        val request = PasetoVerifyRequest(token = "v4.local.invalid-token-here")

        mockMvc.post("/api/v1/paseto/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { exists() }
        }
    }

    @Test
    fun `POST issue should return error when subject is blank`() {
        val request = PasetoIssueRequest(subject = "")

        mockMvc.post("/api/v1/paseto/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("MISSING_PARAMETER") }
        }
    }

    @Test
    fun `POST issue with custom claims should preserve them`() {
        val request = PasetoIssueRequest(
            subject = "user-789",
            customClaims = mapOf("role" to "admin", "orgId" to "org-999")
        )

        val issueResult = mockMvc.post("/api/v1/paseto/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andReturn()

        val token = objectMapper.readTree(issueResult.response.contentAsString)
            .get("data").get("token").asText()

        val verifyRequest = PasetoVerifyRequest(token = token)
        mockMvc.post("/api/v1/paseto/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(verifyRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.customClaims.role") { value("admin") }
            jsonPath("$.data.customClaims.orgId") { value("org-999") }
        }
    }

    @Test
    fun `POST issue with custom expiration should work`() {
        val request = PasetoIssueRequest(
            subject = "user-123",
            expiresIn = 1800
        )

        mockMvc.post("/api/v1/paseto/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.expiresIn") { value(1800) }
        }
    }

    @Test
    fun `POST issue with audience should preserve it`() {
        val request = PasetoIssueRequest(
            subject = "user-123",
            audience = "my-client-app"
        )

        val issueResult = mockMvc.post("/api/v1/paseto/issue") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andReturn()

        val token = objectMapper.readTree(issueResult.response.contentAsString)
            .get("data").get("token").asText()

        val verifyRequest = PasetoVerifyRequest(token = token)
        mockMvc.post("/api/v1/paseto/verify") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(verifyRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.audience") { value("my-client-app") }
        }
    }
}
