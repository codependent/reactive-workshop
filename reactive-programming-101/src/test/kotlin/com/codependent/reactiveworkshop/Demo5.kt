package com.codependent.reactiveworkshop

import org.junit.Assert.assertTrue
import org.junit.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.UnicastProcessor
import java.util.concurrent.CountDownLatch

class Demo5 : DemoBase() {

    /**
     * Invocación directa a Sink
     */
    @Test
    fun unicastProcessor() {
        val latch = CountDownLatch(1)

        val processor = UnicastProcessor.create<Long>()

        processor.log()
                .doOnComplete(latch::countDown)
                .subscribe {
                    logger.info("Element [{}]", it)
                }

        processor.onNext(23)
        processor.onNext(45)
        processor.onComplete()

        latch.await()
    }

    /**
     * Sólo se permite un suscriptor a UnicastProcessor
     */
    @Test
    fun unicastProcessor2() {
        val processor = UnicastProcessor.create<Long>()

        processor.subscribe {
            logger.info("Element [{}]", it)
        }

        processor.doOnError {
            assertTrue(it is IllegalStateException)
        }.subscribe {
            logger.info("Element2 [{}]", it)
        }

    }

    /**
     * Suscripción a un Publisher upstream
     * Buffer de elementos generados antes de la suscripción
     */
    @Test
    fun unicastProcessor3() {
        val latch = CountDownLatch(10)

        val numberGenerator: Flux<Long> = counter(1000)
        val processor = UnicastProcessor.create<Long>()

        numberGenerator.subscribeWith(processor)

        Thread.sleep(5000)

        processor.subscribe {
            logger.info("Element [{}]", it)
            latch.countDown()
        }

        latch.await()
    }

    /**
     * Suscripción a un Publisher upstream
     * Múltiples suscriptores mediante un ConnectableFlux
     * @see https://stackoverflow.com/questions/49536849/why-does-a-unicastprocessor-plus-connectableflux-send-previously-emitted-items-d/49538007#49538007
     */
    @Test
    fun unicastProcessor4() {
        val latch = CountDownLatch(15)

        val numberGenerator: Flux<Long> = counter(1000)
        val processor = UnicastProcessor.create<Long>()
        numberGenerator.subscribeWith(processor)
        val connectableFlux = processor.doOnSubscribe { logger.info("subscribed!") }.log().publish().autoConnect()

        Thread.sleep(5000)

        connectableFlux.subscribe {
            logger.info("Element [{}]", it)
            latch.countDown()
        }

        Thread.sleep(5000)

        connectableFlux.subscribe {
            logger.info("Element2 [{}]", it)
        }

        latch.await()
    }

    /**
     * Suscripción a un Publisher upstream
     * Múltiples suscriptores mediante un ConnectableFlux
     * @see https://stackoverflow.com/questions/49536849/why-does-a-unicastprocessor-plus-connectableflux-send-previously-emitted-items-d/49538007#49538007
     */
    @Test
    fun unicastProcessor5() {
        val latch = CountDownLatch(15)

        val numberGenerator: Flux<Long> = counter(1000)
        val processor = UnicastProcessor.create<Long>()
        numberGenerator.subscribeWith(processor)
        val connectableFlux = processor.doOnSubscribe { logger.info("subscribed!") }.log().publish()
        connectableFlux.connect()

        Thread.sleep(5000)

        connectableFlux.subscribe {
            logger.info("Element [{}]", it)
            latch.countDown()
        }

        Thread.sleep(5000)

        connectableFlux.subscribe {
            logger.info("Element2 [{}]", it)
        }

        latch.await()
    }


}