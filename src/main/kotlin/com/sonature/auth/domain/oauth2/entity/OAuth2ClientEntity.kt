package com.sonature.auth.domain.oauth2.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "oauth2_clients",
    indexes = [
        Index(name = "idx_oauth2_clients_client_id", columnList = "clientId", unique = true),
        Index(name = "idx_oauth2_clients_tenant", columnList = "tenant_id")
    ]
)
class OAuth2ClientEntity(
    @Id
    val id: String,

    @Column(name = "client_id", nullable = false, unique = true, length = 100)
    val clientId: String,

    @Column(name = "client_secret", length = 255)
    val clientSecret: String? = null,

    @Column(name = "client_name", nullable = false, length = 255)
    val clientName: String,

    @Column(name = "redirect_uris", nullable = false, length = 2000)
    val redirectUris: String,

    @Column(name = "scopes", nullable = false, length = 1000)
    val scopes: String = "openid,profile,email",

    @Column(name = "grant_types", nullable = false, length = 500)
    val grantTypes: String = "authorization_code,refresh_token",

    @Column(name = "require_pkce", nullable = false)
    val requirePkce: Boolean = true,

    @Column(name = "access_token_ttl_seconds", nullable = false)
    val accessTokenTtlSeconds: Long = 900,

    @Column(name = "refresh_token_ttl_seconds", nullable = false)
    val refreshTokenTtlSeconds: Long = 604800,

    @Column(name = "tenant_id")
    val tenantId: UUID? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    fun getRedirectUriList(): List<String> = redirectUris.split(",").map { it.trim() }
    fun getScopeList(): List<String> = scopes.split(",").map { it.trim() }
    fun getGrantTypeList(): List<String> = grantTypes.split(",").map { it.trim() }
}
