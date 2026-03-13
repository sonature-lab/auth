package com.sonature.auth.infrastructure.crypto.jwt

import com.sonature.auth.application.port.output.KeyManager
import com.sonature.auth.domain.token.exception.TokenExpiredException
import com.sonature.auth.domain.token.exception.TokenInvalidException
import com.sonature.auth.domain.token.exception.TokenMalformedException
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.TokenClaims
import com.sonature.auth.domain.token.model.TokenConfig
import com.sonature.auth.domain.token.model.TokenType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Hs256ProviderTest {

    private lateinit var keyManager: KeyManager
    private lateinit var provider: Hs256Provider
    private lateinit var secretKey: SecretKeySpec

    @BeforeEach
    fun setUp() {
        val keyBytes = ByteArray(32) { it.toByte() }
        secretKey = SecretKeySpec(keyBytes, "HmacSHA256")

        keyManager = mockk()
        every { keyManager.getSigningKey(Algorithm.HS256) } returns secretKey
        every { keyManager.getVerificationKey(Algorithm.HS256) } returns secretKey

        provider = Hs256Provider(keyManager)
    }

    @Test
    fun `supportedAlgorithm should return HS256`() {
        assertEquals(Algorithm.HS256, provider.supportedAlgorithm())
    }

    @Test
    fun `should issue valid JWT token`() {
        val claims = createTestClaims()
        val config = createTestConfig()

        val token = provider.issue(claims, config)

        assertNotNull(token)
        assertTrue(token.isNotBlank())
        assertEquals(3, token.split(".").size)
    }

    @Test
    fun `should verify valid JWT token`() {
        val originalClaims = createTestClaims()
        val config = createTestConfig()

        val token = provider.issue(originalClaims, config)
        val verifiedClaims = provider.verify(token)

        assertEquals(originalClaims.issuer, verifiedClaims.issuer)
        assertEquals(originalClaims.subject, verifiedClaims.subject)
        assertEquals(originalClaims.tokenId, verifiedClaims.tokenId)
        assertEquals(originalClaims.tokenType, verifiedClaims.tokenType)
    }

    @Test
    fun `should throw TokenExpiredException for expired token`() {
        val expiredClaims = createTestClaims(
            issuedAt = Instant.now().minusSeconds(3600),
            expiresAt = Instant.now().minusSeconds(1800)
        )
        val config = createTestConfig()

        val token = provider.issue(expiredClaims, config)

        assertThrows<TokenExpiredException> {
            provider.verify(token)
        }
    }

    @Test
    fun `should throw TokenInvalidException for tampered token`() {
        val claims = createTestClaims()
        val config = createTestConfig()

        val token = provider.issue(claims, config)
        val tamperedToken = token.dropLast(5) + "xxxxx"

        assertThrows<TokenInvalidException> {
            provider.verify(tamperedToken)
        }
    }

    @Test
    fun `should throw TokenMalformedException for malformed token`() {
        assertThrows<TokenMalformedException> {
            provider.verify("not-a-valid-jwt")
        }
    }

    @Test
    fun `should preserve custom claims`() {
        val customClaims = mapOf("role" to "admin", "orgId" to "org-123")
        val claims = createTestClaims(customClaims = customClaims)
        val config = createTestConfig()

        val token = provider.issue(claims, config)
        val verifiedClaims = provider.verify(token)

        assertEquals("admin", verifiedClaims.customClaims["role"])
        assertEquals("org-123", verifiedClaims.customClaims["orgId"])
    }

    @Test
    fun `should include audience in token when provided`() {
        val claims = createTestClaims(audience = "my-client-app")
        val config = createTestConfig()

        val token = provider.issue(claims, config)
        val verifiedClaims = provider.verify(token)

        assertEquals("my-client-app", verifiedClaims.audience)
    }

    @Test
    fun `should handle null audience`() {
        val claims = createTestClaims(audience = null)
        val config = createTestConfig()

        val token = provider.issue(claims, config)
        val verifiedClaims = provider.verify(token)

        assertEquals(null, verifiedClaims.audience)
    }

    private fun createTestClaims(
        issuedAt: Instant = Instant.now(),
        expiresAt: Instant = Instant.now().plusSeconds(900),
        audience: String? = null,
        customClaims: Map<String, Any> = emptyMap()
    ) = TokenClaims(
        issuer = "sonature-auth",
        subject = "user-123",
        audience = audience,
        expiresAt = expiresAt,
        issuedAt = issuedAt,
        tokenId = "jti-${System.currentTimeMillis()}",
        tokenType = TokenType.ACCESS,
        customClaims = customClaims
    )

    private fun createTestConfig() = TokenConfig(
        algorithm = Algorithm.HS256,
        issuer = "sonature-auth"
    )
}
