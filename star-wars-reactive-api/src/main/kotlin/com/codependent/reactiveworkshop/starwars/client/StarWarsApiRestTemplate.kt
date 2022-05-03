package com.codependent.reactiveworkshop.starwars.client

import com.codependent.reactiveworkshop.starwars.dto.Character
import com.codependent.reactiveworkshop.starwars.dto.Film
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Schedulers
import java.net.URI

@Component
class StarWarsApiRestTemplate {

    private val restTemplate = RestTemplate()

    fun findCharacter(id: Int): Character {
        val headers = HttpHeaders()
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE)
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        headers.add("User-Agent", "curl/7.37.0")
        val httpEntity = HttpEntity(null, headers)
        val exchange = restTemplate.exchange("https://swapi.co/api/people/{id}", HttpMethod.GET, httpEntity, Character::class.java, id)
        return exchange.body as Character
    }

    fun findFilm(uri: URI): Film {
        val headers = HttpHeaders()
        headers.add("Accept", MediaType.APPLICATION_JSON_VALUE)
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        headers.add("User-Agent", "curl/7.37.0")
        val httpEntity = HttpEntity(null, headers)
        val exchange = restTemplate.exchange(uri, HttpMethod.GET, httpEntity, Film::class.java)
        return exchange.body as Film
    }

    fun findFilmDeferred(uri: URI): Mono<Film> {
        return Mono.fromCallable { findFilm(uri) }.log().subscribeOn(Schedulers.boundedElastic())
    }

}
