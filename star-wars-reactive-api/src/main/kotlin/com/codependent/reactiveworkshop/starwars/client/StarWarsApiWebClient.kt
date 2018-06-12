package com.codependent.reactiveworkshop.starwars.client

import com.codependent.reactiveworkshop.starwars.dto.Character
import com.codependent.reactiveworkshop.starwars.dto.Film
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI

@Component
class StarWarsApiWebClient {

    private val webClient = WebClient.builder()

    fun findCharacter(id: Int): Mono<Character> {
        return webClient.baseUrl("https://swapi.co/api/").build()
                .get().uri("/people/$id/")
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON).retrieve()
                .bodyToMono(Character::class.java).log()
    }

    fun findFilm(uri: URI): Mono<Film> {
        val webClient = WebClient.builder()
        return webClient.build()
                .get().uri(uri)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.APPLICATION_JSON).retrieve()
                .bodyToMono(Film::class.java).log()
    }


}