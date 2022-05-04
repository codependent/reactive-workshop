package com.codependent.reactiveworkshop.starwars.client

import org.junit.jupiter.api.Test
import reactor.test.StepVerifier.create
import java.net.URI

class StarWarsApiWebClientTests {
    private val starWarsApiWebClient = StarWarsApiWebClient()

    @Test
    fun findCharacter() {
        create(starWarsApiWebClient.findCharacter(1))
                .expectNextCount(1)
                .verifyComplete()
    }

    @Test
    fun findFilm() {
        create(starWarsApiWebClient.findFilm(URI("https://swapi.co/api/films/1/")))
                .expectNextCount(1)
                .verifyComplete()
    }
}
