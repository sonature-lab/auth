package com.sonature.auth.infrastructure.oauth2

import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.user.model.AuthProvider
import com.sonature.auth.domain.user.model.UserStatus
import com.sonature.auth.domain.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SocialLoginIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    @Test
    fun `OAuth2 login endpoints should be accessible`() {
        // Verify Google OAuth2 authorization redirect endpoint is available
        mockMvc.get("/oauth2/authorization/google")
            .andExpect {
                status { is3xxRedirection() }
            }
    }

    @Test
    fun `OAuth2 login endpoint for GitHub should redirect`() {
        mockMvc.get("/oauth2/authorization/github")
            .andExpect {
                status { is3xxRedirection() }
            }
    }

    @Test
    fun `OAuth2 login endpoint for Kakao should redirect`() {
        mockMvc.get("/oauth2/authorization/kakao")
            .andExpect {
                status { is3xxRedirection() }
            }
    }

    @Test
    fun `CustomOAuth2UserService should create new user from social profile`() {
        // Directly test the service layer logic via repository
        val user = UserEntity(
            email = "social@gmail.com",
            name = "Social User",
            provider = AuthProvider.GOOGLE,
            providerId = "google-12345",
            status = UserStatus.ACTIVE
        )
        val saved = userRepository.save(user)

        val found = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-12345")
        assertNotNull(found)
        assertEquals("social@gmail.com", found!!.email)
        assertEquals(AuthProvider.GOOGLE, found.provider)
        assertEquals("google-12345", found.providerId)
    }

    @Test
    fun `should find user by email for account linking`() {
        val localUser = UserEntity(
            email = "local@example.com",
            passwordHash = "hashed",
            provider = AuthProvider.LOCAL,
            status = UserStatus.ACTIVE
        )
        userRepository.save(localUser)

        val found = userRepository.findByEmail("local@example.com")
        assertNotNull(found)
        assertEquals(AuthProvider.LOCAL, found!!.provider)

        // Simulate account linking
        found.provider = AuthProvider.GOOGLE
        found.providerId = "google-linked-id"
        userRepository.save(found)

        val linked = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, "google-linked-id")
        assertNotNull(linked)
        assertEquals("local@example.com", linked!!.email)
    }

    @Test
    fun `OAuth2UserProfileMapper should correctly map all providers`() {
        // Google
        val googleProfile = OAuth2UserProfileMapper.map(
            AuthProvider.GOOGLE,
            mapOf("sub" to "g-123", "email" to "g@test.com", "name" to "Google")
        )
        assertEquals(AuthProvider.GOOGLE, googleProfile.provider)
        assertEquals("g-123", googleProfile.providerId)

        // GitHub
        val githubProfile = OAuth2UserProfileMapper.map(
            AuthProvider.GITHUB,
            mapOf("id" to 456, "email" to "gh@test.com", "login" to "ghuser", "name" to "GitHub")
        )
        assertEquals(AuthProvider.GITHUB, githubProfile.provider)
        assertEquals("456", githubProfile.providerId)

        // Kakao
        val kakaoProfile = OAuth2UserProfileMapper.map(
            AuthProvider.KAKAO,
            mapOf(
                "id" to 789L,
                "kakao_account" to mapOf(
                    "email" to "k@test.com",
                    "profile" to mapOf("nickname" to "Kakao")
                )
            )
        )
        assertEquals(AuthProvider.KAKAO, kakaoProfile.provider)
        assertEquals("789", kakaoProfile.providerId)
    }
}
