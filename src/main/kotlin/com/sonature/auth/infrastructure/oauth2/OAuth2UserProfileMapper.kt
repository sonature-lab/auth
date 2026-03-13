package com.sonature.auth.infrastructure.oauth2

import com.sonature.auth.domain.user.model.AuthProvider

object OAuth2UserProfileMapper {

    fun map(provider: AuthProvider, attributes: Map<String, Any>): OAuth2UserProfile {
        return when (provider) {
            AuthProvider.GOOGLE -> mapGoogle(attributes)
            AuthProvider.GITHUB -> mapGitHub(attributes)
            AuthProvider.KAKAO -> mapKakao(attributes)
            AuthProvider.LOCAL -> throw IllegalArgumentException("LOCAL provider does not support OAuth2 profile mapping")
        }
    }

    private fun mapGoogle(attributes: Map<String, Any>): OAuth2UserProfile {
        return OAuth2UserProfile(
            provider = AuthProvider.GOOGLE,
            providerId = attributes["sub"] as String,
            email = attributes["email"] as? String,
            name = attributes["name"] as? String
        )
    }

    private fun mapGitHub(attributes: Map<String, Any>): OAuth2UserProfile {
        val id = attributes["id"]
        val providerId = id.toString()
        val name = attributes["name"] as? String ?: attributes["login"] as? String

        return OAuth2UserProfile(
            provider = AuthProvider.GITHUB,
            providerId = providerId,
            email = attributes["email"] as? String,
            name = name
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapKakao(attributes: Map<String, Any>): OAuth2UserProfile {
        val id = attributes["id"]
        val providerId = id.toString()

        val kakaoAccount = attributes["kakao_account"] as? Map<String, Any>
        val email = kakaoAccount?.get("email") as? String
        val profile = kakaoAccount?.get("profile") as? Map<String, Any>
        val nickname = profile?.get("nickname") as? String

        return OAuth2UserProfile(
            provider = AuthProvider.KAKAO,
            providerId = providerId,
            email = email,
            name = nickname
        )
    }
}
