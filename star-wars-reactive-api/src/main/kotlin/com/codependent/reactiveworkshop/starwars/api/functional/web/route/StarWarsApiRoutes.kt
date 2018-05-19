package com.codependent.reactiveworkshop.starwars.api.functional.web.route

import com.codependent.reactiveworkshop.starwars.api.functional.web.handler.StarWarsApiHandlers
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.router

@Configuration
class StarWarsApiRoutes(private val starWarsApiHandlers: StarWarsApiHandlers) {

    @Bean
    fun starWarsApiRouter() = router {
        "/functionalapi/people/{id}".nest {
            GET("", starWarsApiHandlers::getCharacter)
            GET("/films", starWarsApiHandlers::getCharacterFilms)
        }
    }

}