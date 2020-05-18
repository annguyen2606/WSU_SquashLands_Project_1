package com.example.squashlandswsuproject

import android.animation.AnimatorInflater
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Looper.getMainLooper
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import java.io.StringReader
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.test.assertEquals


class MainActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    var screenSaverHandler = Handler()
    private val runnableScreenSaverCaller = Runnable {
        val intent = Intent(this@MainActivity,ScreenSaverActivity::class.java)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        textViewAnnouncement.isSelected = true;
        loadReferences()
        socket.once("connect"){
            socket.emit("tablet modify mobile announcement text", announcement)
        }

        socket.on("connect") {
            socket.emit("sync library and queue", "request to get library and queue")
            socket.emit("request request list")
            socket.emit("get pin number")
        }

        socket.on("respond to sync with library and queue") { it ->
            if(library.size > 0)
                library.clear()
            if(queue.size > 0){
                queue.clear()
                queueActual.clear()
                queueSetting.clear()
            }
            request.clear()
            val tmp = it[0] as JSONArray
            JsonReader(StringReader(tmp[0].toString())).use {
                it.beginArray {
                    while (it.hasNext()){
                        val song = Klaxon().parse<Song>(it) as Song
                        queueActual.add(song)
                    }
                }
            }
            queueActual.sortBy { it.id }
            queue.addAll(queueActual)
            if(queueActual.find{it.uri.contains("/Video%20Announcements/")} != null){
                queue.remove(queueActual.find { it.uri.contains("/Video%20Announcements/") }!!)
            }

            JsonReader(StringReader(tmp[1].toString())).use {
                it.beginArray {
                    while (it.hasNext()){
                        val song = Klaxon().parse<Song>(it) as Song
                        library.add(song)
                    }
                }
            }

            currentSong = queue.minBy { it.id }?.name!!
            val queueTmp = arrayListOf<Song>()
            queueTmp.addAll(queue)
            queueTmp.removeAt(0)
            settingQueueRepopulate(queueTmp)
            queueSize = (tmp[2].toString()).toInt()
            connectStatus = true
        }

        val fTransaction = supportFragmentManager.beginTransaction()
        socket.on("respond to sync") {
            val tmpJsonArray = it[0] as JSONArray
            val tmp = tmpJsonArray[0].toString()
            if (connectStatus){
                try {
                    val currentSongTmp = queueActual.find { song -> song.uri == tmp }
                    if(currentSongTmp != null){
                        if (queueActual.minBy { song -> song.id }?.uri != currentSongTmp.uri) {
                            if(!currentSongTmp.uri.contains("/Video%20Announcements/") &&  queue.find { it.uri == currentSongTmp.uri } != null) {
                                val oldSong = queue.minBy { song -> song.id }
                                if (oldSong != null && oldSong.uri != currentSongTmp.uri) {
                                    currentSong = currentSongTmp.name
                                    queueRemoveSong(oldSong)
                                    settingQueueRemoveSong(currentSongTmp)
                                }
                                settingLoadCurrentSong(currentSongTmp)
                            }
                            queueActual.remove(queueActual.minBy { song -> song.id })
                        }
                    }
                }catch (e: Exception){
                    throw e
                }
            }
        }

        socket.on("web remove song from queue") {
            val uriTmp = it[0].toString()
            val removedSong = queueActual.find { it.uri == uriTmp }
            if(removedSong != null){
                if(!removedSong.uri.contains("/Video%20Announcements/")){
                    queueRemoveSong(removedSong)
                    settingQueueRemoveSong(removedSong)
                }
                queueActual.remove(removedSong)
            }
        }

        socket.on("sync for repopulated queue") { it ->
            queueActual.clear()
            val tmp = it[0] as JSONArray
            val queueTmp = arrayListOf<Song>()
            JsonReader(StringReader(tmp.toString())).use {reader ->
                reader.beginArray {
                    while (reader.hasNext()){
                        val song = Klaxon().parse<Song>(reader) as Song
                        queueTmp.add(song)
                    }
                }
            }
            queueActual.addAll(queueTmp)
            queueActual.sortBy { it.id }
            if(queueActual.find { it.uri.contains("/Video%20Announcements/") } != null){
                queueTmp.removeAll(queueActual.filter { it.uri.contains("/Video%20Announcements/") })
            }
            queueRepopulate(queueTmp)
            val queueSettingTmp = arrayListOf<Song>()
            queueSettingTmp.addAll(queue)
            if(queueSettingTmp.size > 1){
                queueSettingTmp.removeAt(0)
                settingQueueRepopulate(queueSettingTmp)
            }
        }

        socket.on("respond request request list") {
            JsonReader(StringReader(it[0].toString())).use {reader ->
                reader.beginArray {
                    while(reader.hasNext()){
                        val requestTmp = Klaxon().parse<Request>(reader) as Request
                        if (!request.contains(requestTmp))
                            request.add(requestTmp)
                    }
                }
            }
        }

        socket.on("respond add request from tablet") {
            JsonReader(StringReader(it[0].toString())).use {reader ->
                reader.beginArray {
                    while(reader.hasNext()){
                        val requestTmp = Klaxon().parse<Request>(reader) as Request
                        if (!request.contains(requestTmp))
                            request.add(requestTmp)
                    }
                }
            }
            val fragmentRequestSong = supportFragmentManager.findFragmentByTag("fragment_request_song")
            if(fragmentRequestSong!= null && fragmentRequestSong.isVisible){
                val adapterTmp = CustomRequestRecyclerViewAdapter(request,fragmentRequestSong.context!!)
                val recyclerView = fragmentRequestSong.view?.findViewById<RecyclerView>(R.id.recyclerViewRequest)
                this.runOnUiThread {
                    recyclerView?.adapter = adapterTmp
                    Toast.makeText(this, "request sent", Toast.LENGTH_LONG).show()
                }
            }
        }

        socket.on("sync pin number"){
            val pinNumberTmp = it[0].toString()
            pinNumber = pinNumberTmp
        }

        socket.on("disconnect") {
            connectStatus = false
            runOnUiThread {
                currentSong = ""
                val fTransaction = supportFragmentManager.beginTransaction()
                fTransaction.replace(R.id.fragment_holder, HomeFragment(),"fragment_home")
                fTransaction.commit()
                buttonBack.visibility = Button.VISIBLE
            }
        }

        socket.on("web change queue size"){
            val tmp = it[0].toString()
            queueSize = tmp.toInt()
        }

        socket.on("removed song from queue"){
            val tmp = it[0].toString()
            queue.find { song->song.id == tmp }?.let { it1 -> settingQueueRemoveSong(it1) }
            queue.remove(queue.find { song->song.id == tmp })
        }
        val homeFragment = HomeFragment()
        fTransaction.replace(R.id.fragment_holder, homeFragment, "fragment_home")
        fTransaction.commit()
        buttonBack.setOnClickListener {
            var settingFragment = supportFragmentManager.findFragmentByTag("fragment_settings")
            if(settingFragment != null && settingFragment.isVisible){
                settingFragment = settingFragment as SettingFragment
                val announcement = settingFragment.editTextAnnouncement.text.toString()
                val idleInterval = settingFragment.editTextIdleInterval.text.toString().toInt()
                var screenSaverAnim = ""
                if(settingFragment.radioButtonRotation.isChecked)
                    screenSaverAnim = settingFragment.radioButtonRotation.text.toString()
                else if(settingFragment.radioButtonPumping.isChecked)
                    screenSaverAnim = settingFragment.radioButtonPumping.text.toString()

                if (announcement != MainActivity.announcement || idleInterval != MainActivity.idleInterval || screenSaverAnim != MainActivity.screenSaverAnimation){
                    val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
                    alertDialog.setCancelable(true)
                    alertDialog.setTitle("Confirmation")
                    alertDialog.setMessage("Apply new settings ?")
                    alertDialog.setNegativeButton("No",
                        DialogInterface.OnClickListener { dialogInterface, _ ->
                            dialogInterface.dismiss()
                            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder,homeFragment).commit()
                            buttonBack.visibility = Button.INVISIBLE
                        }
                    )
                    alertDialog.setPositiveButton("Yes",
                        DialogInterface.OnClickListener { _, _ ->
                            settingFragment.saveSettings()
                            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder,homeFragment).commit()
                            buttonBack.visibility = Button.INVISIBLE
                        }
                    )

                    alertDialog.show()
                }
            }else{
                supportFragmentManager.beginTransaction().replace(R.id.fragment_holder,homeFragment).commit()
                buttonBack.visibility = Button.INVISIBLE
            }
        }

        socket.on("web modify mobile announcement text"){
            val strNewAnnouncement = it[0].toString()
            val editor = getSharedPreferences(preferencesStr, Context.MODE_PRIVATE).edit()
            editor.putString(announcementStr,strNewAnnouncement)
            editor.apply()
            val settingFragment = supportFragmentManager.findFragmentByTag("fragment_settings")
            if(settingFragment != null && settingFragment.isVisible){
                val editTextAnnouncement = settingFragment.view?.findViewById<EditText>(R.id.editTextSettingAnnouncement)
                runOnUiThread {
                    editTextAnnouncement?.setText(strNewAnnouncement)
                }
            }
            announcement = strNewAnnouncement
            runOnUiThread {
                textViewAnnouncement.text = announcement
            }
        }

        socket.on("sync status"){
            val tmp = it[0].toString()
            var booleanTmp = tmp == "playing"

            if(booleanTmp != playingStatus){
                playingStatus = booleanTmp
                val fragmentSettingFragment = supportFragmentManager.findFragmentByTag("fragment_settings")
                if(fragmentSettingFragment != null && fragmentSettingFragment.isVisible){
                    val buttonSettingPlayStop = fragmentSettingFragment.view?.findViewById<ImageButton>(R.id.imageButtonSettingPlayStop)
                    val textViewPlayingStatus = fragmentSettingFragment.view?.findViewById<TextView>(R.id.textViewSettingPlayingStatus)
                    val imageSettingLogo = fragmentSettingFragment.view?.findViewById<ImageView>(R.id.imageViewSettingLogo)
                    val rotation = AnimationUtils.loadAnimation(fragmentSettingFragment.context,R.anim.rotate)
                    if (playingStatus) {
                        rotation.interpolator = LinearInterpolator()
                        rotation.fillAfter = true
                        runOnUiThread {
                            buttonSettingPlayStop?.setImageResource(android.R.drawable.ic_media_pause)
                            textViewPlayingStatus?.text = "Playing"
                            textViewPlayingStatus?.setTextColor(Color.parseColor("#608E0B"))
                            imageSettingLogo?.animation = rotation
                            rotation.start()
                        }
                    }else
                        runOnUiThread {
                            buttonSettingPlayStop?.setImageResource(android.R.drawable.ic_media_play)
                            textViewPlayingStatus?.text = "Paused"
                            textViewPlayingStatus?.setTextColor(Color.parseColor("#FF7F11"))
                            imageSettingLogo?.clearAnimation()
                        }
                }
            }
        }
    }

    private fun startScreenSaverHandler(){
        if(idleInterval == 0 )
            screenSaverHandler.postDelayed(runnableScreenSaverCaller, (2*60*1000).toLong())
        else
            screenSaverHandler.postDelayed(runnableScreenSaverCaller, (idleInterval*60*1000).toLong())
    }

    private fun stopScreenSaverHandler(){
        screenSaverHandler.removeCallbacks(runnableScreenSaverCaller)
    }

    private fun resetScreenSaverHandler(){
        stopScreenSaverHandler()
        startScreenSaverHandler()
    }

    override fun onResume() {
        super.onResume()
        startScreenSaverHandler()
    }

    override fun onPause() {
        super.onPause()
        stopScreenSaverHandler()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetScreenSaverHandler()
        val fragmentSettingFragment = supportFragmentManager.findFragmentByTag("fragment_settings")
        val fragmentStats = supportFragmentManager.findFragmentByTag("fragment_statistic")

        if(fragmentSettingFragment != null && fragmentSettingFragment.isVisible){
            (fragmentSettingFragment as SettingFragment).resetLogoutHandler()
        }

        if(fragmentStats != null && fragmentStats.isVisible){
            (fragmentStats as StatisticFragment).resetLogoutHandler()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
            R.id.action_setting ->{
                val fragmentStaffLoginFragment = supportFragmentManager.findFragmentByTag("fragment_staff_login")
                val fragmentSettingFragment = supportFragmentManager.findFragmentByTag("fragment_settings")
                if (fragmentStaffLoginFragment == null && fragmentSettingFragment == null){
                    val ft = supportFragmentManager.beginTransaction()
                    ft.replace(R.id.fragment_holder, StaffLoginFragment(), "fragment_staff_login")
                    ft.commit()
                }
            }
        }
        return true
    }

    override fun onBackPressed() {
        //TODO
    }

    private fun queueRemoveSong(song: Song){
        val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
        if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
            val recyclerView = fragmentChooseASongFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
            val adapterTmp = (recyclerView?.adapter as CustomQueueRecyclerViewAdapter)
            this.runOnUiThread {
                try {
                    adapterTmp.removeItem(adapterTmp.songs.indexOf(adapterTmp.songs.find{it.uri == song.uri}))
                }catch (exception: java.lang.Exception) {
                    queue.remove(song)
                    adapterTmp.repopulateData(queue)
                }
            }
        }else
            queue.remove(song)
    }

    private fun settingQueueRemoveSong(song: Song){
        var fragmentSettingFragment = supportFragmentManager.findFragmentByTag("fragment_settings")
        if(fragmentSettingFragment != null && fragmentSettingFragment.isVisible) {
            val recyclerView = fragmentSettingFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewSetting)
            val adapter = recyclerView?.adapter as CustomSettingRecyclerViewAdapter
            runOnUiThread{
                adapter.removeItem(queueSetting.indexOf(song))
            }
        }else
            queueSetting.remove(song)
    }

    private fun queueRepopulate(songs: ArrayList<Song>){
        val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
        if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
            val adapter = (fragmentChooseASongFragment as ChooseASongFragment).view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)?.adapter as CustomQueueRecyclerViewAdapter
            this.runOnUiThread {
                adapter.repopulateData(songs)
            }
        }else{
            queue.clear()
            queue.addAll(songs)
            queue.sortBy { song-> song.id }
        }

    }

    private fun settingQueueRepopulate(songs: ArrayList<Song>){
        var fragmentSetting = supportFragmentManager.findFragmentByTag("fragment_settings")
        if(fragmentSetting != null && fragmentSetting.isVisible){
            fragmentSetting = fragmentSetting as SettingFragment
            val recyclerView = fragmentSetting.view?.findViewById<RecyclerView>(R.id.recyclerViewSetting)
            this.runOnUiThread {
                fragmentSetting.readyStatus = false

                fragmentSetting.readyStatus = (recyclerView?.adapter as CustomSettingRecyclerViewAdapter).repopulateData(songs)
                Toast.makeText(this@MainActivity, "Playlist ready", Toast.LENGTH_SHORT).show()
            }
        }else{
            queueSetting.clear()
            queueSetting.addAll(songs)
            queueSetting.sortBy { song -> song.id }
        }
    }

    private fun loadReferences(){
        val sharedReferences = getSharedPreferences(preferencesStr, Context.MODE_PRIVATE)
        announcement = sharedReferences.getString(announcementStr, resources.getString(R.string.default_announcement))!!
        idleInterval = sharedReferences.getInt(idleIntervalStr, 2)
        screenSaverAnimation = sharedReferences.getString(screenSaverAnimationStr, "Rotation")!!
        runOnUiThread{
            textViewAnnouncement.text = announcement
        }
    }

    private fun settingLoadCurrentSong(song: Song){
        val fragmentSettingFragment = supportFragmentManager.findFragmentByTag("fragment_settings")
        if(fragmentSettingFragment != null && fragmentSettingFragment.isVisible){
            val textViewSettingSongName = fragmentSettingFragment.view?.findViewById<TextView>(R.id.textViewSettingPlayingSong)
            runOnUiThread { textViewSettingSongName?.text = song.name}
            val textViewSettingTime = fragmentSettingFragment.view?.findViewById<TextView>(R.id.textViewSettingPlayingTime)
            val minute = song.duration.toInt() / 60
            val second = song.duration.toInt() % 60

            if (second == 0)
                runOnUiThread {  textViewSettingTime?.text = minute.toString() + ":00"}
            else
                runOnUiThread {  textViewSettingTime?.text = minute.toString() + ":" + second.toString()}
        }
    }

    companion object{
        val socket: Socket = IO.socket("http://192.168.0.3:5000/")
        var currentSong: String = ""
        var queue =  arrayListOf<Song>()
        var library = arrayListOf<Song>()
        var request = arrayListOf<Request>()
        var queueSetting = arrayListOf<Song>()
        var queueActual = arrayListOf<Song>()
        var queueSize = 0
        var pinNumber = "None"
        var connectStatus = false
        var announcement = ""
        var idleInterval = 0
        var screenSaverAnimation = ""
        var playingStatus = false
        const val preferencesStr = "squashlandReferences"
        const val announcementStr = "announcement"
        const val idleIntervalStr = "idleInterval"
        const val screenSaverAnimationStr = "screenSaverAnimation"
    }

}
