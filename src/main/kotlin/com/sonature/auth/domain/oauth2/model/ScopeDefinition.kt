package com.sonature.auth.domain.oauth2.model

/**
 * OAuth2 scope definitions with localized descriptions.
 */
data class ScopeDefinition(
    val scope: String,
    val descriptionKo: String,
    val descriptionEn: String
) {
    companion object {
        private val DEFINITIONS = listOf(
            ScopeDefinition(
                scope = "openid",
                descriptionKo = "OpenID Connect 인증 정보에 접근",
                descriptionEn = "Access to OpenID Connect identity information"
            ),
            ScopeDefinition(
                scope = "profile",
                descriptionKo = "프로필 정보 (이름, 사진 등) 읽기",
                descriptionEn = "Read profile information (name, picture, etc.)"
            ),
            ScopeDefinition(
                scope = "email",
                descriptionKo = "이메일 주소 읽기",
                descriptionEn = "Read email address"
            ),
            ScopeDefinition(
                scope = "auth:read",
                descriptionKo = "인증 정보 읽기",
                descriptionEn = "Read authentication information"
            ),
            ScopeDefinition(
                scope = "auth:write",
                descriptionKo = "인증 정보 수정",
                descriptionEn = "Modify authentication information"
            )
        )

        private val SCOPE_MAP = DEFINITIONS.associateBy { it.scope }

        fun findByScope(scope: String): ScopeDefinition? = SCOPE_MAP[scope]

        fun findByScopes(scopes: Set<String>): List<ScopeDefinition> =
            scopes.mapNotNull { SCOPE_MAP[it] }

        fun all(): List<ScopeDefinition> = DEFINITIONS.toList()
    }
}
