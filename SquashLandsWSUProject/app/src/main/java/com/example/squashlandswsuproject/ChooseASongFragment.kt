package com.example.squashlandswsuproject

import android.content.DialogInterface
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import org.json.JSONObject
import kotlin.coroutines.coroutineContext
import kotlin.reflect.jvm.internal.impl.descriptors.annotations.BuiltInAnnotationDescriptor


class ChooseASongFragment : Fragment(R.layout.fragment_choose_a_song){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val libTmp: ArrayList<Song> = ArrayList(MainActivity.library)
        val queueTmp = ArrayList(MainActivity.queue)
        val recyclerViewQueue = view.findViewById<RecyclerView>(R.id.recyclerViewQueue)
        val recyclerViewLib = view.findViewById<RecyclerView>(R.id.recyclerView)
        val buttonBack = view.findViewById<Button>(R.id.buttonChooseASongBack)

        val adapter = CustomRecyclerViewAdapter(libTmp,view.context,{song -> onSongClick(song)})
        recyclerViewLib.adapter = adapter
        recyclerViewLib.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        val queueAdapter = CustomQueueRecyclerView(queueTmp,view.context)
        recyclerViewQueue.adapter = queueAdapter
        recyclerViewQueue.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        val searchView = view.findViewById<SearchView>(R.id.searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                adapter.filter.filter(p0)
                return false
            }

        })

        buttonBack.setOnClickListener {
            val ft = this.fragmentManager?.beginTransaction()
            ft?.remove(this)
            ft?.replace(R.id.fragment_holder, HomeFragment(), "fragment_home")
            ft?.commit()
        }
    }

    private fun onSongClick(song: Song) {
        val alertDialogNotConnected: AlertDialog.Builder = AlertDialog.Builder(this.context!!)
        alertDialogNotConnected.setCancelable(true)
        alertDialogNotConnected.setTitle("I want to play " + song.name + "!")
        alertDialogNotConnected.setMessage("Are you sure?")
        alertDialogNotConnected.setNegativeButton("No",
            DialogInterface.OnClickListener { dialogInterface, _ ->
                dialogInterface.dismiss()
            })
        alertDialogNotConnected.setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, _->
            if(MainActivity.queue.size >= MainActivity.queueSize){
                Toast.makeText(this.context,"Queue is full! There should be only ${MainActivity.queueSize} songs in queue",Toast.LENGTH_LONG).show()
            }else if(MainActivity.queue.size < MainActivity.queueSize && MainActivity.queue.find{ it.uri == song.uri} == null) {
                song.id = (MainActivity.queue.get(MainActivity.queue.lastIndex).id + 1)
                val recyclerViewQueue = this.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
                val queueTmp = MainActivity.queue
                queueTmp.add(song)
                val adapterTmp = CustomQueueRecyclerView(queueTmp, this.context!!)
                recyclerViewQueue?.adapter = adapterTmp
                val jsonString = Klaxon().toJsonString(song)
                MainActivity.socket.emit("add song to queue from tablet", jsonString )
            }else if(MainActivity.queue.size < MainActivity.queueSize && MainActivity.queue.find{ it.uri == song.uri} != null){
                Toast.makeText(this.context,"The song \"${song.name}\" already exists in the queue",Toast.LENGTH_LONG).show()
            }
        })
        alertDialogNotConnected.show()
    }
}