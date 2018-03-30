package com.codependent.cryptomarket.ui.web

import com.codependent.cryptomarket.ui.dto.Character
import com.codependent.cryptomarket.ui.dto.Film
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.net.URI

@RestController
class StarWarsApiController {

    @GetMapping("/api/people/{id}")
    fun getCharacter(@PathVariable id: Int): Mono<Character> {
        return findCharacterWebClient(id)
    }

    @GetMapping("/api/people/{id}/films")
    fun getCharacterFilms(@PathVariable id: Int, @RequestParam mode: String?): Flux<Film> {
        return when (mode) {
            "serial" -> {
                val returnedFilms = mutableListOf<Film>()
                val character = findCharacterWebClient(id).subscribeOn(Schedulers.elastic()).block()
                character?.films?.forEach { returnedFilms.add(findFilmRestTemplate(it)) }
                Flux.just(*returnedFilms.toTypedArray())
            }
            "parallel-rest-template" ->
                findCharacterWebClient(id)
                    .flatMapMany { Flux.just(*it.films.toTypedArray()) }
                    .flatMap(this::findFilmDeferredRestTemplate)
            else ->
                findCharacterWebClient(id)
                    .flatMapMany { Flux.just(*it.films.toTypedArray()) }
                    .flatMap(this::findFilmWebClient)
        }
    }

    private fun findCharacterWebClient(id: Int): Mono<Character> {
        val webClient = WebClient.builder()
        return webClient.baseUrl("https://swapi.co/api/").build()
                .get().uri("/people/$id/")
                .header("Content-Type", APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON).retrieve()
                .bodyToMono(Character::class.java).log()
    }

    private fun findFilmWebClient(uri: URI): Mono<Film> {
        val webClient = WebClient.builder()
        return webClient.baseUrl("https://swapi.co/api/").build()
                .get().uri(uri)
                .header("Content-Type", APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON).retrieve()
                .bodyToMono(Film::class.java).log()
    }

    private fun findFilmRestTemplate(uri: URI): Film {
        val restTemplate = RestTemplate()
        val headers = HttpHeaders()
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE)
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        headers.add("User-Agent", "curl/7.37.0")
        val httpEntity = HttpEntity(null, headers)
        val exchange = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, Film::class.java)
        return exchange.body as Film
    }

    private fun findFilmDeferredRestTemplate(uri: URI): Mono<Film> {
        return Mono.defer { findFilmRestTemplate(uri).toMono() }.log().subscribeOn(Schedulers.elastic())
    }

}