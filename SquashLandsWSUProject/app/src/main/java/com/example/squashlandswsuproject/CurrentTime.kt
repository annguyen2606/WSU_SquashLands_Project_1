package com.example.squashlandswsuproject

import androidx.databinding.BaseObservable
import androidx.databinding.Bindable

class CurrentTime(private var timeStr: String): BaseObservable() {
    @Bindable
    fun getTimeStr(): String{
        return timeStr
    }

    fun setTimeStr(time: String){
        timeStr = time
        notifyPropertyChanged(BR.time)
    }
}