package com.codependent.reactiveworkshop

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.stream.Collectors
import java.util.stream.Stream

class Demo1 : DemoBase(){

    @Test
    fun streamOperatorsTest() {
        val stringsStream = getStringList().stream()
        val strings = stringsStream.map(String::toUpperCase)
                .flatMap { Stream.of(it, it) }
                .collect(Collectors.toList())
        logger.info("{}", strings)
        assertEquals(6, strings.size)

        try {
            stringsStream.count()
            Assert.fail("No debería llegar aquí")
        } catch (ise: IllegalStateException) {
            // Los streams no son reusables
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

        //Todavía no ha pasado nada...

        strings.subscribe {
            logger.info("{}", it)
            elements++
        }

        //Publishers reusables
        elements = 0
        strings.subscribe {
            logger.info("{}", it)
            elements++
        }
    }
}