package com.sonature.auth.infrastructure.oauth2

import com.sonature.auth.domain.user.model.AuthProvider

data class OAuth2UserProfile(
    val provider: AuthProvider,
    val providerId: String,
    val email: String?,
    val name: String?
)
