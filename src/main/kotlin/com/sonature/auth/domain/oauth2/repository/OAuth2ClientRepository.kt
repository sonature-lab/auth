package com.sonature.auth.domain.oauth2.repository

import com.sonature.auth.domain.oauth2.entity.OAuth2ClientEntity
import org.springframework.data.jpa.repository.JpaRepository

interface OAuth2ClientRepository : JpaRepository<OAuth2ClientEntity, String> {
    fun findByClientId(clientId: String): OAuth2ClientEntity?
}
