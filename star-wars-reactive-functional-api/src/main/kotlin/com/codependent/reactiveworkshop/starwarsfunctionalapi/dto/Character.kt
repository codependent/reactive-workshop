package com.codependent.reactiveworkshop.starwars.dto

import java.net.URI

data class Character(val name: String, val films: List<URI>)