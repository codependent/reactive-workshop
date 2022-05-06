package com.codependent.reactiveworkshop

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch

class Demo4 : DemoBase() {

    @Test
    fun hotPublisherTest() {
        val latch = CountDownLatch(10)

        val numberGenerator = counter(1000).publish() //Convierte el Flux enConnectableFlux
        numberGenerator.connect() //Lo conecta a la fuente (counter)

        Thread.sleep(5000)

        numberGenerator.subscribe {
            logger.info("Element [{}]", it)
            Assertions.assertTrue(it >= 5)
        }

        Thread.sleep(5000)

        numberGenerator.subscribe {
            logger.info("Element2 [{}]", it)
            Assertions.assertTrue(it >= 10)
            latch.countDown()
        }

        latch.await()
    }

}
