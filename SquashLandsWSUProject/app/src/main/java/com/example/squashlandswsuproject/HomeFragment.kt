package com.example.squashlandswsuproject

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        var rotation = AnimationUtils.loadAnimation(view.context,R.anim.rotate)
        rotation.interpolator = LinearInterpolator()
        var logo = view.findViewById<ImageView>(R.id.imageView) //get imageView contains logo
        var alertDialogNotConnected: AlertDialog.Builder = AlertDialog.Builder(view.context)

        var buttonChooseASong = view.findViewById<Button>(R.id.buttonChooseASong)
        var buttonRequestASong = view.findViewById<Button>(R.id.buttonRequestNewSong)


        alertDialogNotConnected.setCancelable(true)
        alertDialogNotConnected.setTitle("The app is not connected")
        alertDialogNotConnected.setMessage("Try to connect")
        alertDialogNotConnected.setNeutralButton("Ok",
            DialogInterface.OnClickListener { dialogInterface, i ->
                MainActivity.socket.connect()
                dialogInterface.cancel()
            }
        )

        MainActivity.socket.once("respond to sync",{
            rotation.fillAfter = true
            logo.startAnimation(rotation)
            this.activity?.runOnUiThread({
                textViewPlayingSong.text = MainActivity.currentSong
            })
            MainActivity.socket.emit("request request list")
        })
        if(!MainActivity.socket.connected())
            alertDialogNotConnected.show()
        buttonRequestASong.setOnClickListener {
            if(MainActivity.socket.connected()){
                var ft = this.fragmentManager?.beginTransaction()
                ft?.remove(this)
                ft?.replace(R.id.fragment_holder, RequestSongFragment(), "fragment_request_song")
                ft?.commit()
            }else
                alertDialogNotConnected.show()
        }

        buttonChooseASong.setOnClickListener {
            if(MainActivity.socket.connected()){
                var ft = this.fragmentManager?.beginTransaction()
                ft?.remove(this)
                ft?.replace(R.id.fragment_holder, ChooseASongFragment(), "fragment_choose_a_song")
                ft?.commit()
            }else
                alertDialogNotConnected.show()
        }
    }
}