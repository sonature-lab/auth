package com.sonature.auth.domain.tenant.repository

import com.sonature.auth.domain.tenant.entity.TenantEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantRepository : JpaRepository<TenantEntity, UUID> {
    fun findBySlug(slug: String): TenantEntity?
    fun existsBySlug(slug: String): Boolean
}
