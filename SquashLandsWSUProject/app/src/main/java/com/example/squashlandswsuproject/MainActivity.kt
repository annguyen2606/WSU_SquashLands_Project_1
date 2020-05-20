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
import androidx.databinding.ObservableField
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import com.example.squashlandswsuproject.databinding.RowQueuePlayingBinding
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

        //remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        //select the announcement text to make it sliding (text has to be longer than text view size)
        textViewAnnouncement.isSelected = true

        //call loadReferences() function
        loadReferences()

        //sync announcement stored in shared references to server
        socket.once("connect"){
            socket.emit("tablet modify mobile announcement text", announcement)
        }

        //on socket connect get pin number, media library, and queue from server
        socket.on("connect") {
            socket.emit("sync library and queue", "request to get library and queue")
            socket.emit("request request list")
            socket.emit("get pin number")
        }

        //when server respond to sync library and queue request
        socket.on("respond to sync with library and queue") { it ->

            //clear all current static arrayListOf<Song> and arrayListOf<Request>
            if(library.size > 0)
                library.clear()
            if(queue.size > 0){
                queue.clear()
                queueActual.clear()
                queueSetting.clear()
            }
            request.clear()

            //get data from server which is always store at it[0] and cast it as JSONArray
            val tmp = it[0] as JSONArray

            //use klaxon to parse the json string as Song object
            JsonReader(StringReader(tmp[0].toString())).use {
                it.beginArray {
                    while (it.hasNext()){
                        val song = Klaxon().parse<Song>(it) as Song
                        queueActual.add(song)
                    }
                }
            }

            //sort the actual queue which may include announcement
            queueActual.sortBy { it.id }

            //add all item from actual queue to displayed queue
            queue.addAll(queueActual)

            //find if there is any item in actual queue which is announcement video
            if(queueActual.find{it.uri.contains("/Video%20Announcements/")} != null){
                queue.remove(queueActual.find { it.uri.contains("/Video%20Announcements/") }!!)
            }

            //parse JSON strings in JSONArray as Song and add it to library
            JsonReader(StringReader(tmp[1].toString())).use {
                it.beginArray {
                    while (it.hasNext()){
                        val song = Klaxon().parse<Song>(it) as Song
                        library.add(song)
                    }
                }
            }

            //current song is least id of the queue
            currentSong = queue.minBy { it.id }?.name!!

            //current max second is duration of the current song
            currentMaxSecond = queue.minBy { it.id }?.duration!!.toInt()

            //extract artist of current song by its uri string
            var uriTmp = queue.minBy { it.id }?.uri
            var uriArray = uriTmp?.split(Regex("/Music%20Videos/"))
            var fileName = uriArray?.get(1)?.split(Regex("-"))
            var artist = fileName?.get(0)?.replace("%20", " ")
            if(artist!= null)
                currentArtist = artist

            //performing re-populating of setting queue which does not contain the current song
            val queueTmp = arrayListOf<Song>()
            queueTmp.addAll(queue)
            queueTmp.removeAt(0)
            settingQueueRepopulate(queueTmp)

            //set queue size by value received from server
            queueSize = (tmp[2].toString()).toInt()

            //set connection status flag as true which indicate the app ready to process
            connectStatus = true
        }

        val fTransaction = supportFragmentManager.beginTransaction()

        //'respond to sync' event fire by background task of server
        socket.on("respond to sync") {
            val tmpJsonArray = it[0] as JSONArray
            val tmp = tmpJsonArray[0].toString()

            //check if app is ready to perform
            if (connectStatus){
                try {
                    val currentSongTmp = queueActual.find { song -> song.uri == tmp }
                    if(currentSongTmp != null){
                        //if new uri is different from current uri
                        if (queueActual.minBy { song -> song.id }?.uri != currentSongTmp.uri) {

                            //if that new uri is not from an announcement video
                            if(!currentSongTmp.uri.contains("/Video%20Announcements/") &&  queue.find { it.uri == currentSongTmp.uri } != null) {

                                val oldSong = queue.minBy { song -> song.id }

                                //the if clause to make sure no null pointer exception happens
                                if (oldSong != null && oldSong.uri != currentSongTmp.uri) {

                                    //set 'current' variables
                                    currentSong = currentSongTmp.name
                                    currentMaxSecond = currentSongTmp.duration.toInt()
                                    var uriTmp = currentSongTmp.uri
                                    var uriArray = uriTmp.split(Regex("/Music%20Videos/"))
                                    var fileName = uriArray.get(1).split(Regex("-"))
                                    var artist = fileName.get(0).replace("%20", " ")
                                    currentArtist = artist

                                    //remove old song from queue
                                    queueRemoveSong(oldSong)

                                    //remove current song from setting queue
                                    settingQueueRemoveSong(currentSongTmp)
                                }
                                settingLoadCurrentSong(currentSongTmp)
                            }
                            //remove song from actual queue
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

        //when server auto-generate new queue
        socket.on("sync for repopulated queue") { it ->
            queueActual.clear()
            try {
                val tmp = it[0] as JSONArray
                socket.emit("tablet sync for queue")

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
                queueSettingTmp.addAll(queueTmp)
                queueSettingTmp.removeAt(0)
                settingQueueRepopulate(queueSettingTmp)
            }catch (exception: ClassCastException){
                runOnUiThread { Toast.makeText(this, "Error 1", Toast.LENGTH_SHORT).show() }
            }
        }

        //server repond with request list
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

        //server respond with recent added request
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

        //server sync generated pin number to tablet
        socket.on("sync pin number"){
            val pinNumberTmp = it[0].toString()
            pinNumber = pinNumberTmp
        }


        socket.on("disconnect") {

            //set connection ready flag to false
            connectStatus = false

            //go to home fragment
            runOnUiThread {
                currentSong = ""
                val fTransaction = supportFragmentManager.beginTransaction()
                fTransaction.replace(R.id.fragment_holder, HomeFragment(),"fragment_home")
                fTransaction.commit()
                buttonBack.visibility = Button.INVISIBLE
            }
        }

        //server sync changed queue size
        socket.on("web change queue size"){
            val tmp = it[0].toString()
            queueSize = tmp.toInt()
        }

        //server respond to 'tablet remove song from queue'
        socket.on("removed song from queue"){
            val tmp = it[0].toString()
            val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
            if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
                val recyclerViewTmp = fragmentChooseASongFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
                val adater = recyclerViewTmp?.adapter as CustomQueueRecyclerViewAdapter
                runOnUiThread { adater.removeItem(adater.songs.indexOf(adater.songs.find { it.id == tmp })) }
            }else{
                queue.find { song->song.id == tmp }?.let { it1 -> settingQueueRemoveSong(it1) }
                queue.remove(queue.find { song->song.id == tmp })
            }
        }
        val homeFragment = HomeFragment()
        fTransaction.replace(R.id.fragment_holder, homeFragment, "fragment_home")
        fTransaction.commit()

        //set onclicklistener for back button
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
            }
            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder,homeFragment).commit()
            buttonBack.visibility = Button.INVISIBLE
        }

        //when editing announcement from tablet socket will fire this event
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

        //sync playing status and time from vlc
        socket.on("sync status"){
            val tmp = it[0] as JSONArray
            var booleanTmp = tmp[0].toString() == "playing"
            currentProgress = tmp[1].toString().toInt()

            val minuteTmp = tmp[1].toString().toInt() / 60
            val secondTmp = tmp[1].toString().toInt() % 60
            val fragmentSettingFragment = supportFragmentManager.findFragmentByTag("fragment_settings")

            if(secondTmp < 10)
                time = minuteTmp.toString() + ":0" + secondTmp.toString()
            else
                time = minuteTmp.toString() + ":" + secondTmp.toString()
            if(booleanTmp != playingStatus){

                //if playing status different from server playing status -> change it
                playingStatus = booleanTmp
                if(fragmentSettingFragment != null && fragmentSettingFragment.isVisible){
                    val buttonSettingPlayStop = fragmentSettingFragment.view?.findViewById<ImageButton>(R.id.imageButtonSettingPlayStop)
                    val textViewPlayingStatus = fragmentSettingFragment.view?.findViewById<TextView>(R.id.textViewSettingPlayingStatus)
                    val imageSettingLogo = fragmentSettingFragment.view?.findViewById<ImageView>(R.id.imageViewSettingLogo)
                    val rotation = AnimationUtils.loadAnimation(fragmentSettingFragment.context,R.anim.rotate)

                    //set status and animation of logo in setting screen
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
                    }else{
                        runOnUiThread {
                            buttonSettingPlayStop?.setImageResource(android.R.drawable.ic_media_play)
                            textViewPlayingStatus?.text = "Paused"
                            textViewPlayingStatus?.setTextColor(Color.parseColor("#FF7F11"))
                            imageSettingLogo?.clearAnimation()
                        }
                    }
                }
            }else{
                if(fragmentSettingFragment != null && fragmentSettingFragment.isVisible){

                    //change attribute of data variables in fragment_settings.xml
                    (fragmentSettingFragment as SettingFragment).currentTime.setTimeStr(time)
                    fragmentSettingFragment.currentArtist.setCurrentArtistStr(currentArtist)
                    fragmentSettingFragment.currentProgress.setCurrentSecond(currentProgress)
                    fragmentSettingFragment.binding.invalidateAll()
                }
            }
        }

        //when server change announcement
        socket.on("broadcast modified announcement"){
            val sharedReferencesEditor  = getSharedPreferences(preferencesStr, Context.MODE_PRIVATE)!!.edit()
            announcement = it[0].toString()
            sharedReferencesEditor.putString(announcementStr, it[0].toString())
            runOnUiThread {
                textViewAnnouncement.text = announcement
                Toast.makeText(this, it[0].toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startScreenSaverHandler(){

        //two minutes will be set as default in first run
        if(idleInterval == 0)
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

        //catch interaction from user and reset logout and screen saver handlers
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
                (recyclerView?.adapter as CustomSettingRecyclerViewAdapter).repopulateData(songs)
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

    //declare static variables
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
        var time = ""
        var currentMaxSecond = 0
        var currentProgress = 0
        var currentArtist = ""
        const val preferencesStr = "squashlandReferences"
        const val announcementStr = "announcement"
        const val idleIntervalStr = "idleInterval"
        const val screenSaverAnimationStr = "screenSaverAnimation"
    }

}
