package com.sonature.auth.api.v1.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.sonature.auth.api.v1.jwt.dto.JwtIssueRequest
import com.sonature.auth.api.v1.jwt.dto.JwtRefreshRequest
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
class JwtRefreshIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `POST issue-pair should return both access and refresh tokens`() {
        val request = JwtIssueRequest(
            subject = "user-123",
            audience = "test-client"
        )

        mockMvc.post("/api/v1/jwt/issue-pair") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { exists() }
            jsonPath("$.data.refreshToken") { exists() }
            jsonPath("$.data.tokenType") { value("Bearer") }
            jsonPath("$.data.accessExpiresIn") { isNumber() }
            jsonPath("$.data.refreshExpiresIn") { isNumber() }
        }
    }

    @Test
    fun `POST refresh should return new token pair`() {
        val issueRequest = JwtIssueRequest(subject = "user-refresh-test")
        val issueResult = mockMvc.post("/api/v1/jwt/issue-pair") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(issueRequest)
        }.andReturn()

        val issueResponse = objectMapper.readTree(issueResult.response.contentAsString)
        val refreshToken = issueResponse["data"]["refreshToken"].asText()

        val refreshRequest = JwtRefreshRequest(refreshToken = refreshToken)

        mockMvc.post("/api/v1/jwt/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(refreshRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { exists() }
            jsonPath("$.data.refreshToken") { exists() }
            jsonPath("$.data.tokenType") { value("Bearer") }
        }
    }

    @Test
    fun `POST refresh should fail with reused refresh token (token rotation)`() {
        val issueResult = mockMvc.post("/api/v1/jwt/issue-pair") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtIssueRequest(subject = "user-rotation-test-${System.currentTimeMillis()}"))
        }.andReturn()

        val responseJson = objectMapper.readTree(issueResult.response.contentAsString)
        val firstRefreshToken = responseJson["data"]["refreshToken"].asText()

        mockMvc.post("/api/v1/jwt/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtRefreshRequest(refreshToken = firstRefreshToken))
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/api/v1/jwt/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtRefreshRequest(refreshToken = firstRefreshToken))
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("REFRESH_TOKEN_REUSE_DETECTED") }
        }
    }

    @Test
    fun `POST refresh should fail with invalid refresh token`() {
        val request = JwtRefreshRequest(refreshToken = "invalid.token.here")

        mockMvc.post("/api/v1/jwt/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST refresh should fail when access token is used as refresh token`() {
        val issueResult = mockMvc.post("/api/v1/jwt/issue-pair") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtIssueRequest(subject = "user-type-test"))
        }.andReturn()

        val responseJson = objectMapper.readTree(issueResult.response.contentAsString)
        val accessToken = responseJson["data"]["accessToken"].asText()

        mockMvc.post("/api/v1/jwt/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtRefreshRequest(refreshToken = accessToken))
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error.code") { value("INVALID_TOKEN_TYPE") }
        }
    }

    @Test
    fun `Token rotation should invalidate all tokens after reuse detection`() {
        val subject = "user-security-test-${System.currentTimeMillis()}"

        val firstIssue = mockMvc.post("/api/v1/jwt/issue-pair") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtIssueRequest(subject = subject))
        }.andReturn()

        val firstJson = objectMapper.readTree(firstIssue.response.contentAsString)
        val firstRefreshToken = firstJson["data"]["refreshToken"].asText()

        val secondIssue = mockMvc.post("/api/v1/jwt/issue-pair") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtIssueRequest(subject = subject))
        }.andReturn()

        val secondJson = objectMapper.readTree(secondIssue.response.contentAsString)
        val secondRefreshToken = secondJson["data"]["refreshToken"].asText()

        mockMvc.post("/api/v1/jwt/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtRefreshRequest(refreshToken = firstRefreshToken))
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/api/v1/jwt/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtRefreshRequest(refreshToken = firstRefreshToken))
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("REFRESH_TOKEN_REUSE_DETECTED") }
        }

        mockMvc.post("/api/v1/jwt/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtRefreshRequest(refreshToken = secondRefreshToken))
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST issue-pair with RS256 should work`() {
        val request = JwtIssueRequest(
            subject = "user-rs256-refresh",
            algorithm = "RS256"
        )

        mockMvc.post("/api/v1/jwt/issue-pair") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { exists() }
            jsonPath("$.data.refreshToken") { exists() }
        }
    }

    @Test
    fun `POST refresh with RS256 should work`() {
        val issueResult = mockMvc.post("/api/v1/jwt/issue-pair") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtIssueRequest(subject = "user-rs256-refresh-test", algorithm = "RS256"))
        }.andReturn()

        val responseJson = objectMapper.readTree(issueResult.response.contentAsString)
        val refreshToken = responseJson["data"]["refreshToken"].asText()

        mockMvc.post("/api/v1/jwt/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(JwtRefreshRequest(refreshToken = refreshToken, algorithm = "RS256"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.accessToken") { exists() }
            jsonPath("$.data.refreshToken") { exists() }
        }
    }
}
