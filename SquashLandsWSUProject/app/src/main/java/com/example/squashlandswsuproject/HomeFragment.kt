package com.example.squashlandswsuproject

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Interpolator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private var currentSongTmp = ""
    private lateinit var textViewPlayingSong: TextView
    lateinit var logo: ImageView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rotation = AnimationUtils.loadAnimation(view.context,R.anim.rotate)

        rotation.interpolator = LinearInterpolator()
        logo = view.findViewById<ImageView>(R.id.imageView) //get imageView contains logo
        val alertDialogNotConnected: AlertDialog.Builder = AlertDialog.Builder(view.context)

        val buttonChooseASong = view.findViewById<Button>(R.id.buttonChooseASong)
        val buttonRequestASong = view.findViewById<Button>(R.id.buttonRequestNewSong)

        textViewPlayingSong = view.findViewById<TextView>(R.id.textViewPlayingSong)
        alertDialogNotConnected.setCancelable(true)
        alertDialogNotConnected.setTitle("The app is not connected")
        alertDialogNotConnected.setMessage("Try to connect")
        alertDialogNotConnected.setNeutralButton("Ok",
            DialogInterface.OnClickListener { dialogInterface, _ ->
                MainActivity.socket.connect()
                dialogInterface.cancel()
            }
        )

        MainActivity.socket.once("respond to sync") {
            rotation.fillAfter = true
            logo.startAnimation(rotation)
        }

        MainActivity.socket.on("respond to sync"){
            if(MainActivity.currentSong != currentSongTmp){
                currentSongTmp = MainActivity.currentSong
                activity?.runOnUiThread {
                       textViewPlayingSong.text = currentSongTmp
                }
            }
        }

        if(!MainActivity.socket.connected())
            alertDialogNotConnected.show()
        buttonRequestASong.setOnClickListener {
            if(MainActivity.connectStatus){
                val supportFragmentManagerTmp = this.fragmentManager
                val ft = this.fragmentManager?.beginTransaction()
                supportFragmentManagerTmp?.popBackStack("request_song",0)!!
                val tag = "fragment_request_song"
                if(supportFragmentManagerTmp.findFragmentByTag(tag) == null){
                    ft?.replace(R.id.fragment_holder, RequestSongFragment(), tag)
                    ft?.commit()
                }
            }else
                alertDialogNotConnected.show()
        }

        buttonChooseASong.setOnClickListener {
            if(MainActivity.connectStatus){
                val supportFragmentManagerTmp = this.fragmentManager
                supportFragmentManagerTmp?.popBackStack("choose_a_song",0)!!
                val ft = this.fragmentManager?.beginTransaction()
                val tag = "fragment_choose_a_song"
                if(supportFragmentManagerTmp.findFragmentByTag(tag) == null){
                    ft?.replace(R.id.fragment_holder, ChooseASongFragment(), tag)
                    ft?.commit()
                }
            }else
                alertDialogNotConnected.show()
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {

        super.onViewStateRestored(savedInstanceState)
    }

    override fun onResume() {
        textViewPlayingSong.text = currentSongTmp
        super.onResume()
    }
}