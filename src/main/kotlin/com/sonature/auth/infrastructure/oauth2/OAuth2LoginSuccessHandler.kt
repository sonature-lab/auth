package com.sonature.auth.infrastructure.oauth2

import com.fasterxml.jackson.databind.ObjectMapper
import com.sonature.auth.application.usecase.TokenRefreshUseCase
import com.sonature.auth.domain.token.model.Algorithm
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2LoginSuccessHandler(
    private val tokenRefreshUseCase: TokenRefreshUseCase
) : AuthenticationSuccessHandler {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val objectMapper = ObjectMapper()

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oAuth2User = authentication.principal as OAuth2User
        val userId = oAuth2User.attributes["userId"] as? String
            ?: oAuth2User.name

        val email = oAuth2User.attributes["email"] as? String
        val provider = if (authentication is OAuth2AuthenticationToken) {
            authentication.authorizedClientRegistrationId
        } else {
            "unknown"
        }

        logger.info("OAuth2 login success: provider=$provider, userId=$userId")

        val customClaims = buildMap<String, Any> {
            put("provider", provider)
            email?.let { put("email", it) }
            oAuth2User.attributes["name"]?.let { put("name", it.toString()) }
        }

        val tokenPair = tokenRefreshUseCase.issueTokenPair(
            subject = userId,
            algorithm = Algorithm.HS256,
            customClaims = customClaims
        )

        val responseBody = mapOf(
            "success" to true,
            "data" to mapOf(
                "accessToken" to tokenPair.accessToken.value,
                "refreshToken" to tokenPair.refreshToken.value,
                "expiresIn" to (tokenPair.accessToken.expiresAt.epochSecond - tokenPair.accessToken.issuedAt.epochSecond)
            )
        )

        response.contentType = "application/json;charset=UTF-8"
        response.status = HttpServletResponse.SC_OK
        response.writer.write(objectMapper.writeValueAsString(responseBody))
    }
}
