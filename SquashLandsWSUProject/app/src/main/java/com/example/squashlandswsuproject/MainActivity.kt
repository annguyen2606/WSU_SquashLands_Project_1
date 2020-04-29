package com.example.squashlandswsuproject

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        })

        socket.on("respond to sync", {
            currentSong = it[0].toString()
            if (queue.size > 0){
                if (queue[0].name != currentSong) {
                    queue.removeAt(0)
                }
            }
        })

        socket.on("sync for repopulated queue",{
            var tmp = it[0] as JSONArray
            queue.removeAll(queue)
            JsonReader(StringReader(tmp.toString())).use {
                it.beginArray {
                    while (it.hasNext()){
                        val song = Klaxon().parse<Song>(it) as Song
                        if(song.uri.contains("/Music%20Videos/"))
                            queue.add(song)
                    }
                }
            }
        })

        socket.on("respons to request request list",{
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
        var homeFragment = HomeFragment()
        var fTransaction = supportFragmentManager.beginTransaction()
        fTransaction.replace(R.id.fragment_holder, homeFragment, "fragment_home")
        fTransaction.commit()
    }

    companion object{
        val socket: Socket = IO.socket("http://192.168.0.3:5000/")
        var currentSong: String = ""
        var queue =  arrayListOf<Song>()
        var library = arrayListOf<Song>()
        var request = arrayListOf<Request>()
    }

}
