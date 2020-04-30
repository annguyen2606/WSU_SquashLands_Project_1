package com.example.squashlandswsuproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_request_songs.*

class RequestSongFragment : Fragment(R.layout.fragment_request_songs){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerViewRequest = view.findViewById<RecyclerView>(R.id.recyclerViewRequest)
        val adapter = CustomRequestRecyclerViewAdapter(MainActivity.request,view.context)
        recyclerViewRequest.adapter = adapter
        recyclerViewRequest.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        buttonRequestBack.setOnClickListener {
            val ft = this.fragmentManager?.beginTransaction()
            ft?.remove(this)
            ft?.replace(R.id.fragment_holder, HomeFragment(), "fragment_home")
            ft?.commit()
        }
    }
}