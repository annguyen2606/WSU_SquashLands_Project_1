package com.example.squashlandswsuproject

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChooseASongFragment : Fragment(R.layout.fragment_choose_a_song){
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        
        var libTmp: ArrayList<Song> = ArrayList(MainActivity.library)
        var queueTmp = ArrayList(MainActivity.queue)
        var recyclerViewQueue = view.findViewById<RecyclerView>(R.id.recyclerViewQueue)
        var recyclerViewLib = view.findViewById<RecyclerView>(R.id.recyclerView)
        val buttonBack = view.findViewById<Button>(R.id.buttonChooseASongBack)

        var adapter = CustomeRyclerViewAdapter(libTmp,view.context,{song -> onSongClick(song)})
        recyclerViewLib.adapter = adapter
        recyclerViewLib.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        var queueAdapter = CustomQueueRecyclerView(queueTmp,view.context)
        recyclerViewQueue.adapter = queueAdapter
        recyclerViewQueue.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        var searchView = view.findViewById<SearchView>(R.id.searchView)

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
            var ft = this.fragmentManager?.beginTransaction()
            ft?.remove(this)
            ft?.replace(R.id.fragment_holder, HomeFragment(), "fragment_home")
            ft?.commit()
        }
    }

    private fun onSongClick(song: Song) {
        var alertDialogNotConnected: AlertDialog.Builder = AlertDialog.Builder(this.context!!)
        alertDialogNotConnected.setCancelable(true)
        alertDialogNotConnected.setTitle("I want to play " + song.name + "!!!!!")
        alertDialogNotConnected.setMessage("Are you sure?????")
        alertDialogNotConnected.setNegativeButton("No",
            DialogInterface.OnClickListener { dialogInterface, i ->
                dialogInterface.dismiss()
            })
        alertDialogNotConnected.setPositiveButton("Yes", DialogInterface.OnClickListener { dialogInterface, i->
            dialogInterface.cancel()
        })
        alertDialogNotConnected.show()
    }
}