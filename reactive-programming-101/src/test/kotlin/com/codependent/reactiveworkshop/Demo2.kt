package com.codependent.reactiveworkshop

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
import java.util.stream.Stream

class Demo2 : DemoBase() {

    /**
     * Lista de String, toUppercase + duplicar elementos - Asincrono
     */
    @Test
    fun completableFutureAsyncTest() {
        logger.info("Starting")
        val strings = CompletableFuture.supplyAsync {
            logger.info("Getting list")
            getStringList()
        }.thenApply {
            val upperCaseNumbers = mutableListOf<String>()
            it.forEach { upperCaseNumbers.add(it.toUpperCase()) }
            logger.info("Uppercase strings [{}]", upperCaseNumbers)
            upperCaseNumbers
        }.thenApply {
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
    fun streamOperatorsAsyncTest() {
        val stringsStream = getStringList().parallelStream()
        val strings = stringsStream.map(String::toUpperCase)
                .flatMap {
                    logger.info("{}", it)
                    Stream.of(it, it)
                }
                .collect(Collectors.toList())
        logger.info("Strings {}", strings)
        assertEquals(6, strings.size)

    }

}