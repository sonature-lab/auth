package com.sonature.auth.common.util

import org.springframework.stereotype.Component
import java.time.Instant

interface TimeProvider {
    fun now(): Instant
}

@Component
class SystemTimeProvider : TimeProvider {
    override fun now(): Instant = Instant.now()
}
