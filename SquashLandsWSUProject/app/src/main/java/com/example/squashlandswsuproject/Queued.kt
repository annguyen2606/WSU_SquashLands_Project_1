package com.example.squashlandswsuproject

import com.beust.klaxon.Json

data class Queued(
    @Json(name = "songName")
    val songName: String,

    @Json(name = "queuer")
    val queuer: String,

    @Json(name = "timeStamp")
    var timeStamp: String
)