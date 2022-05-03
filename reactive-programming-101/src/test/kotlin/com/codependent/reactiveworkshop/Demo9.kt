package com.codependent.reactiveworkshop

import org.junit.jupiter.api.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.CountDownLatch

class Demo9 : DemoBase() {

    /**
     * No parallelism
     */
    @Test
    fun blocking1() {
        val latch = CountDownLatch(1)

        val range = Flux.range(1, 10)
            .map { expensiveCalculation(it) }
            .doOnComplete { latch.countDown() }

        range.subscribe {
            logger.info("{}", it)
        }

        latch.await()
    }


    /**
     * No parallelism
     */
    @Test
    fun blocking2() {
        val latch = CountDownLatch(1)

        val range = Flux.range(1, 10)
            .map { expensiveCalculation(it) }
            .doOnComplete { latch.countDown() }
            .subscribeOn(Schedulers.boundedElastic())

        range.subscribe {
            logger.info("{}", it)
        }

        latch.await()
    }

    /**
     * No parallelism
     */
    @Test
    fun blocking3() {
        val latch = CountDownLatch(1)

        val range = Flux.range(1, 10)
            .flatMap { expensiveCalculation(it).toMono().subscribeOn(Schedulers.boundedElastic()) }
            .doOnComplete { latch.countDown() }

        range.subscribe {
            logger.info("{}", it)
        }

        latch.await()

    }

    /**
     * Parallelism
     */
    @Test
    fun nonBlocking() {
        val latch = CountDownLatch(1)

        val range = Flux.range(1, 10)
            .flatMap {
                Mono.fromCallable { expensiveCalculation(it) }.subscribeOn(Schedulers.boundedElastic())
            }.doOnComplete { latch.countDown() }

        range.subscribe {
            logger.info("{}", it)
        }

        latch.await()

    }

}
