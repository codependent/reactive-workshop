package com.codependent.reactiveworkshop.starwars.api.functional.web.handler

import com.codependent.reactiveworkshop.starwars.client.StarWarsApiWebClient
import com.codependent.reactiveworkshop.starwars.dto.Character
import com.codependent.reactiveworkshop.starwars.dto.Film
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

@Component
class StarWarsApiHandlers(private val starWarsApiWebClient: StarWarsApiWebClient) {

    fun getCharacter(serverRequest: ServerRequest) =
            ok().body(starWarsApiWebClient.findCharacter(serverRequest.pathVariable("id").toInt()), Character::class.java)


    fun getCharacterFilms(serverRequest: ServerRequest): Mono<ServerResponse> {
        val films = starWarsApiWebClient.findCharacter(serverRequest.pathVariable("id").toInt())
                .flatMapMany { it.films.toFlux() }
                .flatMap(starWarsApiWebClient::findFilm)

        return ok().body(films, Film::class.java)
    }
}
