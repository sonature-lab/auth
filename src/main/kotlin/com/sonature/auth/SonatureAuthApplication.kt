package com.sonature.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ConfigurationPropertiesScan("com.sonature.auth.infrastructure")
@EnableScheduling
class SonatureAuthApplication

fun main(args: Array<String>) {
    runApplication<SonatureAuthApplication>(*args)
}
