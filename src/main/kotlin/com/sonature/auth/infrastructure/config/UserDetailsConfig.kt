package com.sonature.auth.infrastructure.config

import com.sonature.auth.domain.user.repository.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

@Configuration
class UserDetailsConfig {

    @Bean
    fun userDetailsService(userRepository: UserRepository): UserDetailsService {
        return UserDetailsService { email ->
            val user = userRepository.findByEmail(email)
                ?: throw UsernameNotFoundException("User not found: $email")

            User.builder()
                .username(user.email)
                .password(user.passwordHash ?: "")
                .roles("USER")
                .disabled(!user.isActive())
                .build()
        }
    }
}
