package com.example.squashlandswsuproject

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

class CurrentStatus(private var status: Boolean):BaseObservable() {
    @Bindable
    fun getStatusStr(): String{
        if(status)
            return "Playing"
        else
            return "Paused"
    }


    @Bindable
    fun getStatus():Boolean{
        return status
    }

    fun setStatus(statusVar: Boolean){
        status = statusVar
        notifyPropertyChanged(BR.status)
    }
}