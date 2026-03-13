package com.sonature.auth.api.v1.paseto

import com.sonature.auth.api.common.ApiResponse
import com.sonature.auth.api.v1.paseto.dto.PasetoIssueRequest
import com.sonature.auth.api.v1.paseto.dto.PasetoIssueResponse
import com.sonature.auth.api.v1.paseto.dto.PasetoVerifyRequest
import com.sonature.auth.api.v1.paseto.dto.PasetoVerifyResponse
import com.sonature.auth.application.service.PasetoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@Tag(name = "PASETO", description = "PASETO Token API")
@RestController
@RequestMapping("/api/v1/paseto")
class PasetoController(
    private val pasetoService: PasetoService
) {
    @Operation(summary = "Issue PASETO token", description = "Issues a new PASETO v4.local token")
    @PostMapping("/issue")
    fun issue(@Valid @RequestBody request: PasetoIssueRequest): ResponseEntity<ApiResponse<PasetoIssueResponse>> {
        val expiration = request.expiresIn?.let { Duration.ofSeconds(it) }
            ?: PasetoService.DEFAULT_EXPIRATION

        val token = pasetoService.issueToken(
            subject = request.subject,
            audience = request.audience,
            expiration = expiration,
            customClaims = request.customClaims ?: emptyMap()
        )

        val response = PasetoIssueResponse(
            token = token.value,
            expiresIn = Duration.between(token.issuedAt, token.expiresAt).seconds,
            issuedAt = token.issuedAt.epochSecond
        )

        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @Operation(summary = "Verify PASETO token", description = "Verifies a PASETO token and returns its claims")
    @PostMapping("/verify")
    fun verify(@Valid @RequestBody request: PasetoVerifyRequest): ResponseEntity<ApiResponse<PasetoVerifyResponse>> {
        val claims = pasetoService.verifyToken(request.token)

        val response = PasetoVerifyResponse(
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
}
