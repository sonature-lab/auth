package com.sonature.auth.api.v1.jwt

import com.sonature.auth.api.common.ApiResponse
import com.sonature.auth.api.v1.jwt.dto.JwtIssueRequest
import com.sonature.auth.api.v1.jwt.dto.JwtIssueResponse
import com.sonature.auth.api.v1.jwt.dto.JwtRefreshRequest
import com.sonature.auth.api.v1.jwt.dto.JwtRefreshResponse
import com.sonature.auth.api.v1.jwt.dto.JwtTokenPairResponse
import com.sonature.auth.api.v1.jwt.dto.JwtVerifyRequest
import com.sonature.auth.api.v1.jwt.dto.JwtVerifyResponse
import com.sonature.auth.application.service.JwtService
import com.sonature.auth.application.usecase.TokenRefreshUseCase
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.TokenConfig
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@Tag(name = "JWT", description = "JWT Token API")
@RestController
@RequestMapping("/api/v1/jwt")
class JwtController(
    private val jwtService: JwtService,
    private val tokenRefreshUseCase: TokenRefreshUseCase
) {
    @Operation(summary = "Issue JWT token", description = "Issues a new JWT access token")
    @PostMapping("/issue")
    fun issue(@Valid @RequestBody request: JwtIssueRequest): ResponseEntity<ApiResponse<JwtIssueResponse>> {
        val algorithm = Algorithm.fromValue(request.algorithm)
        val expiration = request.expiresIn?.let { Duration.ofSeconds(it) }
            ?: TokenConfig.DEFAULT_ACCESS_EXPIRATION

        val token = jwtService.issueAccessToken(
            subject = request.subject,
            audience = request.audience,
            algorithm = algorithm,
            expiration = expiration,
            customClaims = request.customClaims ?: emptyMap()
        )

        val response = JwtIssueResponse(
            accessToken = token.value,
            expiresIn = Duration.between(token.issuedAt, token.expiresAt).seconds,
            issuedAt = token.issuedAt.epochSecond
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @Operation(summary = "Verify JWT token", description = "Verifies a JWT token and returns its claims")
    @PostMapping("/verify")
    fun verify(@Valid @RequestBody request: JwtVerifyRequest): ResponseEntity<ApiResponse<JwtVerifyResponse>> {
        val algorithm = Algorithm.fromValue(request.algorithm)
        val claims = jwtService.verifyToken(request.token, algorithm)

        val response = JwtVerifyResponse(
            valid = true,
            subject = claims.subject,
            issuer = claims.issuer,
            audience = claims.audience,
            expiresAt = claims.expiresAt.epochSecond,
            issuedAt = claims.issuedAt.epochSecond,
            tokenId = claims.tokenId,
            tokenType = claims.tokenType.name,
            customClaims = claims.customClaims
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @Operation(summary = "Issue JWT token pair", description = "Issues a new JWT access token and refresh token pair")
    @PostMapping("/issue-pair")
    fun issuePair(@Valid @RequestBody request: JwtIssueRequest): ResponseEntity<ApiResponse<JwtTokenPairResponse>> {
        val algorithm = Algorithm.fromValue(request.algorithm)
        val accessExpiration = request.expiresIn?.let { Duration.ofSeconds(it) }
            ?: TokenConfig.DEFAULT_ACCESS_EXPIRATION

        val tokenPair = tokenRefreshUseCase.issueTokenPair(
            subject = request.subject,
            audience = request.audience,
            algorithm = algorithm,
            accessExpiration = accessExpiration,
            customClaims = request.customClaims ?: emptyMap()
        )

        val response = JwtTokenPairResponse(
            accessToken = tokenPair.accessToken.value,
            refreshToken = tokenPair.refreshToken.value,
            accessExpiresIn = Duration.between(
                tokenPair.accessToken.issuedAt,
                tokenPair.accessToken.expiresAt
            ).seconds,
            refreshExpiresIn = Duration.between(
                tokenPair.refreshToken.issuedAt,
                tokenPair.refreshToken.expiresAt
            ).seconds,
            issuedAt = tokenPair.accessToken.issuedAt.epochSecond
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @Operation(summary = "Refresh JWT tokens", description = "Refreshes JWT tokens using a valid refresh token. Implements token rotation.")
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: JwtRefreshRequest): ResponseEntity<ApiResponse<JwtRefreshResponse>> {
        val algorithm = Algorithm.fromValue(request.algorithm)

        val tokenPair = tokenRefreshUseCase.refreshTokens(
            refreshToken = request.refreshToken,
            algorithm = algorithm
        )

        val response = JwtRefreshResponse(
            accessToken = tokenPair.accessToken.value,
            refreshToken = tokenPair.refreshToken.value,
            accessExpiresIn = Duration.between(
                tokenPair.accessToken.issuedAt,
                tokenPair.accessToken.expiresAt
            ).seconds,
            refreshExpiresIn = Duration.between(
                tokenPair.refreshToken.issuedAt,
                tokenPair.refreshToken.expiresAt
            ).seconds,
            issuedAt = tokenPair.accessToken.issuedAt.epochSecond
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
