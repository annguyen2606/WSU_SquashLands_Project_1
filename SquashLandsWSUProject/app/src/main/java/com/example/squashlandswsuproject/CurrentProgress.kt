package com.example.squashlandswsuproject

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
//Observable class from current time counted in second
class CurrentProgress(var currentSec: Int, var maxSec: Int):BaseObservable() {
    @Bindable
    fun getCurrentSecond(): Int{
        return currentSec
    }

    fun setCurrentSecond(currentSec: Int){
        this.currentSec = currentSec
        notifyPropertyChanged(BR.currentProgress)
    }

    @Bindable
    fun getMaxSecond():Int{
        return maxSec
        notifyPropertyChanged(BR.currentSecond)
    }

    fun setMaxSecond(maxSec: Int){
        this.maxSec = maxSec
    }
}