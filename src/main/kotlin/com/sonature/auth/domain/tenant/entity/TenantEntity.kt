package com.sonature.auth.domain.tenant.entity

import com.sonature.auth.domain.tenant.model.TenantPlan
import com.sonature.auth.domain.tenant.model.TenantStatus
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
    name = "tenants",
    indexes = [
        Index(name = "idx_tenants_slug", columnList = "slug", unique = true)
    ]
)
class TenantEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, unique = true, length = 100)
    val slug: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var plan: TenantPlan = TenantPlan.FREE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TenantStatus = TenantStatus.ACTIVE,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
