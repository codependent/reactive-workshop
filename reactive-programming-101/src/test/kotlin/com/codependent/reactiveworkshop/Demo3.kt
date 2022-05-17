package com.codependent.reactiveworkshop

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CountDownLatch

class Demo3 : DemoBase() {

    /**
     * Async (not paralell) on same thread
     */
    @Test
    fun reactiveAsync1Test() {
        var elements = 0
        val strings = getStringListReactive()
                .map(String::uppercase)
                .flatMap { duplicateStringReactive(it) }
                .log()
                .doOnNext {
                    logger.info("onNext() [{}]", it)
                    elements++
                }.doOnComplete {
                    logger.info("Finished")
                    assertEquals(6, elements)
                }
        logger.info("--PRESUBSCRIBE--")

        strings.subscribe {
            logger.info("Element [{}]", it)
        }
    }

    /**
     * Async (not paralell) on different thread
     */
    @Test
    fun reactiveAsync2Test() {
        val latch = CountDownLatch(6)
        val strings = getStringListReactive()
                .map(String::uppercase)
                .flatMap { duplicateStringReactive(it) }
                .log()
                .doOnNext {
                    logger.info("onNext() [{}]", it)
                    latch.countDown()
                }.doOnComplete {
                    logger.info("Finished")
                }.subscribeOn(Schedulers.boundedElastic())

        logger.info("--PRESUBSCRIBE--")
        strings.subscribe {
            logger.info("Element [{}]", it)
        }
        latch.await()
    }

    /**
     * Async (and paralell) on different thread
     */
    @Test
    fun reactiveAsync3Test() {
        val latch = CountDownLatch(6)
        val strings = getStringListReactive()
            .map(String::uppercase)
            .flatMap {
                Flux.defer {
                    duplicateStringReactive(it)
                }.subscribeOn(Schedulers.boundedElastic())
            }
            .log()
            .doOnNext {
                logger.info("onNext() [{}]", it)
                latch.countDown()
            }.doOnComplete {
                logger.info("Finished")
            }

        logger.info("--PRESUBSCRIBE--")
        strings.subscribe {
            logger.info("Element [{}]", it)
        }
        latch.await()
    }

}
