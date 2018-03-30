package com.codependent.cryptomarket.ui.web

import com.codependent.cryptomarket.ui.dto.Character
import com.codependent.cryptomarket.ui.dto.Film
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.util.function.Function

@RestController
class StarWarsApiController {

    @GetMapping("/api/people/{id}")
    fun getCharacter(@PathVariable id: Int): Mono<Character> {

        return queryCharacter(id)
    }

    @GetMapping("/api/people/{id}/films")
    fun getCharacterFilms(@PathVariable id: Int): Flux<Film> {
        return queryCharacter(id)
                .flatMapMany { Flux.just(*it.films.toTypedArray()) }
                .flatMap({
                    val webClient = WebClient.builder()
                    webClient.baseUrl("https://swapi.co/api/").build()
                            .get().uri(it)
                            .header("Content-Type", APPLICATION_JSON_VALUE)
                            .accept(APPLICATION_JSON).retrieve()
                            .bodyToMono(Film::class.java)

                }, 100)
    }

    @GetMapping("/api/people/{id}/films?serial=true")
    fun getCharacterFilmsSerial(@PathVariable id: Int): Flux<Film> {
        val films = Flux.defer {
            val films = mutableListOf<URI>()
            queryCharacter(id).subscribe {
                films.addAll(it.films.toTypedArray())
            }
            val returnedFilms = mutableListOf<Film>()
            films.forEach {
                val webClient = WebClient.builder()
                webClient.baseUrl("https://swapi.co/api/").build()
                        .get().uri(it)
                        .header("Content-Type", APPLICATION_JSON_VALUE)
                        .accept(APPLICATION_JSON).retrieve()
                        .bodyToMono(Film::class.java)
                        .subscribe {
                           returnedFilms.add(it)
                        }
            }
            Flux.just(*returnedFilms.toTypedArray())
        }
        return films
    }

    private fun queryCharacter(id: Int): Mono<Character> {
        val webClient = WebClient.builder()
        return webClient.baseUrl("https://swapi.co/api/").build()
                .get().uri("/people/$id/")
                .header("Content-Type", APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON).retrieve()
                .bodyToMono(Character::class.java)
    }

}