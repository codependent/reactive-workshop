package com.codependent.reactiveworkshop

import org.junit.Assert.assertTrue
import org.junit.Test
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.ReplayProcessor
import reactor.core.publisher.UnicastProcessor
import reactor.core.scheduler.Schedulers
import java.time.Duration
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

    /**
     * Suscripción a un Publisher upstream
     * EmitterProcessor se bloquea cuando su buffer se llena
     * El primer suscriptor recibe todos los elementos cacheados
     * El segundo suscriptor sólo recibe los nuevos elementos
     */
    @Test
    fun emitterProcessor() {
        val latch = CountDownLatch(1)

        val numbers: Flux<Int> = Flux.range(1, 1000).log()
        val processor = EmitterProcessor.create<Int>(100)

        numbers.subscribeOn(Schedulers.elastic()).subscribeWith(processor)

        Thread.sleep(2000)

        processor.subscribe {
            logger.info("Subscriber1 onNext {}", it)
        }

        processor.doOnComplete {
            logger.info("Subscriber2 onComplete()")
            latch.countDown()
        }.subscribe {
            logger.info("Subscriber2 onNext() {}", it)
        }

        latch.await()
    }

    /**
     * Cachea el último
     */
    @Test
    fun replayProcessor() {
        val latch = CountDownLatch(1)

        val numbers: Flux<Int> = Flux.range(1, 1000).log()
        val processor = ReplayProcessor.cacheLast<Int>()

        numbers.subscribeOn(Schedulers.elastic()).subscribeWith(processor)

        Thread.sleep(2000)

        processor.subscribe {
            logger.info("Subscriber1 onNext {}", it)
        }

        processor.doOnComplete {
            logger.info("Subscriber2 onComplete()")
            latch.countDown()
        }.subscribe {
            logger.info("Subscriber2 onNext() {}", it)
        }

        latch.await()
    }

    /**
     * Cachea con límite de 10
     */
    @Test
    fun replayProcessor2() {
        val latch = CountDownLatch(1)

        val numbers: Flux<Int> = Flux.range(1, 1000).log()
        val processor = ReplayProcessor.create<Int>(10)

        numbers.subscribeOn(Schedulers.elastic()).subscribeWith(processor)

        Thread.sleep(2000)

        processor.subscribe {
            logger.info("Subscriber1 onNext {}", it)
        }

        processor.doOnComplete {
            logger.info("Subscriber2 onComplete()")
            latch.countDown()
        }.subscribe {
            logger.info("Subscriber2 onNext() {}", it)
        }

        latch.await()
    }

    /**
     * Cachea sin límite
     */
    @Test
    fun replayProcessor3() {
        val latch = CountDownLatch(1)

        val numbers: Flux<Int> = Flux.range(1, 1000).log()
        val processor = ReplayProcessor.create<Int>()

        numbers.subscribeOn(Schedulers.elastic()).subscribeWith(processor)

        Thread.sleep(2000)

        processor.subscribe {
            logger.info("Subscriber1 onNext {}", it)
        }

        processor.doOnComplete {
            logger.info("Subscriber2 onComplete()")
            latch.countDown()
        }.subscribe {
            logger.info("Subscriber2 onNext() {}", it)
        }

        latch.await()
    }

    /**
     * Cachea con ventana temporal
     */
    @Test
    fun replayProcessor4() {
        val latch = CountDownLatch(20)

        val numbers: Flux<Long> = counter(100)
        val processor = ReplayProcessor.createTimeout<Long>(Duration.ofMillis(500))

        numbers.subscribeOn(Schedulers.elastic()).subscribeWith(processor)

        Thread.sleep(1000)

        processor.subscribe {
            logger.info("Subscriber1 onNext {}", it)
        }

        Thread.sleep(1000)

        processor.doOnComplete {
            logger.info("Subscriber2 onComplete()")
        }.subscribe {
            logger.info("Subscriber2 onNext() {}", it)
            latch.countDown()
        }

        latch.await()
    }

}