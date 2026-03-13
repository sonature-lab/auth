package com.sonature.auth.domain.token.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TokenClaimsTest {

    @Test
    fun `should create valid TokenClaims`() {
        val now = Instant.now()
        val claims = TokenClaims(
            issuer = "sonature-auth",
            subject = "user-123",
            audience = "client-app",
            expiresAt = now.plusSeconds(900),
            issuedAt = now,
            tokenId = "jti-123",
            tokenType = TokenType.ACCESS
        )

        assertEquals("sonature-auth", claims.issuer)
        assertEquals("user-123", claims.subject)
        assertEquals("client-app", claims.audience)
        assertEquals("jti-123", claims.tokenId)
        assertEquals(TokenType.ACCESS, claims.tokenType)
    }

    @Test
    fun `should allow null audience`() {
        val now = Instant.now()
        val claims = TokenClaims(
            issuer = "sonature-auth",
            subject = "user-123",
            audience = null,
            expiresAt = now.plusSeconds(900),
            issuedAt = now,
            tokenId = "jti-123",
            tokenType = TokenType.ACCESS
        )

        assertEquals(null, claims.audience)
    }

    @Test
    fun `should throw when issuer is blank`() {
        val now = Instant.now()
        assertThrows<IllegalArgumentException> {
            TokenClaims(
                issuer = "",
                subject = "user",
                audience = null,
                expiresAt = now.plusSeconds(900),
                issuedAt = now,
                tokenId = "jti",
                tokenType = TokenType.ACCESS
            )
        }
    }

    @Test
    fun `should throw when subject is blank`() {
        val now = Instant.now()
        assertThrows<IllegalArgumentException> {
            TokenClaims(
                issuer = "issuer",
                subject = "",
                audience = null,
                expiresAt = now.plusSeconds(900),
                issuedAt = now,
                tokenId = "jti",
                tokenType = TokenType.ACCESS
            )
        }
    }

    @Test
    fun `should throw when tokenId is blank`() {
        val now = Instant.now()
        assertThrows<IllegalArgumentException> {
            TokenClaims(
                issuer = "issuer",
                subject = "user",
                audience = null,
                expiresAt = now.plusSeconds(900),
                issuedAt = now,
                tokenId = "",
                tokenType = TokenType.ACCESS
            )
        }
    }

    @Test
    fun `should throw when expiresAt is before issuedAt`() {
        val now = Instant.now()
        assertThrows<IllegalArgumentException> {
            TokenClaims(
                issuer = "issuer",
                subject = "user",
                audience = null,
                expiresAt = now.minusSeconds(100),
                issuedAt = now,
                tokenId = "jti",
                tokenType = TokenType.ACCESS
            )
        }
    }

    @Test
    fun `should throw when expiresAt equals issuedAt`() {
        val now = Instant.now()
        assertThrows<IllegalArgumentException> {
            TokenClaims(
                issuer = "issuer",
                subject = "user",
                audience = null,
                expiresAt = now,
                issuedAt = now,
                tokenId = "jti",
                tokenType = TokenType.ACCESS
            )
        }
    }

    @Test
    fun `isExpired should return true for expired token`() {
        val now = Instant.now()
        val claims = TokenClaims(
            issuer = "issuer",
            subject = "user",
            audience = null,
            expiresAt = now.minusSeconds(1),
            issuedAt = now.minusSeconds(900),
            tokenId = "jti",
            tokenType = TokenType.ACCESS
        )
        assertTrue(claims.isExpired())
    }

    @Test
    fun `isExpired should return false for valid token`() {
        val now = Instant.now()
        val claims = TokenClaims(
            issuer = "issuer",
            subject = "user",
            audience = null,
            expiresAt = now.plusSeconds(900),
            issuedAt = now,
            tokenId = "jti",
            tokenType = TokenType.ACCESS
        )
        assertFalse(claims.isExpired())
    }

    @Test
    fun `isExpired should use provided time for comparison`() {
        val issuedAt = Instant.parse("2024-01-01T00:00:00Z")
        val expiresAt = Instant.parse("2024-01-01T00:15:00Z")
        val claims = TokenClaims(
            issuer = "issuer",
            subject = "user",
            audience = null,
            expiresAt = expiresAt,
            issuedAt = issuedAt,
            tokenId = "jti",
            tokenType = TokenType.ACCESS
        )

        val beforeExpiry = Instant.parse("2024-01-01T00:10:00Z")
        val afterExpiry = Instant.parse("2024-01-01T00:20:00Z")

        assertFalse(claims.isExpired(beforeExpiry))
        assertTrue(claims.isExpired(afterExpiry))
    }

    @Test
    fun `should support custom claims`() {
        val now = Instant.now()
        val customClaims = mapOf("role" to "admin", "tenant" to "acme")
        val claims = TokenClaims(
            issuer = "issuer",
            subject = "user",
            audience = null,
            expiresAt = now.plusSeconds(900),
            issuedAt = now,
            tokenId = "jti",
            tokenType = TokenType.ACCESS,
            customClaims = customClaims
        )

        assertEquals("admin", claims.customClaims["role"])
        assertEquals("acme", claims.customClaims["tenant"])
    }
}
