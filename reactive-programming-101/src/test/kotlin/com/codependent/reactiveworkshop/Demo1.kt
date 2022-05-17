package com.codependent.reactiveworkshop

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.stream.Collectors
import java.util.stream.Stream

class Demo1 : DemoBase(){

    /**
     * Lista de String, toUppercase + duplicar elementos
     */
    @Test
    fun streamOperatorsTest() {
        val stringsStream = getStringList().stream()
        val strings = stringsStream.map(String::uppercase)
                .flatMap { Stream.of(it, it) }
                .collect(Collectors.toList())
        logger.info("{}", strings)
        assertEquals(6, strings.size)

        //Los Streams no se pueden reutilizar
        assertThrows(IllegalStateException::class.java) { stringsStream.count() }
    }

    /**
     * Lista de String, toUppercase + duplicar elementos
     */
    @Test
    fun reactiveOperatorsTest() {
        var elements = 0
        val strings = getStringListReactive()
                .map(String::uppercase)
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
