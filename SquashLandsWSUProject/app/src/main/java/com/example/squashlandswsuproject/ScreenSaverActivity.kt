package com.example.squashlandswsuproject

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import kotlinx.android.synthetic.main.activity_screen_saver.*

class ScreenSaverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_saver)
        if(MainActivity.screenSaverAnimation.contentEquals("Rotation")){
            val rotation = AnimationUtils.loadAnimation(this,R.anim.rotate_fading)

            rotation.interpolator = LinearInterpolator()
            rotation.fillAfter = true
            imageViewScreenSaverLogo.startAnimation(rotation)
        }else if(MainActivity.screenSaverAnimation.contentEquals("Pumping")){
            val pumping = AnimationUtils.loadAnimation(this,R.anim.pump)

            pumping.interpolator = MyBounceInterpolator(0.2,24.0)
            pumping.fillAfter = true
            imageViewScreenSaverLogo.startAnimation(pumping)
        }

        imageViewScreenSaverLogo.setOnClickListener {
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
        }

        textViewScreenSaverAnnouncement.text = MainActivity.announcement
        textViewScreenSaverAnnouncement.isSelected = true
        MainActivity.socket.on("web modify mobile announcement text"){
            val strAnnouncement = it[0].toString()
            runOnUiThread {
                textViewScreenSaverAnnouncement.text = strAnnouncement
            }
        }

        MainActivity.socket.on("broadcast modified announcement"){
            runOnUiThread {
                textViewScreenSaverAnnouncement.text = it[0].toString()
            }
        }
    }
}
