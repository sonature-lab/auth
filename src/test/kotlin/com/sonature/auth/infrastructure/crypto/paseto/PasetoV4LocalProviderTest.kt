package com.sonature.auth.infrastructure.crypto.paseto

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sonature.auth.application.port.output.KeyManager
import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.token.exception.TokenExpiredException
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

class PasetoV4LocalProviderTest {

    private lateinit var keyManager: KeyManager
    private lateinit var timeProvider: TimeProvider
    private lateinit var provider: PasetoV4LocalProvider
    private lateinit var secretKey: SecretKeySpec

    private val now = Instant.now()

    @BeforeEach
    fun setUp() {
        // 32 bytes key for PASETO v4.local (XChaCha20-Poly1305)
        val keyBytes = ByteArray(32) { it.toByte() }
        secretKey = SecretKeySpec(keyBytes, "XChaCha20")

        keyManager = mockk()
        timeProvider = mockk()
        every { keyManager.getSigningKey(Algorithm.PASETO_V4_LOCAL) } returns secretKey
        every { keyManager.getVerificationKey(Algorithm.PASETO_V4_LOCAL) } returns secretKey
        every { timeProvider.now() } returns now

        provider = PasetoV4LocalProvider(keyManager, jacksonObjectMapper(), timeProvider)
    }

    @Test
    fun `supportedAlgorithm should return PASETO_V4_LOCAL`() {
        assertEquals(Algorithm.PASETO_V4_LOCAL, provider.supportedAlgorithm())
    }

    @Test
    fun `should issue valid PASETO v4 local token`() {
        val claims = createTestClaims()
        val config = createTestConfig()

        val token = provider.issue(claims, config)

        assertNotNull(token)
        assertTrue(token.startsWith("v4.local."), "Token should start with 'v4.local.' prefix")
    }

    @Test
    fun `should verify valid PASETO token`() {
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
    fun `should throw TokenMalformedException for invalid token`() {
        assertThrows<TokenMalformedException> {
            provider.verify("not-a-valid-paseto-token")
        }
    }

    @Test
    fun `should throw TokenMalformedException for tampered token`() {
        val claims = createTestClaims()
        val config = createTestConfig()

        val token = provider.issue(claims, config)
        val tamperedToken = token.dropLast(10) + "xxxxxxxxxx"

        assertThrows<TokenMalformedException> {
            provider.verify(tamperedToken)
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
        algorithm = Algorithm.PASETO_V4_LOCAL,
        issuer = "sonature-auth"
    )
}
