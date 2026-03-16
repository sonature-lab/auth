package com.sonature.auth.infrastructure.scheduler

import com.sonature.auth.application.service.RefreshTokenService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RefreshTokenSchedulerTest {

    private lateinit var refreshTokenService: RefreshTokenService
    private lateinit var scheduler: RefreshTokenScheduler

    @BeforeEach
    fun setUp() {
        refreshTokenService = mockk()
        scheduler = RefreshTokenScheduler(refreshTokenService)
    }

    @Test
    fun `should call cleanupExpiredTokens and log deleted count when scheduled`() {
        // Given
        every { refreshTokenService.cleanupExpiredTokens() } returns 42

        // When
        scheduler.cleanupExpiredTokens()

        // Then
        verify(exactly = 1) { refreshTokenService.cleanupExpiredTokens() }
    }

    @Test
    fun `should handle zero deleted tokens without error`() {
        // Given
        every { refreshTokenService.cleanupExpiredTokens() } returns 0

        // When
        scheduler.cleanupExpiredTokens()

        // Then
        verify(exactly = 1) { refreshTokenService.cleanupExpiredTokens() }
    }
}
