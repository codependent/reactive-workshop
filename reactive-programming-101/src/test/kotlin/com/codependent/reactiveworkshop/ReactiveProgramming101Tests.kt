package com.codependent.reactiveworkshop

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.*
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.stream.Collectors
import java.util.stream.Stream

class ReactiveProgramming101Tests {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun streamOperatorsTest() {
        val stringsStream = getStringList().stream()
        val strings = stringsStream.map { it.toUpperCase() }
                .flatMap { Stream.of(it, it) }
                .collect(Collectors.toList())
        logger.info("{}", strings)
        assertEquals(6, strings.size)

        try {
            stringsStream.count()
            Assert.fail("No debería llegar aquí")
        } catch (ise: IllegalStateException) {
            logger.error(ise.message)
        }
    }

    @Test
    fun reactiveOperatorsTest() {
        var elements = 0
        val strings = getStringListReactive()
                .map(String::toUpperCase)
                .flatMap(this::duplicateStringReactive)
                .doOnComplete { assertEquals(6, elements) }

        strings.subscribe {
            logger.info("{}", it)
            elements++
        }

        elements = 0
        strings.subscribe {
            logger.info("{}", it)
            elements++
        }
    }

    @Test
    fun completableFutureAsyncTest() {
        logger.info("Starting")
        val strings = CompletableFuture.supplyAsync {
            logger.info("Getting list")
            getStringList()
        }.thenApplyAsync {
            val upperCaseNumbers = mutableListOf<String>()
            it.forEach { upperCaseNumbers.add(it.toUpperCase()) }
            logger.info("Uppercase strings [{}]", upperCaseNumbers)
            upperCaseNumbers
        }.thenApplyAsync {
            val doubledNumbers = mutableListOf<String>()
            it.forEach { doubledNumbers.addAll(duplicateString(it)) }
            logger.info("Doubled strings [{}]", doubledNumbers)
            doubledNumbers
        }
        logger.info("blocked")
        val gottenNumbers = strings.get()
        logger.info("Finished [{}]", gottenNumbers)
        assertEquals(6, gottenNumbers.size)
    }

    @Test
    fun reactiveAsync1Test() {
        var elements = 0
        val strings = getStringListReactive()
                .map(String::toUpperCase)
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

    @Test
    fun reactiveAsync2Test() {
        val latch = CountDownLatch(6)
        val strings = getStringListReactive()
                .map(String::toUpperCase)
                .flatMap { duplicateStringReactive(it) }
                .log()
                .doOnNext {
                    logger.info("onNext() [{}]", it)
                    latch.countDown()
                }.doOnComplete {
                    logger.info("Finished")
                }.subscribeOn(Schedulers.elastic())

        logger.info("--PRESUBSCRIBE--")
        strings.subscribe {
            logger.info("Element [{}]", it)
        }
        latch.await()
    }

    @Test
    fun hotPublisherTest() {
        val latch = CountDownLatch(10)

        val numberGenerator = counter(1000).publish()
        numberGenerator.connect()

        Thread.sleep(5000)

        numberGenerator.subscribe {
            logger.info("Element [{}]", it)
            latch.countDown()
        }

        Thread.sleep(5000)

        numberGenerator.subscribe {
            logger.info("Element2 [{}]", it)
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun backPressureTest() {
        val latch = CountDownLatch(1)

        val numberGenerator = counter(1)
        val processor = EmitterProcessor.create<Long>()
        numberGenerator.subscribeWith(processor)

        Thread.sleep(5000)

        processor.doOnError {
            latch.countDown()
        }.subscribe {
            logger.info("Element [{}]", it)
        }

        latch.await()
    }

    @Test
    fun backPressure2Text() {
        val latch = CountDownLatch(1000)

        val numberGenerator = counter(1)
        val processor = EmitterProcessor.create<Long>()
        numberGenerator.onBackpressureDrop().subscribeWith(processor)

        Thread.sleep(5000)

        processor.subscribe {
            logger.info("Element [{}]", it)
            latch.countDown()
        }

        latch.await()
    }

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

    private fun counter(emissionIntervalMillis: Long) =
            Flux.interval(Duration.ofMillis(emissionIntervalMillis))
                    .map { it }.doOnSubscribe { logger.info("Counter subscribed") }.log()


    private fun getStringList(): List<String> = listOf("uno", "dos", "tres")

    private fun getStringListReactive() = getStringList().toFlux()

    private fun duplicateString(string: String) = listOf(string, string)

    private fun duplicateStringReactive(string: String) = duplicateString(string).toFlux()
}