package com.sonature.auth.api.v1.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.sonature.auth.api.v1.auth.dto.LoginRequest
import com.sonature.auth.api.v1.auth.dto.SignupRequest
import com.sonature.auth.infrastructure.security.RateLimitFilter
import org.junit.jupiter.api.BeforeEach
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
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var rateLimitFilter: RateLimitFilter

    @BeforeEach
    fun resetRateLimits() {
        rateLimitFilter.clearBuckets()
    }

    @Test
    fun `POST signup should create user and return tokens`() {
        val request = SignupRequest(
            email = "test@example.com",
            password = "password123",
            name = "Test User"
        )

        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.data.user.email") { value("test@example.com") }
            jsonPath("$.data.user.name") { value("Test User") }
            jsonPath("$.data.user.provider") { value("LOCAL") }
            jsonPath("$.data.accessToken") { isNotEmpty() }
            jsonPath("$.data.refreshToken") { isNotEmpty() }
            jsonPath("$.data.accessExpiresIn") { isNumber() }
        }
    }

    @Test
    fun `POST signup with duplicate email should return 409`() {
        val request = SignupRequest(
            email = "duplicate@example.com",
            password = "password123"
        )

        // First signup
        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect { status { isCreated() } }

        // Duplicate signup
        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error.code") { value("EMAIL_ALREADY_EXISTS") }
        }
    }

    @Test
    fun `POST signup with invalid email should return 400`() {
        val request = SignupRequest(
            email = "not-an-email",
            password = "password123"
        )

        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST signup with short password should return 400`() {
        val request = SignupRequest(
            email = "short@example.com",
            password = "123"
        )

        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST login with valid credentials should return tokens`() {
        val email = "login-test@example.com"
        val password = "password123"

        // Signup first
        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SignupRequest(email = email, password = password))
        }.andExpect { status { isCreated() } }

        // Login
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(email = email, password = password))
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.user.email") { value(email) }
            jsonPath("$.data.accessToken") { isNotEmpty() }
            jsonPath("$.data.refreshToken") { isNotEmpty() }
        }
    }

    @Test
    fun `POST login with wrong password should return 401`() {
        val email = "wrong-pw@example.com"

        // Signup
        mockMvc.post("/api/v1/auth/signup") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(SignupRequest(email = email, password = "password123"))
        }.andExpect { status { isCreated() } }

        // Login with wrong password
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(email = email, password = "wrongpassword"))
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("INVALID_CREDENTIALS") }
        }
    }

    @Test
    fun `POST login with non-existent email should return 401`() {
        mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                LoginRequest(email = "nobody@example.com", password = "password123")
            )
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error.code") { value("INVALID_CREDENTIALS") }
        }
    }
}
