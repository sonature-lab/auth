package com.sonature.auth.domain.refresh.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "refresh_tokens",
    indexes = [
        Index(name = "idx_refresh_tokens_hash", columnList = "tokenHash", unique = true),
        Index(name = "idx_refresh_tokens_subject", columnList = "subject"),
        Index(name = "idx_refresh_tokens_expires", columnList = "expiresAt"),
        Index(name = "idx_refresh_tokens_tenant", columnList = "tenant_id")
    ]
)
class RefreshTokenEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    val tokenHash: String,

    @Column(nullable = false, length = 255)
    val subject: String,

    @Column(name = "client_id", length = 255)
    val clientId: String? = null,

    @Column(name = "tenant_id")
    val tenantId: UUID? = null,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: Instant,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "replaced_by")
    var replacedBy: UUID? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun isValid(now: Instant): Boolean =
        revokedAt == null && expiresAt.isAfter(now)

    fun isRevoked(): Boolean = revokedAt != null

    fun isExpired(now: Instant): Boolean = now.isAfter(expiresAt)

    fun revoke(now: Instant, newTokenId: UUID? = null) {
        this.revokedAt = now
        this.replacedBy = newTokenId
    }
}
