package com.codependent.reactiveworkshop

import org.junit.Test
import reactor.core.publisher.EmitterProcessor
import java.util.concurrent.CountDownLatch

class Demo6 : DemoBase() {

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
        val latch = CountDownLatch(1)

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


}