package com.example.squashlandswsuproject

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener


class CustomRecyclerViewOnItemTouchListener(context: Context?, recyclerView: RecyclerView,
                                            private val clickListener: ClickListener?
) : OnItemTouchListener {
    //GestureDetector to detect touch event.
    private val gestureDetector: GestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {

            return true
        }

        override fun onLongPress(e: MotionEvent) {
            val child = recyclerView.findChildViewUnder(e.x, e.y)
            if (child != null && clickListener != null) {
                clickListener.onLongClick(child, recyclerView.getChildLayoutPosition(child))
            }
        }
    })

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(e)
        val child = rv.findChildViewUnder(e.x, e.y)
        if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
            if(e.action == MotionEvent.ACTION_UP)
                clickListener.onClickUp(child, rv.getChildLayoutPosition(child))
            if(e.action == MotionEvent.ACTION_CANCEL)
                clickListener.onClickUp(child, rv.getChildLayoutPosition(child))
        }

        //On Touch event
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}

}