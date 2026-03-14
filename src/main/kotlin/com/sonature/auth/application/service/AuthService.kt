package com.sonature.auth.application.service

import com.sonature.auth.common.util.TimeProvider
import com.sonature.auth.domain.token.model.Algorithm
import com.sonature.auth.domain.token.model.TokenPair
import com.sonature.auth.domain.user.entity.UserEntity
import com.sonature.auth.domain.tenant.repository.TenantMembershipRepository
import com.sonature.auth.domain.user.exception.EmailAlreadyExistsException
import com.sonature.auth.domain.user.exception.InvalidCredentialsException
import com.sonature.auth.domain.user.exception.UserSuspendedException
import com.sonature.auth.domain.user.model.AuthProvider
import com.sonature.auth.domain.user.repository.UserRepository
import com.sonature.auth.application.usecase.TokenRefreshUseCase
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val tenantMembershipRepository: TenantMembershipRepository,
    private val tokenRefreshUseCase: TokenRefreshUseCase,
    private val passwordEncoder: PasswordEncoder,
    private val timeProvider: TimeProvider
) {
    @Transactional
    fun signup(email: String, password: String, name: String?): Pair<UserEntity, TokenPair> {
        if (userRepository.existsByEmail(email)) {
            throw EmailAlreadyExistsException(email)
        }

        val user = UserEntity(
            email = email,
            passwordHash = passwordEncoder.encode(password),
            name = name,
            provider = AuthProvider.LOCAL
        )
        val savedUser = userRepository.save(user)

        val tokenPair = tokenRefreshUseCase.issueTokenPair(
            subject = savedUser.id.toString(),
            algorithm = Algorithm.HS256,
            customClaims = buildUserClaims(savedUser)
        )

        return savedUser to tokenPair
    }

    @Transactional
    fun login(email: String, password: String): Pair<UserEntity, TokenPair> {
        val user = userRepository.findByEmail(email)
            ?: throw InvalidCredentialsException()

        if (!user.isActive()) {
            throw UserSuspendedException()
        }

        if (user.passwordHash == null || !passwordEncoder.matches(password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }

        user.recordLogin(timeProvider.now())
        userRepository.save(user)

        val tokenPair = tokenRefreshUseCase.issueTokenPair(
            subject = user.id.toString(),
            algorithm = Algorithm.HS256,
            customClaims = buildUserClaims(user)
        )

        return user to tokenPair
    }

    private fun buildUserClaims(user: UserEntity): Map<String, Any> = buildMap {
        put("email", user.email)
        put("provider", user.provider.name)
        user.name?.let { put("name", it) }

        val memberships = tenantMembershipRepository.findAllByUser(user)
        if (memberships.isNotEmpty()) {
            put("tenants", memberships.map { membership ->
                mapOf(
                    "slug" to membership.tenant.slug,
                    "role" to membership.role.name
                )
            })
        }
    }
}
