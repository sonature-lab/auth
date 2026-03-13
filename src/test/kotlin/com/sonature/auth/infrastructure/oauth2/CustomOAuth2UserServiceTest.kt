package com.sonature.auth.infrastructure.oauth2

import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.user.model.AuthProvider
import com.sonature.auth.domain.user.model.UserStatus
import com.sonature.auth.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import java.time.Instant

class CustomOAuth2UserServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var timeProvider: TimeProvider
    private lateinit var delegateOAuth2UserService: DefaultOAuth2UserService
    private lateinit var customOAuth2UserService: CustomOAuth2UserService

    private val now = Instant.parse("2026-03-14T10:00:00Z")

    @BeforeEach
    fun setUp() {
        userRepository = mockk(relaxed = true)
        timeProvider = mockk()
        delegateOAuth2UserService = mockk()
        every { timeProvider.now() } returns now

        customOAuth2UserService = CustomOAuth2UserService(
            userRepository = userRepository,
            timeProvider = timeProvider,
            delegateOAuth2UserService = delegateOAuth2UserService
        )
    }

    private fun createUserRequest(registrationId: String): OAuth2UserRequest {
        val clientRegistration = ClientRegistration.withRegistrationId(registrationId)
            .clientId("test-client-id")
            .clientSecret("test-client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost:8080/login/oauth2/code/$registrationId")
            .authorizationUri("https://example.com/auth")
            .tokenUri("https://example.com/token")
            .userInfoUri("https://example.com/userinfo")
            .userNameAttributeName("sub")
            .build()

        val accessToken = OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "test-access-token",
            now,
            now.plusSeconds(3600)
        )

        return OAuth2UserRequest(clientRegistration, accessToken)
    }

    private fun createOAuth2User(attributes: Map<String, Any>, nameKey: String = "sub"): OAuth2User {
        return DefaultOAuth2User(
            emptyList(),
            attributes,
            nameKey
        )
    }

    @Test
    fun `should create new user for new social login`() {
        val userRequest = createUserRequest("google")
        val oAuth2User = createOAuth2User(
            mapOf(
                "sub" to "google-123",
                "email" to "new@gmail.com",
                "name" to "New User"
            )
        )
        every { delegateOAuth2UserService.loadUser(userRequest) } returns oAuth2User
        every { userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-123") } returns null
        every { userRepository.findByEmail("new@gmail.com") } returns null

        val userSlot = slot<UserEntity>()
        every { userRepository.save(capture(userSlot)) } answers { userSlot.captured }

        val result = customOAuth2UserService.loadUser(userRequest)

        assertNotNull(result)
        verify { userRepository.save(any()) }
        val savedUser = userSlot.captured
        assertEquals("new@gmail.com", savedUser.email)
        assertEquals("New User", savedUser.name)
        assertEquals(AuthProvider.GOOGLE, savedUser.provider)
        assertEquals("google-123", savedUser.providerId)
    }

    @Test
    fun `should login existing social user`() {
        val userRequest = createUserRequest("google")
        val oAuth2User = createOAuth2User(
            mapOf(
                "sub" to "google-existing",
                "email" to "existing@gmail.com",
                "name" to "Existing User"
            )
        )
        every { delegateOAuth2UserService.loadUser(userRequest) } returns oAuth2User

        val existingUser = UserEntity(
            email = "existing@gmail.com",
            name = "Existing User",
            provider = AuthProvider.GOOGLE,
            providerId = "google-existing",
            status = UserStatus.ACTIVE
        )
        every { userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-existing") } returns existingUser
        every { userRepository.save(any()) } returns existingUser

        val result = customOAuth2UserService.loadUser(userRequest)

        assertNotNull(result)
        verify { userRepository.save(existingUser) }
        assertEquals(now, existingUser.lastLoginAt)
    }

    @Test
    fun `should link social login to existing LOCAL account with same email`() {
        val userRequest = createUserRequest("google")
        val oAuth2User = createOAuth2User(
            mapOf(
                "sub" to "google-link-123",
                "email" to "local@example.com",
                "name" to "Local User"
            )
        )
        every { delegateOAuth2UserService.loadUser(userRequest) } returns oAuth2User
        every { userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-link-123") } returns null

        val localUser = UserEntity(
            email = "local@example.com",
            passwordHash = "hashed-pw",
            name = "Local User",
            provider = AuthProvider.LOCAL
        )
        every { userRepository.findByEmail("local@example.com") } returns localUser
        every { userRepository.save(any()) } returns localUser

        val result = customOAuth2UserService.loadUser(userRequest)

        assertNotNull(result)
        verify { userRepository.save(localUser) }
        assertEquals(AuthProvider.GOOGLE, localUser.provider)
        assertEquals("google-link-123", localUser.providerId)
    }

    @Test
    fun `should throw when social account has no email`() {
        val userRequest = createUserRequest("github")
        val oAuth2User = createOAuth2User(
            mapOf(
                "id" to 12345,
                "login" to "no-email-user"
            ),
            nameKey = "login"
        )
        every { delegateOAuth2UserService.loadUser(userRequest) } returns oAuth2User
        every { userRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "12345") } returns null

        assertThrows<OAuth2AuthenticationException> {
            customOAuth2UserService.loadUser(userRequest)
        }
    }
}
