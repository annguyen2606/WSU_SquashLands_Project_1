package com.example.squashlandswsuproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomRequestRecyclerViewAdapter (var request: ArrayList<Request>, var context: Context): RecyclerView.Adapter<CustomRequestRecyclerViewAdapter.ViewHolder>(){
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        fun bindItems(request: Request){
            val textViewRequestSongName = itemView.findViewById<TextView>(R.id.textViewRequestSongName)
            textViewRequestSongName.text = request.songName
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomRequestRecyclerViewAdapter.ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_request, parent, false))
    }

    override fun getItemCount(): Int {
        return request.size
    }

    override fun onBindViewHolder(holder: CustomRequestRecyclerViewAdapter.ViewHolder, position: Int) {
        holder.bindItems(request[position])
    }
}