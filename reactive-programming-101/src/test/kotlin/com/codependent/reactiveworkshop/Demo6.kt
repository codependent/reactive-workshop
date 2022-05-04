package com.codependent.reactiveworkshop

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CountDownLatch

class Demo6 : DemoBase() {

    @Test
    fun backPressureDirectAllOrNothingTest() {

        val latch = CountDownLatch(1)
        val latch2 = CountDownLatch(1)

        val sink = Sinks.many().multicast().directAllOrNothing<Int>()

        val result: Sinks.EmitResult = sink.tryEmitNext(1)

        assertEquals(FAIL_ZERO_SUBSCRIBER, result)

        sink.asFlux()
            .publishOn(Schedulers.boundedElastic())
            .subscribe {
                logger.info("Subscriber1 {}", it)
            }

        sink.asFlux()
            .publishOn(Schedulers.boundedElastic())
            .doOnComplete { latch2.countDown() }
            .subscribe {
                logger.info("Subscriber2 {}", it)
                Thread.sleep(100)
                if (it == 256) {
                    latch.countDown()
                }
            }

        (1..400)
            .forEach {
                val res = sink.tryEmitNext(it)
                logger.info("Result {}", res)
            }

        latch.await()

        sink.tryEmitNext(5000)
        sink.tryEmitComplete()

        latch2.await()

    }

    @Test
    fun backPressureOnBackpressureBufferTest() {

        val latch = CountDownLatch(1)
        val latch2 = CountDownLatch(1)

        val sink = Sinks.many().multicast().onBackpressureBuffer<Int>()

        //Warms up with 256 cached elements when no subscribers

        (1..300)
            .forEach {
                val res = sink.tryEmitNext(it)
                logger.info("Result {}", res)
            }

        sink.asFlux()
            .publishOn(Schedulers.boundedElastic())
            .subscribe {
                logger.info("Subscriber1 {}", it)
            }

        sink.asFlux()
            .publishOn(Schedulers.boundedElastic())
            .doOnComplete { latch2.countDown() }
            .subscribe {
                logger.info("Subscriber2 {}", it)
                Thread.sleep(100)
                if (it == 812) {
                    latch.countDown()
                }
            }

        (301..1000)
            .forEach {
                val res = sink.tryEmitNext(it)
                logger.info("Result {}", res)
            }

        latch.await()

        sink.tryEmitNext(5000)
        sink.tryEmitComplete()

        latch2.await()
    }


}
