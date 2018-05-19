package com.codependent.reactiveworkshop.starwars.api.annotated.web

import com.codependent.reactiveworkshop.starwars.client.StarWarsApiRestTemplate
import com.codependent.reactiveworkshop.starwars.client.StarWarsApiWebClient
import com.codependent.reactiveworkshop.starwars.dto.Character
import com.codependent.reactiveworkshop.starwars.dto.Film
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.scheduler.Schedulers

@RestController
class StarWarsApiController(private val starWarsApiWebClient: StarWarsApiWebClient,
                            private val starWarsApiRestTemplate: StarWarsApiRestTemplate) {

    @GetMapping("/api/people/{id}")
    fun getCharacter(@PathVariable id: Int): Mono<Character> {
        return starWarsApiWebClient.findCharacter(id)
    }

    @GetMapping("/api/people/{id}/films")
    fun getCharacterFilms(@PathVariable id: Int, @RequestParam mode: String?): Flux<Film> {
        return when (mode) {
            "serial" -> {
                val returnedFilms = mutableListOf<Film>()
                val character = starWarsApiWebClient.findCharacter(id).subscribeOn(Schedulers.elastic()).block()
                character?.films?.forEach { returnedFilms.add(starWarsApiRestTemplate.findFilm(it)) }
                returnedFilms.toFlux()
            }
            "parallel-rest-template" ->
                starWarsApiWebClient.findCharacter(id)
                        .flatMapMany { it.films.toFlux() }
                        .flatMap(starWarsApiRestTemplate::findFilmDeferred)
            else ->
                starWarsApiWebClient.findCharacter(id)
                        .flatMapMany { it.films.toFlux() }
                        .flatMap(starWarsApiWebClient::findFilm)
        }
    }


}