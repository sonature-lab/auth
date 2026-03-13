package com.sonature.auth.domain.user.entity

import com.sonature.auth.domain.user.model.AuthProvider
import com.sonature.auth.domain.user.model.UserStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_provider", columnList = "provider, providerId")
    ]
)
class UserEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 255)
    val email: String,

    @Column(name = "password_hash", length = 255)
    var passwordHash: String? = null,

    @Column(length = 100)
    var name: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: AuthProvider = AuthProvider.LOCAL,

    @Column(name = "provider_id", length = 255)
    val providerId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "last_login_at")
    var lastLoginAt: Instant? = null
) {
    fun isActive(): Boolean = status == UserStatus.ACTIVE

    fun recordLogin(now: Instant) {
        this.lastLoginAt = now
        this.updatedAt = now
    }
}
