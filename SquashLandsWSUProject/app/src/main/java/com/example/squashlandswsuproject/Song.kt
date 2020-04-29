package com.example.squashlandswsuproject

import com.beust.klaxon.Json

data class Song (
    @Json(name = "@ro")
    val ro: String,

    @Json(name = "@name")
    val name: String,

    @Json(name = "@id")
    val id: String,

    @Json(name = "@uri")
    val uri: String,

    @Json(name = "@duration")
    val duration: String
)