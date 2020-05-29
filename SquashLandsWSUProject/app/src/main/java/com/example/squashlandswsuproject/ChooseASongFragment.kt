package com.example.squashlandswsuproject

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.fragment_choose_a_song.*


class ChooseASongFragment : Fragment(R.layout.fragment_choose_a_song){
    //declare public variables of Search View and Recycler View Adapter
    lateinit var searchView: SearchView
    lateinit var recyclerViewAdapterQueue:CustomQueueRecyclerViewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        //setup Back Button visibility when fragment view on create
        val buttonBack = activity?.findViewById<Button>(R.id.buttonBack)
        buttonBack?.visibility = Button.VISIBLE

        //initiate recyclerViewAdapterQueue
        recyclerViewAdapterQueue = CustomQueueRecyclerViewAdapter(MainActivity.queue, context!!)
        return super.onCreateView(inflater, container, savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val libTmp: ArrayList<SongWithArtist> = ArrayList(MainActivity.library)
        val recyclerViewQueue = view.findViewById<RecyclerView>(R.id.recyclerViewQueue)
        val recyclerViewLib = view.findViewById<RecyclerView>(R.id.recyclerView)

        //declare adapter of recycler view for displaying library with a Unit type variable (a function) declared in line 78
        val adapter = CustomRecyclerViewAdapter(libTmp,view.context) { song -> onSongClick(song)}

        //assign adapter of library recycler view with vertical divider
        recyclerViewLib.adapter = adapter
        recyclerViewLib.addItemDecoration(DividerItemDecoration(context,DividerItemDecoration.VERTICAL))
        recyclerViewLib.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        //assign adapter of queue recycler view with vertical divider
        recyclerViewQueue.adapter = recyclerViewAdapterQueue
        recyclerViewQueue.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recyclerViewQueue.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        searchView = view.findViewById<SearchView>(R.id.searchView)
        //set listeners of search view
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

    //set onClick listener for library recycler view
    private fun onSongClick(song: SongWithArtist) {
        val alertDialogNotConnected: AlertDialog.Builder = AlertDialog.Builder(this.context!!)
        alertDialogNotConnected.setCancelable(true)
        alertDialogNotConnected.setTitle("I want to play " + song.song.name + "!")
        alertDialogNotConnected.setMessage("Are you sure?")
        alertDialogNotConnected.setNegativeButton("No",
            DialogInterface.OnClickListener { dialogInterface, _ ->
                dialogInterface.dismiss()
            })
        alertDialogNotConnected.setPositiveButton("Yes", DialogInterface.OnClickListener { _, _->
            if(MainActivity.queue.size >= MainActivity.queueSize){
                Toast.makeText(this.context,"Queue is full! There should be only ${MainActivity.queueSize} songs in queue",Toast.LENGTH_LONG).show()
            }else if(MainActivity.queue.size < MainActivity.queueSize && MainActivity.queueActual.find{ it.uri == song.song.uri} == null) {
                //assign ID for the new song
                song.song.id = ((MainActivity.queueActual.maxBy { it.id }?.id?.toInt())!! + 1).toString()
                val recyclerViewQueue = this.view?.findViewById<RecyclerView>(R.id.recyclerViewQueue)

                //add the song to the setting queue
                MainActivity.queueSetting.add(song.song)

                val adapter = recyclerViewQueue?.adapter as CustomQueueRecyclerViewAdapter

                //add song to MainActivity.queue by addItem() method of adapter
                adapter.addItem(song.song)

                //cast song as JSON string
                val jsonString = Klaxon().toJsonString(song.song)

                //socket fire event with payload is jsonString
                MainActivity.socket.emit("add song to queue from tablet", jsonString )

                //add song to actual playlist
                MainActivity.queueActual.add(song.song)

                Toast.makeText(this.context, "Added ${song.song.name} to the queue",Toast.LENGTH_SHORT).show()
            }else if(MainActivity.queue.size < MainActivity.queueSize && MainActivity.queue.find{ it.uri == song.song.uri} != null){
                Toast.makeText(this.context,"The song \"${song.song.name}\" already exists in the queue",Toast.LENGTH_LONG).show()
            }

            //clear query string on search view and clear the focus
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