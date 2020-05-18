package com.example.squashlandswsuproject

import android.content.DialogInterface
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.fragment_choose_a_song.*
import kotlin.collections.ArrayList


class ChooseASongFragment : Fragment(R.layout.fragment_choose_a_song){
    lateinit var searchView: SearchView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val buttonBack = activity?.findViewById<Button>(R.id.buttonBack)
        buttonBack?.visibility = Button.VISIBLE
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val libTmp: ArrayList<Song> = ArrayList(MainActivity.library)
        val recyclerViewQueue = view.findViewById<RecyclerView>(R.id.recyclerViewQueue)
        val recyclerViewLib = view.findViewById<RecyclerView>(R.id.recyclerView)

        val adapter = CustomRecyclerViewAdapter(libTmp,view.context) { song -> onSongClick(song)}
        recyclerViewLib.adapter = adapter
        recyclerViewLib.addItemDecoration(DividerItemDecoration(context,DividerItemDecoration.VERTICAL))
        recyclerViewLib.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        val recyclerViewAdapterQueue = CustomQueueRecyclerViewAdapter(MainActivity.queue,view.context)

        recyclerViewQueue.adapter = recyclerViewAdapterQueue
        recyclerViewQueue.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recyclerViewQueue.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        searchView = view.findViewById<SearchView>(R.id.searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                adapter.filter.filter(p0)
                return false
            }

        })

        super.onViewCreated(view, savedInstanceState)
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
        alertDialogNotConnected.setPositiveButton("Yes", DialogInterface.OnClickListener { _, _->
            if(MainActivity.queue.size >= MainActivity.queueSize){
                Toast.makeText(this.context,"Queue is full! There should be only ${MainActivity.queueSize} songs in queue",Toast.LENGTH_LONG).show()
            }else if(MainActivity.queue.size < MainActivity.queueSize && MainActivity.queueActual.find{ it.uri == song.uri} == null) {
                song.id = ((MainActivity.queueActual.maxBy { it.id }?.id?.toInt())!! + 1).toString()
                val recyclerViewQueue = this.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)
                MainActivity.queueSetting.add(song)
                val adapter = recyclerViewQueue?.adapter as CustomQueueRecyclerViewAdapter
                adapter.addItem(song)
                val jsonString = Klaxon().toJsonString(song)
                MainActivity.socket.emit("add song to queue from tablet", jsonString )
                MainActivity.queueActual.add(song)
            }else if(MainActivity.queue.size < MainActivity.queueSize && MainActivity.queue.find{ it.uri == song.uri} != null){
                Toast.makeText(this.context,"The song \"${song.name}\" already exists in the queue",Toast.LENGTH_LONG).show()
            }

            searchView.setQuery("",false)
            searchView.clearFocus()
        })
        alertDialogNotConnected.show()
    }

    override fun onResume() {
        recyclerViewQueue.adapter = CustomQueueRecyclerViewAdapter(MainActivity.queue, this.context!!)
        super.onResume()
    }
}