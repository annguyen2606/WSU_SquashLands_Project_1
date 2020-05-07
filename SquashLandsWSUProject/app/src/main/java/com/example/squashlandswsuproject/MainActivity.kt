package com.example.squashlandswsuproject

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import java.lang.Exception


class MainActivity : AppCompatActivity() {
    lateinit var toolbar: Toolbar
    val fragmentHome = HomeFragment()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        textViewAnnouncement.isSelected = true;

        socket.once("connect") {
            socket.emit("sync library and queue", "request to get library and queue")
            socket.emit("request request list")
            socket.emit("get pin number")
        }

        socket.on("respond to sync with library and queue") { it ->
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

            JsonReader(StringReader(tmp[1].toString())).use {
                it.beginArray {
                    while (it.hasNext()){
                        val song = Klaxon().parse<Song>(it) as Song
                        library.add(song)
                    }
                }
            }
            currentSong = queue.minBy { it.id }?.name!!
            queueSetting.addAll(queue)
            queueSetting.remove(queueSetting.minBy { song -> song.id })
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
                            queue.remove(queue.minBy { song -> song.id }!!)
                            queueSetting.remove(currentSongTmp)
                            val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
                            if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
                                val adapterTmp = CustomQueueRecyclerViewAdapter(queue,fragmentChooseASongFragment.context!!)
                                val recyclerView = fragmentChooseASongFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
                                this.runOnUiThread {
                                    recyclerView?.adapter = adapterTmp
                                }
                            }
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
                queue.remove(removedSong)
            }
            val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
            if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
                val adapterTmp = CustomQueueRecyclerViewAdapter(queue,fragmentChooseASongFragment.context!!)
                val recyclerView = fragmentChooseASongFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
                this.runOnUiThread {
                    recyclerView?.adapter = adapterTmp
                }
            }
        }

        socket.on("sync for repopulated queue") { it ->
            val tmp = it[0] as JSONArray
            queue.removeAll(queue)
            JsonReader(StringReader(tmp.toString())).use {reader ->
                reader.beginArray {
                    while (reader.hasNext()){
                        val song = Klaxon().parse<Song>(reader) as Song
                        queue.add(song)
                    }
                }
            }
            val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
            if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
                val adapterTmp = CustomQueueRecyclerViewAdapter(queue,fragmentChooseASongFragment.context!!)
                val recyclerView = fragmentChooseASongFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
                this.runOnUiThread {
                    recyclerView?.adapter = adapterTmp
                }
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
        val homeFragment = HomeFragment()
        fTransaction.replace(R.id.fragment_holder, homeFragment, "fragment_home")
        fTransaction.commit()

        buttonBack.setOnClickListener {

            supportFragmentManager.beginTransaction().replace(R.id.fragment_holder,homeFragment).commit()
            buttonBack.visibility = Button.INVISIBLE
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
    }

}
