package com.example.squashlandswsuproject

import android.view.animation.Interpolator

internal class MyBounceInterpolator(
    amplitude: Double,
    frequency: Double
) :
    Interpolator {
    private var mAmplitude = amplitude
    private var mFrequency = frequency
    override fun getInterpolation(time: Float): Float {
        return (-1 * Math.pow(Math.E, -time / mAmplitude) *
                Math.cos(mFrequency * time) + 1).toFloat()
    }
}