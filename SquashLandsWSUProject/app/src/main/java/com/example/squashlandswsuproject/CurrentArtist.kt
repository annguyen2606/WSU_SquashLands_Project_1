package com.example.squashlandswsuproject

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

class CurrentArtist(var currentArtist: String):BaseObservable() {
    @Bindable
    fun getCurrentArtistStr():String{
        return currentArtist
    }

    fun setCurrentArtistStr(currentArtist: String){
        this.currentArtist = currentArtist
        notifyPropertyChanged(BR.currentArtistStr)
    }
}