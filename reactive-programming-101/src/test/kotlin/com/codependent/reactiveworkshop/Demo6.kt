package com.codependent.reactiveworkshop
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CountDownLatch

class Demo6 : DemoBase() {

    @Test
    fun backPressureTest() {

        val latch = CountDownLatch(1)

        val sink = Sinks.many().multicast().directAllOrNothing<Int>()

        val result: Sinks.EmitResult = sink.tryEmitNext(1)

        assertEquals(FAIL_ZERO_SUBSCRIBER, result)

        val sink2 = Sinks.many().multicast().onBackpressureBuffer<Int>(10)

        sink2.asFlux()
            .publishOn(Schedulers.boundedElastic())
            .subscribe {
                logger.info("Subscriber1 {}", it)
            }

        sink2.asFlux()
            .publishOn(Schedulers.boundedElastic())
            .subscribe {
                logger.info("Subscriber2 {}", it)
                Thread.sleep(2000)
            }

        (1..1000)
            .forEach {
                val res = sink2.tryEmitNext(it)
                logger.info("Result {}", res)
                latch.countDown()
            }

        latch.await()
    }

}
