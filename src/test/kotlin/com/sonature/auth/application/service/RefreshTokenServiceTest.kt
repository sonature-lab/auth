package com.sonature.auth.application.service

import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.refresh.entity.RefreshTokenEntity
import com.sonature.auth.domain.refresh.repository.RefreshTokenRepository
import com.sonature.auth.domain.token.exception.RefreshTokenRevokedException
import com.sonature.auth.domain.token.exception.RefreshTokenReusedException
import com.sonature.auth.domain.token.exception.TokenExpiredException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.ObjectProvider
import java.time.Instant
import java.util.UUID

class RefreshTokenServiceTest {

    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var timeProvider: TimeProvider
    private lateinit var selfProvider: ObjectProvider<RefreshTokenService>
    private lateinit var refreshTokenService: RefreshTokenService

    private val now = Instant.parse("2026-03-13T10:00:00Z")
    private val tenantId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val subject = "user-abc"

    @BeforeEach
    fun setUp() {
        refreshTokenRepository = mockk()
        timeProvider = mockk()
        selfProvider = mockk()

        every { timeProvider.now() } returns now
        every { selfProvider.getObject() } answers {
            // return a self-reference for circular proxy
            refreshTokenService
        }

        refreshTokenService = RefreshTokenService(refreshTokenRepository, timeProvider, selfProvider)
    }

    // --- validateAndConsume ---

    @Test
    fun `validateAndConsume should return entity when token is valid`() {
        val tokenValue = "valid-refresh-token"
        val entity = buildActiveEntity(subject, tenantId)
        every { refreshTokenRepository.findByTokenHashForUpdate(any()) } returns entity

        val result = refreshTokenService.validateAndConsume(tokenValue)

        assertNotNull(result)
        assertEquals(subject, result.subject)
        assertEquals(tenantId, result.tenantId)
    }

    @Test
    fun `validateAndConsume should throw RefreshTokenRevokedException when token not found`() {
        every { refreshTokenRepository.findByTokenHashForUpdate(any()) } returns null

        assertThrows<RefreshTokenRevokedException> {
            refreshTokenService.validateAndConsume("nonexistent-token")
        }
    }

    @Test
    fun `validateAndConsume should throw TokenExpiredException when token is expired`() {
        val expiredEntity = buildEntity(
            subject = subject,
            expiresAt = now.minusSeconds(60),
            revokedAt = null
        )
        every { refreshTokenRepository.findByTokenHashForUpdate(any()) } returns expiredEntity

        assertThrows<TokenExpiredException> {
            refreshTokenService.validateAndConsume("expired-token")
        }
    }

    @Test
    fun `validateAndConsume should throw RefreshTokenReusedException when token is revoked`() {
        val revokedEntity = buildEntity(
            subject = subject,
            expiresAt = now.plusSeconds(3600),
            revokedAt = now.minusSeconds(10)
        )
        every { refreshTokenRepository.findByTokenHashForUpdate(any()) } returns revokedEntity
        every { refreshTokenRepository.revokeAllBySubject(subject, now) } returns 2

        assertThrows<RefreshTokenReusedException> {
            refreshTokenService.validateAndConsume("revoked-token")
        }

        verify { refreshTokenRepository.revokeAllBySubject(subject, now) }
    }

    // --- rotateToken ---

    @Test
    fun `rotateToken should preserve tenantId from old entity`() {
        val oldEntity = buildActiveEntity(subject, tenantId)
        val newTokenValue = "new-refresh-token-value"
        val newIssuedAt = now
        val newExpiresAt = now.plusSeconds(7 * 24 * 3600)

        val savedSlot = slot<RefreshTokenEntity>()
        every { refreshTokenRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = refreshTokenService.rotateToken(oldEntity, newTokenValue, newIssuedAt, newExpiresAt)

        assertEquals(subject, result.subject)
        assertEquals(tenantId, result.tenantId)
        assertEquals(oldEntity.clientId, result.clientId)
    }

    @Test
    fun `rotateToken should revoke old entity`() {
        val oldEntity = buildActiveEntity(subject, tenantId)
        val savedEntities = mutableListOf<RefreshTokenEntity>()
        every { refreshTokenRepository.save(capture(savedEntities)) } answers { savedEntities.last() }

        refreshTokenService.rotateToken(
            oldEntity,
            "new-token-value",
            now,
            now.plusSeconds(3600)
        )

        val savedOld = savedEntities.find { it.id == oldEntity.id }
        assertNotNull(savedOld?.revokedAt)
    }

    @Test
    fun `rotateToken should preserve null tenantId for global tokens`() {
        val globalEntity = buildActiveEntity(subject, tenantId = null)
        val savedSlot = slot<RefreshTokenEntity>()
        every { refreshTokenRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        val result = refreshTokenService.rotateToken(
            globalEntity,
            "new-global-token",
            now,
            now.plusSeconds(3600)
        )

        assertEquals(null, result.tenantId)
    }

    // --- revokeAllTokensForSubject ---

    @Test
    fun `revokeAllTokensForSubject should revoke all active tokens for subject`() {
        every { refreshTokenRepository.revokeAllBySubject(subject, now) } returns 3

        val count = refreshTokenService.revokeAllTokensForSubject(subject)

        assertEquals(3, count)
        verify { refreshTokenRepository.revokeAllBySubject(subject, now) }
    }

    @Test
    fun `revokeAllTokensForSubject should return zero when no active tokens exist`() {
        every { refreshTokenRepository.revokeAllBySubject(subject, now) } returns 0

        val count = refreshTokenService.revokeAllTokensForSubject(subject)

        assertEquals(0, count)
    }

    // --- cleanupExpiredTokens ---

    @Test
    fun `cleanupExpiredTokens should delete expired tokens and return count`() {
        every { refreshTokenRepository.deleteExpiredTokens(now) } returns 5

        val count = refreshTokenService.cleanupExpiredTokens()

        assertEquals(5, count)
        verify { refreshTokenRepository.deleteExpiredTokens(now) }
    }

    // --- helpers ---

    private fun buildActiveEntity(subject: String, tenantId: UUID?) = buildEntity(
        subject = subject,
        expiresAt = now.plusSeconds(7 * 24 * 3600),
        revokedAt = null,
        tenantId = tenantId
    )

    private fun buildEntity(
        subject: String,
        expiresAt: Instant,
        revokedAt: Instant?,
        tenantId: UUID? = null
    ) = RefreshTokenEntity(
        tokenHash = "fakehash-${UUID.randomUUID()}",
        subject = subject,
        clientId = null,
        tenantId = tenantId,
        issuedAt = now.minusSeconds(60),
        expiresAt = expiresAt,
        revokedAt = revokedAt
    )
}
