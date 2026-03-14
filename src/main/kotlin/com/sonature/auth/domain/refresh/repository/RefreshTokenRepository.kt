package com.sonature.auth.domain.refresh.repository

import com.sonature.auth.domain.refresh.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {

    fun findByTokenHash(tokenHash: String): RefreshTokenEntity?

    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.subject = :subject AND r.revokedAt IS NULL")
    fun findAllActiveBySubject(@Param("subject") subject: String): List<RefreshTokenEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :revokedAt WHERE r.subject = :subject AND r.revokedAt IS NULL")
    fun revokeAllBySubject(@Param("subject") subject: String, @Param("revokedAt") revokedAt: Instant): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.revokedAt = :revokedAt WHERE r.subject = :subject AND r.tenantId = :tenantId AND r.revokedAt IS NULL")
    fun revokeAllBySubjectAndTenant(@Param("subject") subject: String, @Param("tenantId") tenantId: UUID, @Param("revokedAt") revokedAt: Instant): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :now")
    fun deleteExpiredTokens(@Param("now") now: Instant): Int
}
