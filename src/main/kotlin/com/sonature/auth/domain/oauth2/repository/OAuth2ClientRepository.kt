package com.sonature.auth.domain.oauth2.repository

import com.sonature.auth.domain.oauth2.entity.OAuth2ClientEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OAuth2ClientRepository : JpaRepository<OAuth2ClientEntity, String> {
    fun findByClientId(clientId: String): OAuth2ClientEntity?
    fun findByClientIdAndTenantId(clientId: String, tenantId: UUID): OAuth2ClientEntity?
    fun findByClientIdAndTenantIdIsNull(clientId: String): OAuth2ClientEntity?
    fun findAllByTenantId(tenantId: UUID): List<OAuth2ClientEntity>
}
