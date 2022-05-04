package com.codependent.reactiveworkshop.starwars

import com.codependent.reactiveworkshop.starwars.dto.Character
import com.codependent.reactiveworkshop.starwars.dto.Film
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import java.net.URI

@ExtendWith(SpringExtension::class)
@SpringBootTest
@AutoConfigureWebTestClient(timeout = "10000")
class StarWarsApiApplicationTests(@Autowired private val webClient: WebTestClient) {
    @Test
    fun contextLoads() {
    }

    @Test
    fun getCharacter() {
        val films = listOf(
            URI("https://swapi.dev/api/films/1/"),
            URI("https://swapi.dev/api/films/2/"),
            URI("https://swapi.dev/api/films/3/"),
            URI("https://swapi.dev/api/films/6/")
        )
        val expected = Character("Luke Skywalker", films)
        val actual = webClient.get()
            .uri("/functionalapi/people/1")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .exchange()
            .expectStatus().isOk
            .expectBody<Character>()
            .returnResult().responseBody

        assertEquals(expected, actual)
    }

    @Test
    fun getCharacterFilms() {
        val actual = webClient.get()
            .uri("/functionalapi/people/1/films")
            .accept(MediaType.APPLICATION_JSON_UTF8)
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Film>()
            .returnResult().responseBody
        assertEquals(4, actual?.size)
    }
}
