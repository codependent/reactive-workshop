package com.codependent.reactiveworkshop

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
import java.time.Duration

open class DemoBase {

    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    protected fun getStringList(): List<String> = listOf("uno", "dos", "tres")

    protected fun getStringListReactive() = getStringList().toFlux()

    protected fun duplicateString(string: String) = listOf(string, string)

    protected fun duplicateStringReactive(string: String) = duplicateString(string).toFlux()

    protected fun counter(emissionIntervalMillis: Long) =
            Flux.interval(Duration.ofMillis(emissionIntervalMillis))
                    .doOnSubscribe { logger.info("Counter subscribed") }
                    .log()
}