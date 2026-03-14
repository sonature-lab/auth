package com.sonature.auth.application.service

import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.token.model.Token
import com.sonature.auth.domain.token.model.TokenPair
import com.sonature.auth.domain.token.model.TokenType
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.user.exception.EmailAlreadyExistsException
import com.sonature.auth.domain.user.exception.InvalidCredentialsException
import com.sonature.auth.domain.user.exception.UserSuspendedException
import com.sonature.auth.domain.user.model.AuthProvider
import com.sonature.auth.domain.user.model.UserStatus
import com.sonature.auth.domain.tenant.repository.TenantMembershipRepository
import com.sonature.auth.domain.user.repository.UserRepository
import com.sonature.auth.application.usecase.TokenRefreshUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var tenantMembershipRepository: TenantMembershipRepository
    private lateinit var tokenRefreshUseCase: TokenRefreshUseCase
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var timeProvider: TimeProvider
    private lateinit var authService: AuthService

    private val now = Instant.parse("2026-03-13T10:00:00Z")

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        tenantMembershipRepository = mockk()
        tokenRefreshUseCase = mockk()
        passwordEncoder = mockk()
        timeProvider = mockk()
        every { timeProvider.now() } returns now
        every { tenantMembershipRepository.findAllByUser(any()) } returns emptyList()

        authService = AuthService(userRepository, tenantMembershipRepository, tokenRefreshUseCase, passwordEncoder, timeProvider)
    }

    private fun mockTokenPair(): TokenPair {
        val accessToken = Token(
            value = "access-token",
            type = TokenType.ACCESS,
            algorithm = Algorithm.HS256,
            expiresAt = now.plusSeconds(900),
            issuedAt = now
        )
        val refreshToken = Token(
            value = "refresh-token",
            type = TokenType.REFRESH,
            algorithm = Algorithm.HS256,
            expiresAt = now.plusSeconds(604800),
            issuedAt = now
        )
        return TokenPair(accessToken, refreshToken)
    }

    @Test
    fun `signup should create user and return token pair`() {
        every { userRepository.existsByEmail("test@example.com") } returns false
        every { passwordEncoder.encode("password123") } returns "hashed-password"
        val userSlot = slot<UserEntity>()
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }
        every { tokenRefreshUseCase.issueTokenPair(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockTokenPair()

        val (user, tokenPair) = authService.signup("test@example.com", "password123", "Test")

        assertEquals("test@example.com", user.email)
        assertEquals("Test", user.name)
        assertEquals(AuthProvider.LOCAL, user.provider)
        assertEquals("hashed-password", user.passwordHash)
        assertNotNull(tokenPair.accessToken.value)
    }

    @Test
    fun `signup with duplicate email should throw`() {
        every { userRepository.existsByEmail("dup@example.com") } returns true

        assertThrows<EmailAlreadyExistsException> {
            authService.signup("dup@example.com", "password123", null)
        }
    }

    @Test
    fun `login with valid credentials should return token pair`() {
        val user = UserEntity(
            email = "test@example.com",
            passwordHash = "hashed",
            provider = AuthProvider.LOCAL,
            status = UserStatus.ACTIVE
        )
        every { userRepository.findByEmail("test@example.com") } returns user
        every { passwordEncoder.matches("password123", "hashed") } returns true
        every { userRepository.save(any()) } returns user
        every { tokenRefreshUseCase.issueTokenPair(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockTokenPair()

        val (returnedUser, tokenPair) = authService.login("test@example.com", "password123")

        assertEquals("test@example.com", returnedUser.email)
        assertNotNull(tokenPair.accessToken.value)
        verify { userRepository.save(any()) }
    }

    @Test
    fun `login with wrong password should throw`() {
        val user = UserEntity(
            email = "test@example.com",
            passwordHash = "hashed",
            provider = AuthProvider.LOCAL
        )
        every { userRepository.findByEmail("test@example.com") } returns user
        every { passwordEncoder.matches("wrong", "hashed") } returns false

        assertThrows<InvalidCredentialsException> {
            authService.login("test@example.com", "wrong")
        }
    }

    @Test
    fun `login with non-existent email should throw`() {
        every { userRepository.findByEmail("nobody@example.com") } returns null

        assertThrows<InvalidCredentialsException> {
            authService.login("nobody@example.com", "password123")
        }
    }

    @Test
    fun `login with suspended user should throw`() {
        val user = UserEntity(
            email = "suspended@example.com",
            passwordHash = "hashed",
            provider = AuthProvider.LOCAL,
            status = UserStatus.SUSPENDED
        )
        every { userRepository.findByEmail("suspended@example.com") } returns user

        assertThrows<UserSuspendedException> {
            authService.login("suspended@example.com", "password123")
        }
    }
}
