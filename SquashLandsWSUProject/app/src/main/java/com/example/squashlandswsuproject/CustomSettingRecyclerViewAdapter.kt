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
        fun bindItem(song: Song, position: Int){
            val textViewIndex = itemView.findViewById<TextView>(R.id.textViewSettingListIndex)
            val textViewSongName = itemView.findViewById<TextView>(R.id.textViewSettingListSongName)
            val textViewTime = itemView.findViewById<TextView>(R.id.textViewSettingListTime)
            val minute = song.duration.toInt() / 60
            val second = song.duration.toInt() % 60
            if(second == 0)
                textViewTime.text = "$minute:00"
            else
                textViewTime.text = "$minute:$second"
            textViewSongName.text = song.name
            textViewIndex.text = "${position + 1}. "
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
        holder.bindItem(songs[position], position)
    }

    fun removeItem(position: Int) {
        songs.removeAt(position)
        notifyItemRemoved(position)
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

    fun repopulateData(newSongs: ArrayList<Song>){
        songs.clear()
        songs.addAll(newSongs)
        this.notifyDataSetChanged()
    }

}