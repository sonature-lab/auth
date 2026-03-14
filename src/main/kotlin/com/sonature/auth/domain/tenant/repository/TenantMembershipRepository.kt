package com.sonature.auth.domain.tenant.repository

import com.sonature.auth.domain.tenant.entity.TenantEntity
import com.sonature.auth.domain.tenant.entity.TenantMembershipEntity
import com.sonature.auth.domain.user.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TenantMembershipRepository : JpaRepository<TenantMembershipEntity, UUID> {
    fun findByTenantAndUser(tenant: TenantEntity, user: UserEntity): TenantMembershipEntity?
    fun findAllByUser(user: UserEntity): List<TenantMembershipEntity>
    fun findAllByTenant(tenant: TenantEntity): List<TenantMembershipEntity>
    fun existsByTenantAndUser(tenant: TenantEntity, user: UserEntity): Boolean
}
