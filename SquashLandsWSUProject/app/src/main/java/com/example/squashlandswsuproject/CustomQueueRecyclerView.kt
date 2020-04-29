package com.example.squashlandswsuproject

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Size
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.row_queue.view.*

class CustomQueueRecyclerView (var songs: ArrayList<Song>, var context: Context): RecyclerView.Adapter<CustomQueueRecyclerView.ViewHolder>(){

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(song: Song, position: Int) {
            val textViewSongItemName = itemView.findViewById<TextView>(R.id.songItemQueueName)
            val textViewSongItemDuration = itemView.findViewById<TextView>(R.id.songItemQueueDuration)
            val textViewSongIndex = itemView.findViewById<TextView>(R.id.songItemQueueIndex)
            val tv = itemView.findViewById<TextView>(R.id.songItemQueueStatus)
            var minute = song.duration.toInt() / 60
            var second = song.duration.toInt() % 60

            if (second == 0)
                textViewSongItemDuration.text = minute.toString() + ":00"
            else
                textViewSongItemDuration.text = minute.toString() + ":" + second.toString()
            textViewSongItemName.text = song.name
            textViewSongIndex.text = (position+1).toString() + ". "
            if(position == 0){
                tv.text = "Playing"
                tv.setTextColor(Color.rgb(63,122,77))
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0F)
            }else if(position == 1){
                tv.text = "Next"
                tv.setTextColor(Color.rgb(255,140,0))
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_queue, parent, false))
    }

    override fun getItemCount(): Int {
        return songs.size
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(songs[position], position)
    }
}