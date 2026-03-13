package com.sonature.auth.infrastructure.oauth2

import com.sonature.auth.domain.user.model.AuthProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OAuth2UserProfileMapperTest {

    @Test
    fun `should map Google profile correctly`() {
        val attributes = mapOf<String, Any>(
            "sub" to "google-user-id-123",
            "email" to "user@gmail.com",
            "name" to "Google User",
            "picture" to "https://example.com/photo.jpg"
        )

        val profile = OAuth2UserProfileMapper.map(AuthProvider.GOOGLE, attributes)

        assertEquals("google-user-id-123", profile.providerId)
        assertEquals("user@gmail.com", profile.email)
        assertEquals("Google User", profile.name)
        assertEquals(AuthProvider.GOOGLE, profile.provider)
    }

    @Test
    fun `should map GitHub profile correctly`() {
        val attributes = mapOf<String, Any>(
            "id" to 12345,
            "email" to "user@github.com",
            "login" to "github-user",
            "name" to "GitHub User",
            "avatar_url" to "https://avatars.githubusercontent.com/u/12345"
        )

        val profile = OAuth2UserProfileMapper.map(AuthProvider.GITHUB, attributes)

        assertEquals("12345", profile.providerId)
        assertEquals("user@github.com", profile.email)
        assertEquals("GitHub User", profile.name)
        assertEquals(AuthProvider.GITHUB, profile.provider)
    }

    @Test
    fun `should map GitHub profile with null email using login as fallback`() {
        val attributes = mapOf<String, Any>(
            "id" to 67890,
            "login" to "github-user-no-email",
            "name" to "No Email User"
        )

        val profile = OAuth2UserProfileMapper.map(AuthProvider.GITHUB, attributes)

        assertEquals("67890", profile.providerId)
        assertNull(profile.email)
        assertEquals("No Email User", profile.name)
        assertEquals(AuthProvider.GITHUB, profile.provider)
    }

    @Test
    fun `should map GitHub profile using login as name when name is null`() {
        val attributes = mapOf<String, Any>(
            "id" to 11111,
            "email" to "user@github.com",
            "login" to "my-login"
        )

        val profile = OAuth2UserProfileMapper.map(AuthProvider.GITHUB, attributes)

        assertEquals("my-login", profile.name)
    }

    @Test
    fun `should map Kakao profile correctly`() {
        val kakaoAccount = mapOf<String, Any>(
            "email" to "user@kakao.com",
            "profile" to mapOf("nickname" to "Kakao User")
        )
        val attributes = mapOf<String, Any>(
            "id" to 99999L,
            "kakao_account" to kakaoAccount
        )

        val profile = OAuth2UserProfileMapper.map(AuthProvider.KAKAO, attributes)

        assertEquals("99999", profile.providerId)
        assertEquals("user@kakao.com", profile.email)
        assertEquals("Kakao User", profile.name)
        assertEquals(AuthProvider.KAKAO, profile.provider)
    }

    @Test
    fun `should handle Kakao profile with missing nested fields`() {
        val attributes = mapOf<String, Any>(
            "id" to 88888L
        )

        val profile = OAuth2UserProfileMapper.map(AuthProvider.KAKAO, attributes)

        assertEquals("88888", profile.providerId)
        assertNull(profile.email)
        assertNull(profile.name)
    }

    @Test
    fun `should throw for unsupported provider LOCAL`() {
        val attributes = mapOf<String, Any>("id" to "123")

        assertThrows<IllegalArgumentException> {
            OAuth2UserProfileMapper.map(AuthProvider.LOCAL, attributes)
        }
    }
}
