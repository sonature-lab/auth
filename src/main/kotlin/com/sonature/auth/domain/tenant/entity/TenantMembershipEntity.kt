package com.sonature.auth.domain.tenant.entity

import com.sonature.auth.domain.tenant.model.TenantRole
import com.sonature.auth.domain.user.entity.UserEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "tenant_memberships",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_tenant_membership_tenant_user",
            columnNames = ["tenant_id", "user_id"]
        )
    ],
    indexes = [
        Index(name = "idx_tenant_membership_tenant", columnList = "tenant_id"),
        Index(name = "idx_tenant_membership_user", columnList = "user_id")
    ]
)
class TenantMembershipEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    val tenant: TenantEntity,

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: TenantRole = TenantRole.MEMBER,

    @Column(name = "joined_at", nullable = false)
    val joinedAt: Instant = Instant.now()
)
