package com.sonature.auth.domain.user.repository

import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.user.model.AuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?
    fun findByProviderAndProviderId(provider: AuthProvider, providerId: String): UserEntity?
    fun existsByEmail(email: String): Boolean
}
