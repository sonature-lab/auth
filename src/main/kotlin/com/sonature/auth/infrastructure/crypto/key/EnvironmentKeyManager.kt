package com.sonature.auth.infrastructure.crypto.key

import com.sonature.auth.application.port.output.KeyManager
import com.sonature.auth.domain.token.exception.UnsupportedAlgorithmException
import com.sonature.auth.domain.token.model.Algorithm
import org.springframework.stereotype.Component
import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

@Component
class EnvironmentKeyManager(
    private val keyConfig: KeyConfig
) : KeyManager {

    private val signingKeys: MutableMap<Algorithm, Key> = mutableMapOf()
    private val verificationKeys: MutableMap<Algorithm, Key> = mutableMapOf()

    init {
        loadHs256Key()
        loadRs256Keys()
        loadPasetoLocalKey()
        loadPasetoPublicKeys()
    }

    override fun getSigningKey(algorithm: Algorithm): Key =
        signingKeys[algorithm]
            ?: throw UnsupportedAlgorithmException(algorithm.value)

    override fun getVerificationKey(algorithm: Algorithm): Key =
        verificationKeys[algorithm]
            ?: throw UnsupportedAlgorithmException(algorithm.value)

    override fun hasKey(algorithm: Algorithm): Boolean =
        signingKeys.containsKey(algorithm)

    private fun loadHs256Key() {
        keyConfig.hs256Secret?.takeIf { it.isNotBlank() }?.let { secret ->
            val keyBytes = Base64.getDecoder().decode(secret)
            require(keyBytes.size >= 32) { "HS256 secret must be at least 32 bytes" }
            val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
            signingKeys[Algorithm.HS256] = secretKey
            verificationKeys[Algorithm.HS256] = secretKey
        }
    }

    private fun loadRs256Keys() {
        keyConfig.rs256PrivateKey?.takeIf { it.isNotBlank() }?.let { privateKeyPem ->
            val privateKey = parseRsaPrivateKey(privateKeyPem)
            signingKeys[Algorithm.RS256] = privateKey
        }
        keyConfig.rs256PublicKey?.takeIf { it.isNotBlank() }?.let { publicKeyPem ->
            val publicKey = parseRsaPublicKey(publicKeyPem)
            verificationKeys[Algorithm.RS256] = publicKey
        }
    }

    private fun loadPasetoLocalKey() {
        keyConfig.pasetoSecretKey?.takeIf { it.isNotBlank() }?.let { secret ->
            val keyBytes = Base64.getDecoder().decode(secret)
            require(keyBytes.size == 32) { "PASETO v4.local key must be exactly 32 bytes" }
            val secretKey = SecretKeySpec(keyBytes, "XChaCha20")
            signingKeys[Algorithm.PASETO_V4_LOCAL] = secretKey
            verificationKeys[Algorithm.PASETO_V4_LOCAL] = secretKey
        }
    }

    private fun loadPasetoPublicKeys() {
        keyConfig.pasetoPrivateKey?.takeIf { it.isNotBlank() }?.let { privateKeyPem ->
            val privateKey = parseEd25519PrivateKey(privateKeyPem)
            signingKeys[Algorithm.PASETO_V4_PUBLIC] = privateKey
        }
        keyConfig.pasetoPublicKey?.takeIf { it.isNotBlank() }?.let { publicKeyPem ->
            val publicKey = parseEd25519PublicKey(publicKeyPem)
            verificationKeys[Algorithm.PASETO_V4_PUBLIC] = publicKey
        }
    }

    private fun parseRsaPrivateKey(pem: String): Key {
        val keyContent = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec)
    }

    private fun parseRsaPublicKey(pem: String): Key {
        val keyContent = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("RSA").generatePublic(keySpec)
    }

    private fun parseEd25519PrivateKey(pem: String): Key {
        val keyContent = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("Ed25519").generatePrivate(keySpec)
    }

    private fun parseEd25519PublicKey(pem: String): Key {
        val keyContent = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        val keyBytes = Base64.getDecoder().decode(keyContent)
        val keySpec = X509EncodedKeySpec(keyBytes)
        return KeyFactory.getInstance("Ed25519").generatePublic(keySpec)
    }
}
