package com.sonature.auth.infrastructure.scheduler

import com.sonature.auth.application.service.RefreshTokenService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RefreshTokenScheduler(
    private val refreshTokenService: RefreshTokenService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    fun cleanupExpiredTokens() {
        val deleted = refreshTokenService.cleanupExpiredTokens()
        logger.info("Scheduled cleanup: deleted $deleted expired refresh tokens")
    }
}
