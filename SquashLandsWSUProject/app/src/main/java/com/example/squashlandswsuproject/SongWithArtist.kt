package com.example.squashlandswsuproject

class SongWithArtist(val song: Song) {
    var artist = song.uri.split(Regex("/Music%20Videos/"))[1].split(Regex("-"))[0].replace("%20", " ")
}