package com.example.squashlandswsuproject

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RequestSongFragment : Fragment(R.layout.fragment_request_songs){
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerViewRequest = view.findViewById<RecyclerView>(R.id.recyclerViewRequest)
        var adapter = CustomRequestRecyclerViewAdapter(MainActivity.request,view.context)

        recyclerViewRequest.adapter = adapter
        recyclerViewRequest.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
    }
}