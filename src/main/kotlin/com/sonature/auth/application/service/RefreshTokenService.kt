package com.sonature.auth.application.service

import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.refresh.entity.RefreshTokenEntity
import com.sonature.auth.domain.refresh.repository.RefreshTokenRepository
import com.sonature.auth.domain.token.exception.RefreshTokenRevokedException
import com.sonature.auth.domain.token.exception.RefreshTokenReusedException
import com.sonature.auth.domain.token.exception.TokenExpiredException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val timeProvider: TimeProvider,
    private val selfProvider: org.springframework.beans.factory.ObjectProvider<RefreshTokenService>
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val self: RefreshTokenService by lazy { selfProvider.getObject() }

    @Transactional
    fun storeRefreshToken(
        tokenValue: String,
        subject: String,
        clientId: String?,
        issuedAt: Instant,
        expiresAt: Instant,
        tenantId: UUID? = null
    ): RefreshTokenEntity {
        val tokenHash = hashToken(tokenValue)
        val entity = RefreshTokenEntity(
            tokenHash = tokenHash,
            subject = subject,
            clientId = clientId,
            tenantId = tenantId,
            issuedAt = issuedAt,
            expiresAt = expiresAt
        )
        return refreshTokenRepository.save(entity)
    }

    @Transactional
    fun validateAndConsume(tokenValue: String): RefreshTokenEntity {
        val tokenHash = hashToken(tokenValue)
        val now = timeProvider.now()

        val entity = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw RefreshTokenRevokedException("Refresh token not found")

        if (entity.isRevoked()) {
            logger.warn("Refresh token reuse detected for subject: ${entity.subject}. Revoking all tokens.")
            self.revokeAllTokensForSubject(entity.subject)
            throw RefreshTokenReusedException()
        }

        if (entity.isExpired(now)) {
            throw TokenExpiredException(
                message = "The refresh token has expired",
                expiredAt = entity.expiresAt
            )
        }

        return entity
    }

    @Transactional
    fun rotateToken(
        oldEntity: RefreshTokenEntity,
        newTokenValue: String,
        newIssuedAt: Instant,
        newExpiresAt: Instant
    ): RefreshTokenEntity {
        val now = timeProvider.now()
        val newTokenHash = hashToken(newTokenValue)

        val newEntity = RefreshTokenEntity(
            tokenHash = newTokenHash,
            subject = oldEntity.subject,
            clientId = oldEntity.clientId,
            tenantId = oldEntity.tenantId,
            issuedAt = newIssuedAt,
            expiresAt = newExpiresAt
        )
        val savedEntity = refreshTokenRepository.save(newEntity)

        oldEntity.revoke(now, savedEntity.id)
        refreshTokenRepository.save(oldEntity)

        return savedEntity
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun revokeAllTokensForSubject(subject: String): Int {
        val now = timeProvider.now()
        val count = refreshTokenRepository.revokeAllBySubject(subject, now)
        logger.info("Revoked $count refresh tokens for subject: $subject")
        return count
    }

    @Transactional
    fun cleanupExpiredTokens(): Int {
        val now = timeProvider.now()
        return refreshTokenRepository.deleteExpiredTokens(now)
    }

    private fun hashToken(tokenValue: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(tokenValue.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
