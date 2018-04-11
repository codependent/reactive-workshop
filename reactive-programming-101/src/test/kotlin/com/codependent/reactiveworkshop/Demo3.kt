package com.codependent.reactiveworkshop

import org.junit.Test
import java.util.concurrent.CountDownLatch

class Demo3 : DemoBase() {

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

}