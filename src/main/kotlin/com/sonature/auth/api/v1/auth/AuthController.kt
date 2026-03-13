package com.sonature.auth.api.v1.auth

import com.sonature.auth.api.common.ApiResponse
import com.sonature.auth.api.v1.auth.dto.AuthResponse
import com.sonature.auth.api.v1.auth.dto.LoginRequest
import com.sonature.auth.api.v1.auth.dto.SignupRequest
import com.sonature.auth.api.v1.auth.dto.UserInfo
import com.sonature.auth.application.service.AuthService
import com.sonature.auth.domain.token.model.TokenPair
import com.sonature.auth.domain.user.entity.UserEntity
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@Tag(name = "Auth", description = "Authentication API")
@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService
) {
    @Operation(summary = "Sign up", description = "Create a new account with email and password")
    @PostMapping("/signup")
    fun signup(@Valid @RequestBody request: SignupRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val (user, tokenPair) = authService.signup(
            email = request.email,
            password = request.password,
            name = request.name
        )
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(toAuthResponse(user, tokenPair)))
    }

    @Operation(summary = "Login", description = "Authenticate with email and password")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val (user, tokenPair) = authService.login(
            email = request.email,
            password = request.password
        )
        return ResponseEntity.ok(ApiResponse.success(toAuthResponse(user, tokenPair)))
    }

    private fun toAuthResponse(user: UserEntity, tokenPair: TokenPair): AuthResponse {
        return AuthResponse(
            user = UserInfo(
                id = user.id.toString(),
                email = user.email,
                name = user.name,
                provider = user.provider.name
            ),
            accessToken = tokenPair.accessToken.value,
            refreshToken = tokenPair.refreshToken.value,
            accessExpiresIn = Duration.between(
                tokenPair.accessToken.issuedAt,
                tokenPair.accessToken.expiresAt
            ).seconds,
            refreshExpiresIn = Duration.between(
                tokenPair.refreshToken.issuedAt,
                tokenPair.refreshToken.expiresAt
            ).seconds
        )
    }
}
