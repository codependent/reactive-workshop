package com.codependent.workshop

import io.micrometer.core.instrument.Metrics.counter
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.EmitterProcessor
import reactor.core.publisher.Flux
import reactor.core.publisher.UnicastProcessor
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.stream.Collectors
import java.util.stream.Stream

class Tests101 {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun streamOperators() {
        val numbers = getList().stream()
                .map { it.toUpperCase() }
                .flatMap { Stream.of(it, it) }.collect(Collectors.toList())
        println(numbers)
        assertEquals(6, numbers.size)
    }

    @Test
    fun reactiveOperators() {
        var elements = 0
        val numbers = getReactiveList()
                .map { it.toUpperCase() }
                .flatMap { it -> Flux.just(it, it) }
                .doOnComplete { assertEquals(6, elements) }
        numbers.subscribe {
            println(it)
            elements++
        }
    }

    @Test
    fun completableFutureAsync() {
        logger.info("Starting")
        val numbers = CompletableFuture.supplyAsync {
            logger.info("Getting list")
            getList()
        }.thenApplyAsync {
            val upperCaseNumbers = mutableListOf<String>()
            it.forEach { upperCaseNumbers.add(uppercaseService(it)) }
            logger.info("Uppercase numbers [{}]", upperCaseNumbers)
            upperCaseNumbers
        }.thenApplyAsync {
            val doubledNumbers = mutableListOf<String>()
            it.forEach { doubledNumbers.addAll(doubleListService(it)) }
            logger.info("Doubled numbers [{}]", doubledNumbers)
            doubledNumbers
        }
        val gottenNumbers = numbers.get()
        logger.info("Finished [{}]", gottenNumbers)
        assertEquals(6, gottenNumbers.size)
    }

    @Test
    fun reactiveAsync1() {
        var elements = 0
        val numbers = getReactiveList()
                .map { uppercaseService(it) }
                .flatMap { doubleReactiveService(it) }
                .log()
                .doOnNext {
                    logger.info("onNext() [{}]", it)
                    elements++
                }.doOnComplete {
                    logger.info("Finished")
                    assertEquals(6, elements)
                }.subscribeOn(Schedulers.elastic())
        logger.info("--PRESUBSCRIBE--")
        numbers.subscribe {
            logger.info("Element [{}]", it)
        }
    }

    @Test
    fun reactiveAsync2() {
        val latch = CountDownLatch(6)
        val numbers = getReactiveList()
                .map { uppercaseService(it) }
                .flatMap { doubleReactiveService(it) }
                .log()
                .doOnNext {
                    logger.info("onNext() [{}]", it)
                    latch.countDown()
                }.doOnComplete {
                    logger.info("Finished")
                }.subscribeOn(Schedulers.elastic())

        logger.info("--PRESUBSCRIBE--")
        numbers.subscribe {
            logger.info("Element [{}]", it)
        }
        latch.await()
    }

    @Test
    fun hotPublisher() {
        val latch = CountDownLatch(20)

        val numberGenerator = counter(1000).subscribeOn(Schedulers.elastic()).publish()
        numberGenerator.connect()

        Thread.sleep(5000)

        numberGenerator.subscribe {
            logger.info("Element [{}]", it)
        }

        Thread.sleep(5000)

        numberGenerator.subscribe {
            logger.info("Element2 [{}]", it)
        }

        latch.await()
    }

    @Test
    fun backPressure() {
        val latch = CountDownLatch(20)

        val numberGenerator = counter(1)
        val processor = EmitterProcessor.create<Long>()
        numberGenerator.onBackpressureDrop().subscribeWith(processor)

        Thread.sleep(5000)

        processor.publish().autoConnect()
                .subscribe {
                    logger.info("Element [{}]", it)
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
        val connectableFlux = processor.doOnSubscribe { println("subscribed!") }.log().publish().autoConnect()

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
        val connectableFlux = processor.doOnSubscribe { println("subscribed!") }.log().publish()
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

    private fun counter(emissionIntervalMillis: Long) =
            Flux.interval(Duration.ofMillis(emissionIntervalMillis))
                    .map { it }.doOnSubscribe{ println("Counter subscribed")}.log()


    private fun getReactiveList() = Flux.just("uno", "dos", "tres")

    private fun getList(): List<String> = listOf("uno", "dos", "tres")

    private fun uppercaseService(string: String) = string.toUpperCase()

    private fun doubleListService(string: String) = listOf(string, string)

    private fun doubleReactiveService(string: String) = Flux.just(string, string)
}