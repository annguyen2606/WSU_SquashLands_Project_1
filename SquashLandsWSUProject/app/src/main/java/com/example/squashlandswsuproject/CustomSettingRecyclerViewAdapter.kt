package com.example.squashlandswsuproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList

class CustomSettingRecyclerViewAdapter(var songs: ArrayList<Song>, var context: Context): RecyclerView.Adapter<CustomSettingRecyclerViewAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        fun bindItem(song: Song){
            val textViewSongName = itemView.findViewById<TextView>(R.id.textViewSettingListSongName)
            val textViewTime = itemView.findViewById<TextView>(R.id.textViewSettingListTime)
            val textViewArtist = itemView.findViewById<TextView>(R.id.textViewSettingListArtist)

            var uri = song.uri
            var uriArray = uri.split(Regex("/Music%20Videos/"))
            var fileName = uriArray[1].split(Regex("-"))
            var artist = fileName[0].replace("%20", " ")
            textViewArtist.text = artist

            val minute = song.duration.toInt() / 60
            val second = song.duration.toInt() % 60
            if(second == 0)
                textViewTime.text = "$minute:00"
            else
                textViewTime.text = "$minute:$second"
            textViewSongName.text = song.name
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(R.layout.row_setting_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(songs[position])
    }

    //Declare functions for items editing
    fun removeItem(position: Int) {
        songs.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeRemoved(position, songs.size)
        notifyItemRangeChanged(position, songs.size)
    }

    fun moveItem(oldPos: Int, newPos: Int) {
        if (oldPos < newPos) {
            for (i in oldPos until newPos) {
                Collections.swap(songs, i, i + 1)
            }
        } else {
            for (i in oldPos downTo newPos + 1) {
                Collections.swap(songs, i, i - 1)
            }
        }

        notifyItemMoved(oldPos, newPos)
        notifyItemChanged(oldPos)
        notifyItemChanged(newPos)
    }

    fun getSongAt(position: Int): Song{
        return songs[position]
    }

    fun repopulateData(newSongs: ArrayList<Song>): Boolean{
        songs.clear()
        songs.addAll(newSongs)
        this.notifyDataSetChanged()
        return true
    }

}