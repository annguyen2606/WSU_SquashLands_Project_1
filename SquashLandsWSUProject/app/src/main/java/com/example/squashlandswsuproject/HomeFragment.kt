package com.example.squashlandswsuproject

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.graphics.Interpolator
import android.net.InetAddresses
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.client.SocketIOException
import kotlinx.android.synthetic.main.activity_screen_saver.*
import kotlinx.android.synthetic.main.fragment_home.*
import java.net.Inet4Address
import java.util.regex.Pattern
import java.util.zip.Inflater
import kotlin.concurrent.timer

class HomeFragment : Fragment(R.layout.fragment_home) {
    private var currentSongTmp = ""
    private lateinit var textViewPlayingSong: TextView
    lateinit var logo: ImageView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return super.onCreateView(inflater, container, savedInstanceState)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rotation = AnimationUtils.loadAnimation(view.context,R.anim.rotate)

        rotation.interpolator = LinearInterpolator()
        logo = view.findViewById<ImageView>(R.id.imageView) //get imageView contains logo

        val buttonChooseASong = view.findViewById<Button>(R.id.buttonChooseASong)
        val buttonRequestASong = view.findViewById<Button>(R.id.buttonRequestNewSong)

        textViewPlayingSong = view.findViewById<TextView>(R.id.textViewPlayingSong)

        val alertDialogNotConnected: AlertDialog.Builder = AlertDialog.Builder(view.context)
        alertDialogNotConnected.setCancelable(true)
        alertDialogNotConnected.setTitle("The app is not connected")
        alertDialogNotConnected.setMessage("Try to connect")
        alertDialogNotConnected.setNeutralButton("Ok",
            DialogInterface.OnClickListener { dialogInterface, _ ->
                MainActivity.socket.connect()
                dialogInterface.cancel()
            }
        )
        //declare text box appears if socket is not connected

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
        try {
            if(!MainActivity.socket.connected()) {
                if(MainActivity.iP == ""){
                    val alertDialogNotConnected: AlertDialog.Builder = AlertDialog.Builder(view.context)
                    var dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_ip_config,null)
                    val dialogInterface = alertDialogNotConnected.setView(dialogView).show()
                    val editTextIP = dialogView.findViewById<EditText>(R.id.editTextIP)
                    val buttonSubmitIp = dialogView.findViewById<Button>(R.id.buttonSubmitIP)
                    val textViewDialog = dialogView.findViewById<TextView>(R.id.textViewDialog)
                    buttonSubmitIp.setOnClickListener {view ->
                        var iPTmp = editTextIP.text.toString()
                        val validRes = Patterns.IP_ADDRESS.matcher(iPTmp).matches()
                        if(validRes){
                            var urlTmp = "http://$iPTmp:5000/"
                            val options = IO.Options()
                            options.timeout = 10000.toLong()
                            var socketTmp = IO.socket(urlTmp, options)
                            socketTmp.connect()
                            textViewDialog.text = "Connecting to server, please wait..."
                            socketTmp.once(Socket.EVENT_CONNECT_TIMEOUT){
                                activity?.runOnUiThread {
                                    textViewDialog.text = "Connection time out"
                                    view.clearFocus()
                                }
                            }

                            socketTmp.once("connected"){
                                MainActivity.iP = iPTmp
                                dialogInterface.dismiss()
                                MainActivity.socket = socketTmp
                                val sharedReferencesEditor  = activity?.getSharedPreferences(MainActivity.preferencesStr, Context.MODE_PRIVATE)!!.edit()
                                sharedReferencesEditor.putString(MainActivity.iPStr, MainActivity.iP)
                                sharedReferencesEditor.commit()
                                activity?.runOnUiThread {
                                    activity?.finish()
                                    startActivity(activity?.intent)
                                }
                            }
                        }else{
                            textViewDialog.text = "IP is not valid"
                        }

                    }
                }
            }
        }catch (exeption: SocketIOException){
            throw exeption
        }

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


    override fun onResume() {
        textViewPlayingSong.text = currentSongTmp
        super.onResume()
    }
}