package com.sonature.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan("com.sonature.auth.infrastructure")
class SonatureAuthApplication

fun main(args: Array<String>) {
    runApplication<SonatureAuthApplication>(*args)
}
