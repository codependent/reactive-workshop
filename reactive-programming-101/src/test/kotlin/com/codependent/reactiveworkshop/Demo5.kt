package com.codependent.reactiveworkshop

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import reactor.core.publisher.Sinks
import java.util.concurrent.CountDownLatch

class Demo5 : DemoBase() {

    /**
     * Sólo soporta un suscriptor
     */
    @Test
    fun manyUnicastSink() {
        val latch = CountDownLatch(1)

        val processor =  Sinks.many().unicast().onBackpressureBuffer<Long>()

        processor.tryEmitNext(12)
        processor.tryEmitNext(23)

        processor.asFlux().log()
            .doOnComplete(latch::countDown)
            .subscribe {
                logger.info("Element1 [{}]", it)
            }

        processor.asFlux().log()
            .doOnError { assertEquals(IllegalStateException::class, it.javaClass) }
            .doOnComplete(latch::countDown)
            .subscribe {
                logger.info("Element2 [{}]", it)
            }

        processor.tryEmitNext(45)
        processor.tryEmitComplete()

        latch.await()
    }

    /**
     * Sólo emite el primer elemento haciendo replay a los siguientes suscriptores
     */
    @Test
    fun unicastProcessor2() {
        val processor = Sinks.one<Long>()

        val mono = processor.asMono()

        mono.subscribe {
            logger.info("Element [{}]", it)
            assertEquals(1L, it)
        }

        mono.subscribe {
            logger.info("Element2 [{}]", it)
            assertEquals(1L, it)
        }

        processor.tryEmitValue(1L)
        processor.tryEmitValue(2L)

        mono.subscribe {
            logger.info("Element3 [{}]", it)
            assertEquals(1L, it)
        }

    }

    /**
     * Suscripción a un Publisher upstream
     * EmitterProcessor se bloquea cuando su buffer se llena
     * El primer suscriptor recibe todos los elementos cacheados
     * El segundo suscriptor sólo recibe los nuevos elementos
     */
    @Test
    fun multicastReplayLimitSink() {
        val latch = CountDownLatch(100)

        val processor =  Sinks.many().replay().limit<Int>(100)
        (1..1000).forEach {
            processor.tryEmitNext(it)
        }
        processor.tryEmitComplete()

        processor.asFlux().doOnComplete {
            logger.info("Subscriber1 onComplete()")
        }.subscribe {
            logger.info("Subscriber1 onNext() {}", it)
        }

        processor.asFlux().doOnComplete {
            logger.info("Subscriber2 onComplete()")
            latch.countDown()
        }.subscribe {
            logger.info("Subscriber2 onNext() {}", it)
            latch.countDown()
        }

        Thread.sleep(2000)
        latch.await()
    }

    @Test
    fun multicastNewData() {
        val sink = Sinks.many().multicast().onBackpressureBuffer<Int>()

        sink.asFlux().doOnComplete {
            logger.info("Subscriber1 onComplete()")
        }.subscribe {
            logger.info("Subscriber1 onNext() {}", it)
            assertTrue(it == 1 || it == 2)
        }

        sink.tryEmitNext(1)
        sink.tryEmitNext(2)

        sink.asFlux().doOnComplete {
            logger.info("Subscriber2 onComplete()")
        }.subscribe {
            logger.info("Subscriber2 onNext() {}", it)
            assertEquals(3, it)
        }

        sink.tryEmitNext(3)
        sink.tryEmitComplete()
    }

}
