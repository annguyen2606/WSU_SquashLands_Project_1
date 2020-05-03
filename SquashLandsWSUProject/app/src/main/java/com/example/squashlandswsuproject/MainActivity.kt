package com.example.squashlandswsuproject

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.StringReader
import java.util.zip.Inflater

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textViewAnnouncement.setSelected(true);

        socket.once("connect",{
            socket.emit("sync library and queue", "request to get library and queue")
            socket.emit("request request list")
        })

        socket.on("respond to sync with library and queue",{
            var tmp = it[0] as JSONArray
            JsonReader(StringReader(tmp[0].toString())).use {
                it.beginArray {
                    while (it.hasNext()){
                        val song = Klaxon().parse<Song>(it) as Song
                        queue.add(song)
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
            queueSize = (tmp[2].toString()).toInt()
        })

        val fTransaction = supportFragmentManager.beginTransaction()
        socket.on("respond to sync", {
            currentSong = it[0].toString()
            if (queue.size > 0){
                if (queue[0].name != currentSong) {
                    queue.removeAt(0)
                    val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
                    if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
                        val adapterTmp = CustomQueueRecyclerViewAdapter(queue,fragmentChooseASongFragment.context!!)
                        val recyclerView = fragmentChooseASongFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
                        this.runOnUiThread({
                            recyclerView?.adapter = adapterTmp
                        })
                    }
                }
            }
        })

        socket.on("web remove song from queue",{
            var uriTmp = it[0].toString()
            val removedSong = queue.find { it.uri == uriTmp }
            if(removedSong != null){
                queue.remove(removedSong)
            }
            val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
            if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
                val adapterTmp = CustomQueueRecyclerViewAdapter(queue,fragmentChooseASongFragment.context!!)
                val recyclerView = fragmentChooseASongFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
                this.runOnUiThread({
                    recyclerView?.adapter = adapterTmp
                })
            }
        })

        socket.on("sync for repopulated queue",{
            var tmp = it[0] as JSONArray
            queue.removeAll(queue)
            JsonReader(StringReader(tmp.toString())).use {
                it.beginArray {
                    while (it.hasNext()){
                        val song = Klaxon().parse<Song>(it) as Song
                        queue.add(song)
                    }
                }
            }
            val fragmentChooseASongFragment = supportFragmentManager.findFragmentByTag("fragment_choose_a_song")
            if(fragmentChooseASongFragment != null && fragmentChooseASongFragment.isVisible){
                val adapterTmp = CustomQueueRecyclerViewAdapter(queue,fragmentChooseASongFragment.context!!)
                val recyclerView = fragmentChooseASongFragment.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
                this.runOnUiThread({
                    recyclerView?.adapter = adapterTmp
                })
            }
        })

        socket.on("respond request request list",{
            JsonReader(StringReader(it[0].toString())).use {
                it.beginArray {
                    while(it.hasNext()){
                        val requestTmp = Klaxon().parse<Request>(it) as Request
                        if (!request.contains(requestTmp))
                            request.add(requestTmp)
                    }
                }
            }
        })

        socket.on("respond add request from tablet",{
            JsonReader(StringReader(it[0].toString())).use {
                it.beginArray {
                    while(it.hasNext()){
                        val requestTmp = Klaxon().parse<Request>(it) as Request
                        if (!request.contains(requestTmp))
                            request.add(requestTmp)
                    }
                }
            }
            val fragmentRequestSong = supportFragmentManager.findFragmentByTag("fragment_request_song")
            if(fragmentRequestSong!= null && fragmentRequestSong.isVisible){
                val adapterTmp = CustomRequestRecyclerViewAdapter(request,fragmentRequestSong?.context!!)
                val recyclerView = fragmentRequestSong.view?.findViewById<RecyclerView>(R.id.recyclerViewRequest)
                this.runOnUiThread({
                    recyclerView?.adapter = adapterTmp
                    Toast.makeText(this, "request sent", Toast.LENGTH_LONG).show()
                })
            }
        })

        val homeFragment = HomeFragment()
        fTransaction.replace(R.id.fragment_holder, homeFragment, "fragment_home")
        fTransaction.commit()
    }

    companion object{
        val socket: Socket = IO.socket("http://192.168.0.3:5000/")
        var currentSong: String = ""
        var queue =  arrayListOf<Song>()
        var library = arrayListOf<Song>()
        var request = arrayListOf<Request>()
        var queueSize = 0
    }

}
