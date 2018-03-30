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
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

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
                .flatMap{
                    val webClient = WebClient.builder()
                    webClient.baseUrl("https://swapi.co/api/").build()
                            .get().uri(it)
                            .header("Content-Type", APPLICATION_JSON_VALUE)
                            .accept(APPLICATION_JSON).retrieve()
                            .bodyToMono(Film::class.java)

                }
    }

    @GetMapping("/api/people/{id}/filmsSerial")
    fun getCharacterFilmsSerial(@PathVariable id: Int): Flux<Film> {
        return Flux.defer {
            val returnedFilms = mutableListOf<Film>()
            val character = queryCharacter(id).subscribeOn(Schedulers.elastic()).block()
            character?.films?.forEach {
                val restTemplate = RestTemplate()
                val headers = HttpHeaders();
                headers.add("Accept", MediaType.APPLICATION_JSON_VALUE)
                headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                headers.add("User-Agent", "curl/7.37.0")
                val httpEntity = HttpEntity(null, headers)
                val exchange = restTemplate.exchange(it, HttpMethod.GET, httpEntity, Film::class.java)
                returnedFilms.add(exchange.body as Film)
            }
            Flux.just(*returnedFilms.toTypedArray())
        }
    }

    private fun queryCharacter(id: Int): Mono<Character> {
        val webClient = WebClient.builder()
        return webClient.baseUrl("https://swapi.co/api/").build()
                .get().uri("/people/$id/")
                .header("Content-Type", APPLICATION_JSON_VALUE)
                .accept(APPLICATION_JSON).retrieve()
                .bodyToMono(Character::class.java).log()
    }

}