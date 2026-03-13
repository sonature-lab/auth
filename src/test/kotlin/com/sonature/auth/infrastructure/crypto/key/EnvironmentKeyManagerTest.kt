package com.sonature.auth.infrastructure.crypto.key

import com.sonature.auth.domain.token.exception.UnsupportedAlgorithmException
import com.sonature.auth.domain.token.model.Algorithm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64
import javax.crypto.spec.SecretKeySpec
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnvironmentKeyManagerTest {

    @Test
    fun `should load HS256 key from config`() {
        val hs256Secret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

        val keyConfig = KeyConfig(
            hs256Secret = hs256Secret
        )

        val keyManager = EnvironmentKeyManager(keyConfig)

        val signingKey = keyManager.getSigningKey(Algorithm.HS256)
        val verificationKey = keyManager.getVerificationKey(Algorithm.HS256)

        assertTrue(signingKey is SecretKeySpec)
        assertEquals(signingKey, verificationKey)
    }

    @Test
    fun `hasKey should return true for configured algorithm`() {
        val hs256Secret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

        val keyConfig = KeyConfig(
            hs256Secret = hs256Secret
        )

        val keyManager = EnvironmentKeyManager(keyConfig)

        assertTrue(keyManager.hasKey(Algorithm.HS256))
    }

    @Test
    fun `hasKey should return false for unconfigured algorithm`() {
        val hs256Secret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

        val keyConfig = KeyConfig(
            hs256Secret = hs256Secret
        )

        val keyManager = EnvironmentKeyManager(keyConfig)

        assertFalse(keyManager.hasKey(Algorithm.RS256))
        assertFalse(keyManager.hasKey(Algorithm.PASETO_V4_LOCAL))
        assertFalse(keyManager.hasKey(Algorithm.PASETO_V4_PUBLIC))
    }

    @Test
    fun `should throw UnsupportedAlgorithmException for unconfigured algorithm`() {
        val keyConfig = KeyConfig(
            hs256Secret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })
        )

        val keyManager = EnvironmentKeyManager(keyConfig)

        assertThrows<UnsupportedAlgorithmException> {
            keyManager.getSigningKey(Algorithm.RS256)
        }
    }

    @Test
    fun `should throw when HS256 secret is less than 32 bytes`() {
        val shortSecret = Base64.getEncoder().encodeToString(ByteArray(16))
        val keyConfig = KeyConfig(hs256Secret = shortSecret)

        assertThrows<IllegalArgumentException> {
            EnvironmentKeyManager(keyConfig)
        }
    }

    @Test
    fun `should ignore blank hs256Secret`() {
        val keyConfig = KeyConfig(hs256Secret = "")

        val keyManager = EnvironmentKeyManager(keyConfig)

        assertFalse(keyManager.hasKey(Algorithm.HS256))
    }

    @Test
    fun `should ignore null hs256Secret`() {
        val keyConfig = KeyConfig(hs256Secret = null)

        val keyManager = EnvironmentKeyManager(keyConfig)

        assertFalse(keyManager.hasKey(Algorithm.HS256))
    }

    @Test
    fun `should load PASETO v4 local key from config`() {
        val pasetoSecret = Base64.getEncoder().encodeToString(ByteArray(32) { it.toByte() })

        val keyConfig = KeyConfig(
            pasetoSecretKey = pasetoSecret
        )

        val keyManager = EnvironmentKeyManager(keyConfig)

        assertTrue(keyManager.hasKey(Algorithm.PASETO_V4_LOCAL))

        val signingKey = keyManager.getSigningKey(Algorithm.PASETO_V4_LOCAL)
        val verificationKey = keyManager.getVerificationKey(Algorithm.PASETO_V4_LOCAL)

        assertEquals(signingKey, verificationKey)
    }

    @Test
    fun `should throw when PASETO v4 local key is not exactly 32 bytes`() {
        val wrongSizeSecret = Base64.getEncoder().encodeToString(ByteArray(16))
        val keyConfig = KeyConfig(pasetoSecretKey = wrongSizeSecret)

        assertThrows<IllegalArgumentException> {
            EnvironmentKeyManager(keyConfig)
        }
    }
}
