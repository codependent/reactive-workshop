package com.codependent.reactiveworkshop

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.CompletableFuture

class Demo2 : DemoBase() {

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

}