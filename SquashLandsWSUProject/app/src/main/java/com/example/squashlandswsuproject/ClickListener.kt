package com.example.squashlandswsuproject

import android.view.View

interface ClickListener {
    fun onClick(view: View?, position: Int)
    fun onClickUp(view: View?, position: Int)
    fun onLongClick(view: View?, position: Int)
}