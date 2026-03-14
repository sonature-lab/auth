package com.sonature.auth.domain.tenant

import com.sonature.auth.application.service.RefreshTokenService
import com.sonature.auth.domain.refresh.repository.RefreshTokenRepository
import com.sonature.auth.domain.tenant.entity.TenantEntity
import com.sonature.auth.domain.tenant.entity.TenantMembershipEntity
import com.sonature.auth.domain.tenant.model.TenantPlan
import com.sonature.auth.domain.tenant.model.TenantRole
import com.sonature.auth.domain.tenant.repository.TenantMembershipRepository
import com.sonature.auth.domain.tenant.repository.TenantRepository
import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TenantIsolationIntegrationTest {

    @Autowired
    private lateinit var refreshTokenService: RefreshTokenService

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var tenantRepository: TenantRepository

    @Autowired
    private lateinit var tenantMembershipRepository: TenantMembershipRepository

    private lateinit var user: UserEntity
    private lateinit var tenantA: TenantEntity
    private lateinit var tenantB: TenantEntity

    @BeforeEach
    fun setUp() {
        val ts = System.nanoTime()
        user = userRepository.save(UserEntity(email = "isolation-$ts@test.com", name = "Isolation Test"))
        tenantA = tenantRepository.save(TenantEntity(name = "Tenant A", slug = "tenant-a-$ts", plan = TenantPlan.FREE))
        tenantB = tenantRepository.save(TenantEntity(name = "Tenant B", slug = "tenant-b-$ts", plan = TenantPlan.PRO))

        tenantMembershipRepository.save(TenantMembershipEntity(tenant = tenantA, user = user, role = TenantRole.OWNER))
        tenantMembershipRepository.save(TenantMembershipEntity(tenant = tenantB, user = user, role = TenantRole.MEMBER))
    }

    @Test
    fun `refresh token stores tenantId when provided`() {
        val now = Instant.now()
        val entity = refreshTokenService.storeRefreshToken(
            tokenValue = "tenant-token-${System.nanoTime()}",
            subject = user.id.toString(),
            clientId = null,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            tenantId = tenantA.id
        )

        assertEquals(tenantA.id, entity.tenantId)
    }

    @Test
    fun `refresh token without tenantId is global`() {
        val now = Instant.now()
        val entity = refreshTokenService.storeRefreshToken(
            tokenValue = "global-token-${System.nanoTime()}",
            subject = user.id.toString(),
            clientId = null,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600)
        )

        assertNull(entity.tenantId)
    }

    @Test
    fun `revoking tokens for tenant A does not affect tenant B tokens`() {
        val now = Instant.now()

        refreshTokenService.storeRefreshToken(
            tokenValue = "token-a-${System.nanoTime()}",
            subject = user.id.toString(),
            clientId = null,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            tenantId = tenantA.id
        )

        refreshTokenService.storeRefreshToken(
            tokenValue = "token-b-${System.nanoTime()}",
            subject = user.id.toString(),
            clientId = null,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            tenantId = tenantB.id
        )

        val revokedCount = refreshTokenRepository.revokeAllBySubjectAndTenant(
            user.id.toString(),
            tenantA.id,
            Instant.now()
        )

        assertEquals(1, revokedCount)

        val activeTokens = refreshTokenRepository.findAllActiveBySubject(user.id.toString())
        assertEquals(1, activeTokens.size)
        assertEquals(tenantB.id, activeTokens[0].tenantId)
    }

    @Test
    fun `revoking all tokens by subject revokes across all tenants`() {
        val now = Instant.now()
        val subject = "revoke-all-${System.nanoTime()}"

        refreshTokenService.storeRefreshToken(
            tokenValue = "token-a2-${System.nanoTime()}",
            subject = subject,
            clientId = null,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            tenantId = tenantA.id
        )

        refreshTokenService.storeRefreshToken(
            tokenValue = "token-b2-${System.nanoTime()}",
            subject = subject,
            clientId = null,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            tenantId = tenantB.id
        )

        val revokedCount = refreshTokenRepository.revokeAllBySubject(subject, Instant.now())
        assertEquals(2, revokedCount)

        val activeTokens = refreshTokenRepository.findAllActiveBySubject(subject)
        assertTrue(activeTokens.isEmpty())
    }

    @Test
    fun `token rotation preserves tenantId`() {
        val now = Instant.now()

        val original = refreshTokenService.storeRefreshToken(
            tokenValue = "original-${System.nanoTime()}",
            subject = user.id.toString(),
            clientId = null,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            tenantId = tenantA.id
        )

        val rotated = refreshTokenService.rotateToken(
            oldEntity = original,
            newTokenValue = "rotated-${System.nanoTime()}",
            newIssuedAt = now.plusSeconds(60),
            newExpiresAt = now.plusSeconds(3660)
        )

        assertEquals(tenantA.id, rotated.tenantId)
        assertTrue(original.isRevoked())
    }

    @Test
    fun `tenant membership isolates users across tenants`() {
        val ts = System.nanoTime()
        val userX = userRepository.save(UserEntity(email = "userx-$ts@test.com", name = "User X"))

        tenantMembershipRepository.save(TenantMembershipEntity(tenant = tenantA, user = userX, role = TenantRole.VIEWER))

        val membershipsA = tenantMembershipRepository.findAllByTenant(tenantA)
        val membershipsB = tenantMembershipRepository.findAllByTenant(tenantB)

        assertTrue(membershipsA.any { it.user.id == userX.id })
        assertFalse(membershipsB.any { it.user.id == userX.id })
    }

    @Test
    fun `user can have different roles across tenants`() {
        val membershipsA = tenantMembershipRepository.findAllByTenant(tenantA)
        val membershipsB = tenantMembershipRepository.findAllByTenant(tenantB)

        val roleA = membershipsA.first { it.user.id == user.id }.role
        val roleB = membershipsB.first { it.user.id == user.id }.role

        assertEquals(TenantRole.OWNER, roleA)
        assertEquals(TenantRole.MEMBER, roleB)
    }
}
