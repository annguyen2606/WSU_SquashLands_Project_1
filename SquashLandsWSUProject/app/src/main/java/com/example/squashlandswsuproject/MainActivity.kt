package com.example.squashlandswsuproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Looper.getMainLooper
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
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

        socket.once("connect"){
            loadReferences()
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
                queueSetting.clear()
            }
            val tmp = it[0] as JSONArray
            JsonReader(StringReader(tmp[0].toString())).use {
                it.beginArray {
                    while (it.hasNext()){
                        val song = Klaxon().parse<Song>(it) as Song
                        if(!song.uri.contains("/Video%20Announcements/")){
                            queue.add(song)
                        }
                    }
                }
            }

            queue.sortBy { it.id }

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
            val tmp = it[0].toString()
            if (connectStatus){
                try {
                    val currentSongTmp = queue.find { song -> song.uri == tmp }
                    if(currentSongTmp != null){
                        if (queue.minBy { song -> song.id } != currentSongTmp && !currentSongTmp.uri.contains("/Video%20Announcements/")) {
                            currentSong = currentSongTmp.name
                            val oldSong = queue.minBy { song -> song.id }
                            queueRemoveSong(oldSong!!)

                            settingQueueRemoveSong(currentSongTmp)
                        }
                    }
                }catch (e: Exception){
                    throw e
                }
            }
        }

        socket.on("web remove song from queue") {
            val uriTmp = it[0].toString()
            val removedSong = queue.find { it.uri == uriTmp }
            if(removedSong != null){
                queueRemoveSong(removedSong)
                settingQueueRemoveSong(removedSong)
            }
        }

        socket.on("sync for repopulated queue") { it ->

            val tmp = it[0] as JSONArray
            val queueTmp = arrayListOf<Song>()
            JsonReader(StringReader(tmp.toString())).use {reader ->
                reader.beginArray {
                    while (reader.hasNext()){
                        val song = Klaxon().parse<Song>(reader) as Song
                        if(!song.uri.contains("/Video%20Announcements/"))
                            queueTmp.add(song)
                    }
                }
            }

            queueRepopulate(queueTmp)

            val queueSettingTmp = arrayListOf<Song>()
            queueSettingTmp.addAll(queue)
            queueSettingTmp.removeAt(0)

            settingQueueRepopulate(queueSettingTmp)
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
                val adapterTmp = CustomRequestRecyclerViewAdapter(request,fragmentRequestSong?.context!!)
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
        }

        socket.on("web change queue size"){
            val tmp = it[0].toString()
            queueSize = tmp.toInt()
        }

        socket.on("removed song from queue"){
            val tmp = it[0].toString()
            queue.remove(queue.find { song->song.id == tmp })
        }
        val homeFragment = HomeFragment()
        fTransaction.replace(R.id.fragment_holder, homeFragment, "fragment_home")
        fTransaction.commit()
        buttonBack.setOnClickListener {

            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder,homeFragment).commit()
            buttonBack.visibility = Button.INVISIBLE
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
        if(fragmentSettingFragment != null && fragmentSettingFragment.isVisible){
            (fragmentSettingFragment as SettingFragment).resetLogoutHandler()
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
        queue.remove(song)
        val queueTmp = arrayListOf<Song>()
        queueTmp.addAll(queue)
        val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")

        if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
            val recyclerView = fragmentChooseASongFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
            val adapterTmp = (recyclerView?.adapter as CustomQueueRecyclerViewAdapter)
            this.runOnUiThread {
                try {
                    adapterTmp.removeItem(adapterTmp.songs.indexOf(adapterTmp.songs.find{it.uri == song.uri}))
                }catch (exception: java.lang.Exception) {
                    Toast.makeText(this@MainActivity, queue.size.toString(),Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun settingQueueRemoveSong(song: Song){
        val fragmentSettingFragment = supportFragmentManager.findFragmentByTag("fragment_settings")

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
        queue.clear()
        queue.addAll(songs)
        queue.sortBy { song-> song.id }
        val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
        if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
            val adapter = CustomQueueRecyclerViewAdapter(songs, fragmentChooseASongFragment?.context!!)
            val recyclerView = (fragmentChooseASongFragment as ChooseASongFragment).view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
            this.runOnUiThread {
                recyclerView?.adapter = adapter
            }
        }
    }

    private fun settingQueueRepopulate(songs: ArrayList<Song>){
        val fragmentSetting = supportFragmentManager.findFragmentByTag("fragment_settings")
        if(fragmentSetting != null && fragmentSetting.isVisible){
            val recyclerView = fragmentSetting.view?.findViewById<RecyclerView>(R.id.recyclerViewSetting)
            this.runOnUiThread {
                (recyclerView?.adapter as CustomSettingRecyclerViewAdapter).repopulateData(songs)
            }
        }else
            queueSetting.addAll(songs)
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

    companion object{
        val socket: Socket = IO.socket("http://192.168.0.3:5000/")
        var currentSong: String = ""
        var queue =  arrayListOf<Song>()
        var library = arrayListOf<Song>()
        var request = arrayListOf<Request>()
        var queueSetting = arrayListOf<Song>()
        var queueSize = 0
        var pinNumber = "None"
        var connectStatus = false
        var announcement = ""
        var idleInterval = 0
        var screenSaverAnimation = ""
        const val preferencesStr = "squashlandReferences"
        const val announcementStr = "announcement"
        const val idleIntervalStr = "idleInterval"
        const val screenSaverAnimationStr = "screenSaverAnimation"
    }

}
