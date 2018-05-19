package com.codependent.reactiveworkshop

import org.junit.Assert
import org.junit.Test
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.UnicastProcessor
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.util.concurrent.CountDownLatch

class ReactiveProgramming101Tests : DemoBase() {

    /**
     * No parallelism
     */
    @Test
    fun parallel1() {
        val latch = CountDownLatch(1)

        val range = Flux.range(1, 10)
                .map {
                    expensiveCalculation(it)
                }.doOnComplete { latch.countDown() }

        range.subscribe {
            logger.info("{}", it)
        }

        latch.await()

    }

    private fun expensiveCalculation(number: Int): Long {
        val random = (Math.random() * 5000).toLong()
        Thread.sleep(random)
        return number * random
    }

    /**
     * No parallelism
     */
    @Test
    fun parallel2() {
        val latch = CountDownLatch(1)

        val range = Flux.range(1, 10)
                .map {
                    expensiveCalculation(it)
                }.doOnComplete { latch.countDown() }
                .subscribeOn(Schedulers.elastic())

        range.subscribe {
            logger.info("{}", it)
        }

        latch.await()

    }

    /**
     * No parallelism
     */
    @Test
    fun parallel3() {
        val latch = CountDownLatch(1)

        val range = Flux.range(1, 10)
                .flatMap {
                    expensiveCalculation(it).toMono()
                            .subscribeOn(Schedulers.elastic())
                }.doOnComplete { latch.countDown() }


        range.subscribe {
            logger.info("{}", it)
        }

        latch.await()

    }

    /**
     * Parallelism
     */
    @Test
    fun parallel4() {
        val latch = CountDownLatch(1)

        val range = Flux.range(1, 10)
                .flatMap {
                    Mono.fromCallable { expensiveCalculation(it) }
                            .subscribeOn(Schedulers.elastic())
                }.doOnComplete { latch.countDown() }


        range.subscribe {
            logger.info("{}", it)
        }

        latch.await()

    }

    /**
     * Direct Sink invocation
     */
    @Test
    fun unicastProcessor() {
        val latch = CountDownLatch(1)

        val processor = UnicastProcessor.create<Long>()

        processor.log().doOnComplete { latch.countDown() }
                .subscribe {
                    logger.info("Element [{}]", it)
                }

        processor.onNext(23)
        processor.onNext(45)
        processor.onComplete()

        latch.await()
    }

    /**
     * Only one UnicastProcessor subscription allowed
     */
    @Test
    fun unicastProcessor2() {
        val processor = UnicastProcessor.create<Long>()

        processor.subscribe {
            logger.info("Element [{}]", it)
        }

        processor.doOnError {
            Assert.assertTrue(it is IllegalStateException)
        }.subscribe {
            logger.info("Element2 [{}]", it)
        }

    }

    /**
     * Subscription to an upstream Publisher
     * Buffer of elements emitted before subscription
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
     * Subscription to an upstream Publisher
     * Multiple subscribers to processor through a ConnectableFlux
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
     * Subscription to an upstream Publisher
     * Multiple subscribers to processor through a ConnectableFlux
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