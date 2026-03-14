package com.sonature.auth.application.service

import com.sonature.auth.domain.tenant.entity.TenantEntity
import com.sonature.auth.domain.tenant.entity.TenantMembershipEntity
import com.sonature.auth.domain.tenant.exception.AlreadyTenantMemberException
import com.sonature.auth.domain.tenant.exception.NotTenantMemberException
import com.sonature.auth.domain.tenant.exception.TenantNotFoundException
import com.sonature.auth.domain.tenant.exception.TenantSlugAlreadyExistsException
import com.sonature.auth.domain.tenant.model.TenantPlan
import com.sonature.auth.domain.tenant.model.TenantRole
import com.sonature.auth.domain.tenant.repository.TenantMembershipRepository
import com.sonature.auth.domain.tenant.repository.TenantRepository
import com.sonature.auth.domain.user.exception.UserNotFoundException
import com.sonature.auth.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TenantService(
    private val tenantRepository: TenantRepository,
    private val tenantMembershipRepository: TenantMembershipRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createTenant(name: String, slug: String, plan: TenantPlan, creatorUserId: UUID? = null): TenantEntity {
        if (tenantRepository.existsBySlug(slug)) {
            throw TenantSlugAlreadyExistsException(slug)
        }

        val tenant = TenantEntity(
            name = name,
            slug = slug,
            plan = plan
        )
        val savedTenant = tenantRepository.save(tenant)

        if (creatorUserId != null) {
            val user = userRepository.findById(creatorUserId)
                .orElseThrow { UserNotFoundException("User not found: $creatorUserId") }

            val membership = TenantMembershipEntity(
                tenant = savedTenant,
                user = user,
                role = TenantRole.OWNER
            )
            tenantMembershipRepository.save(membership)
        }

        return savedTenant
    }

    fun getTenantBySlug(slug: String): TenantEntity {
        return tenantRepository.findBySlug(slug)
            ?: throw TenantNotFoundException(slug)
    }

    @Transactional
    fun addMember(tenantSlug: String, userId: UUID, role: TenantRole = TenantRole.MEMBER): TenantMembershipEntity {
        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: throw TenantNotFoundException(tenantSlug)

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found: $userId") }

        if (tenantMembershipRepository.existsByTenantAndUser(tenant, user)) {
            throw AlreadyTenantMemberException(tenantSlug, userId.toString())
        }

        val membership = TenantMembershipEntity(
            tenant = tenant,
            user = user,
            role = role
        )
        return tenantMembershipRepository.save(membership)
    }

    fun getMembers(tenantSlug: String): List<TenantMembershipEntity> {
        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: throw TenantNotFoundException(tenantSlug)

        return tenantMembershipRepository.findAllByTenant(tenant)
    }

    fun getUserTenants(userId: UUID): List<TenantMembershipEntity> {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found: $userId") }

        return tenantMembershipRepository.findAllByUser(user)
    }

    @Transactional
    fun changeMemberRole(tenantSlug: String, userId: UUID, newRole: TenantRole): TenantMembershipEntity {
        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: throw TenantNotFoundException(tenantSlug)

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found: $userId") }

        val membership = tenantMembershipRepository.findByTenantAndUser(tenant, user)
            ?: throw NotTenantMemberException(tenantSlug, userId.toString())

        val updatedMembership = TenantMembershipEntity(
            id = membership.id,
            tenant = membership.tenant,
            user = membership.user,
            role = newRole,
            joinedAt = membership.joinedAt
        )
        return tenantMembershipRepository.save(updatedMembership)
    }

    fun getMemberRole(tenantSlug: String, userId: UUID): TenantRole {
        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: throw TenantNotFoundException(tenantSlug)

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found: $userId") }

        val membership = tenantMembershipRepository.findByTenantAndUser(tenant, user)
            ?: throw NotTenantMemberException(tenantSlug, userId.toString())

        return membership.role
    }

    @Transactional
    fun removeMember(tenantSlug: String, userId: UUID) {
        val tenant = tenantRepository.findBySlug(tenantSlug)
            ?: throw TenantNotFoundException(tenantSlug)

        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException("User not found: $userId") }

        val membership = tenantMembershipRepository.findByTenantAndUser(tenant, user)
            ?: throw NotTenantMemberException(tenantSlug, userId.toString())

        tenantMembershipRepository.delete(membership)
    }
}
