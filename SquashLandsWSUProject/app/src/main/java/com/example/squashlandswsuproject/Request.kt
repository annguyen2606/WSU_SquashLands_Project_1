package com.example.squashlandswsuproject

import com.beust.klaxon.Json

data class Request(
    @Json(name = "patronName")
    val patronName: String,

    @Json(name = "songName")
    val songName: String,

    @Json(name = "email")
    val email: String
)