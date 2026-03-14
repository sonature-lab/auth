package com.sonature.auth.domain.tenant

import com.sonature.auth.application.service.RefreshTokenService
import com.sonature.auth.application.usecase.TokenRefreshUseCase
import com.sonature.auth.domain.oauth2.entity.OAuth2ClientEntity
import com.sonature.auth.domain.oauth2.repository.OAuth2ClientRepository
import com.sonature.auth.domain.refresh.repository.RefreshTokenRepository
import com.sonature.auth.domain.tenant.context.TenantContext
import com.sonature.auth.domain.tenant.context.TenantContextHolder
import com.sonature.auth.domain.tenant.entity.TenantEntity
import com.sonature.auth.domain.tenant.entity.TenantMembershipEntity
import com.sonature.auth.domain.tenant.exception.TenantMismatchException
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
    private lateinit var tokenRefreshUseCase: TokenRefreshUseCase

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var oauth2ClientRepository: OAuth2ClientRepository

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

    @Test
    fun `cross-tenant token refresh throws TenantMismatchException`() {
        val tokenPair = tokenRefreshUseCase.issueTokenPair(
            subject = user.id.toString(),
            tenantId = tenantA.id
        )

        TenantContextHolder.set(
            TenantContext(
                tenantSlug = tenantB.slug,
                tenantId = tenantB.id,
                userId = user.id,
                role = TenantRole.MEMBER
            )
        )

        try {
            assertThrows(TenantMismatchException::class.java) {
                tokenRefreshUseCase.refreshTokens(tokenPair.refreshToken.value)
            }
        } finally {
            TenantContextHolder.clear()
        }
    }

    @Test
    fun `global token refresh succeeds in any tenant context`() {
        val tokenPair = tokenRefreshUseCase.issueTokenPair(
            subject = user.id.toString(),
            tenantId = null
        )

        TenantContextHolder.set(
            TenantContext(
                tenantSlug = tenantA.slug,
                tenantId = tenantA.id,
                userId = user.id,
                role = TenantRole.OWNER
            )
        )

        try {
            val newTokenPair = tokenRefreshUseCase.refreshTokens(tokenPair.refreshToken.value)
            assertNotNull(newTokenPair.accessToken)
            assertNotNull(newTokenPair.refreshToken)
        } finally {
            TenantContextHolder.clear()
        }
    }

    @Test
    fun `revokeAllBySubjectAndTenant does not revoke global tokens`() {
        val now = Instant.now()
        val subject = user.id.toString()

        refreshTokenService.storeRefreshToken(
            tokenValue = "tenant-scoped-${System.nanoTime()}",
            subject = subject,
            clientId = null,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            tenantId = tenantA.id
        )

        refreshTokenService.storeRefreshToken(
            tokenValue = "global-token-${System.nanoTime()}",
            subject = subject,
            clientId = null,
            issuedAt = now,
            expiresAt = now.plusSeconds(3600),
            tenantId = null
        )

        val revokedCount = refreshTokenRepository.revokeAllBySubjectAndTenant(
            subject,
            tenantA.id,
            Instant.now()
        )

        assertEquals(1, revokedCount)

        val activeTokens = refreshTokenRepository.findAllActiveBySubject(subject)
        assertEquals(1, activeTokens.size)
        assertNull(activeTokens[0].tenantId)
    }

    @Test
    fun `OAuth2Client tenant-scoped query returns only matching tenant clients`() {
        val ts = System.nanoTime()

        val clientA = oauth2ClientRepository.save(
            OAuth2ClientEntity(
                id = "client-a-$ts",
                clientId = "client-a-$ts",
                clientName = "Client A",
                redirectUris = "https://a.example.com/callback",
                tenantId = tenantA.id
            )
        )

        oauth2ClientRepository.save(
            OAuth2ClientEntity(
                id = "client-b-$ts",
                clientId = "client-b-$ts",
                clientName = "Client B",
                redirectUris = "https://b.example.com/callback",
                tenantId = tenantB.id
            )
        )

        val tenantAClients = oauth2ClientRepository.findAllByTenantId(tenantA.id)
        assertTrue(tenantAClients.any { it.clientId == clientA.clientId })
        assertTrue(tenantAClients.none { it.tenantId == tenantB.id })

        val foundByClientIdAndTenant = oauth2ClientRepository.findByClientIdAndTenantId(clientA.clientId, tenantA.id)
        assertNotNull(foundByClientIdAndTenant)
        assertEquals(tenantA.id, foundByClientIdAndTenant!!.tenantId)

        val notFound = oauth2ClientRepository.findByClientIdAndTenantId(clientA.clientId, tenantB.id)
        assertNull(notFound)
    }

    @Test
    fun `OAuth2Client global client (null tenant) query works`() {
        val ts = System.nanoTime()
        val globalClientId = "global-client-$ts"

        oauth2ClientRepository.save(
            OAuth2ClientEntity(
                id = globalClientId,
                clientId = globalClientId,
                clientName = "Global Client",
                redirectUris = "https://global.example.com/callback",
                tenantId = null
            )
        )

        oauth2ClientRepository.save(
            OAuth2ClientEntity(
                id = "tenant-client-$ts",
                clientId = "tenant-client-$ts",
                clientName = "Tenant Client",
                redirectUris = "https://tenant.example.com/callback",
                tenantId = tenantA.id
            )
        )

        val globalClient = oauth2ClientRepository.findByClientIdAndTenantIdIsNull(globalClientId)
        assertNotNull(globalClient)
        assertNull(globalClient!!.tenantId)

        val tenantClientAsGlobal = oauth2ClientRepository.findByClientIdAndTenantIdIsNull("tenant-client-$ts")
        assertNull(tenantClientAsGlobal)
    }
}
