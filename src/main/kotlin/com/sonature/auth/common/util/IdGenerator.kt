package com.sonature.auth.common.util

import org.springframework.stereotype.Component
import java.util.UUID

interface IdGenerator {
    fun generateId(): String
}

@Component
class UuidGenerator : IdGenerator {
    override fun generateId(): String = UUID.randomUUID().toString()
}
