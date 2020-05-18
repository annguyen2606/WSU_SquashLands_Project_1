package com.example.squashlandswsuproject

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomQueueRecyclerViewAdapter (var songs: ArrayList<Song>, var context: Context): RecyclerView.Adapter<CustomQueueRecyclerViewAdapter.ViewHolder>(){
    val type_playing = 0
    val type_queue = 1

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        @SuppressLint("ResourceType")
        fun bindItems(song: Song, position: Int) {
            val textViewSongItemName = itemView.findViewById<TextView>(R.id.songItemQueueName)
            val textViewSongItemDuration = itemView.findViewById<TextView>(R.id.songItemQueueDuration)

            val minute = song.duration.toInt() / 60
            val second = song.duration.toInt() % 60

            if (second == 0)
                textViewSongItemDuration.text = minute.toString() + ":00"
            else
                textViewSongItemDuration.text = minute.toString() + ":" + second.toString()
            textViewSongItemName.text = song.name
        }

        fun bindPlayingItem(song: Song, position: Int){
            val textViewPlaying = itemView.findViewById<TextView>(R.id.textViewSettingPlayingSong)
            val textViewTime  = itemView.findViewById<TextView>(R.id.textViewSettingPlayingTime)
            val minute = song.duration.toInt() / 60
            val second = song.duration.toInt() % 60
            val rotation = AnimationUtils.loadAnimation(itemView.context,R.anim.rotate)
            val indicator = itemView.findViewById<ImageView>(R.id.imageViewSettingLogo)
            val status = itemView.findViewById<TextView>(R.id.textViewQueuePlayingStatus)
            rotation.interpolator = LinearInterpolator()
            rotation.fillAfter = true
            textViewPlaying.text = song.name
            indicator.startAnimation(rotation)
            if (second == 0)
                textViewTime.text = minute.toString() + ":00"
            else
                textViewTime.text = minute.toString() + ":" + second.toString()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if(viewType == type_queue)
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_queue, parent, false))
        else
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_queue_playing, parent, false))
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0)
            return type_playing
        else
            return type_queue
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(position != 0)
            holder.bindItems(songs[position], position)
        else{
            holder.bindPlayingItem(songs[position],position)
        }
    }

    fun removeItem(position: Int) {
        songs.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeRemoved(position, songs.size)
    }

    fun repopulateData(newSongs: ArrayList<Song>){
        songs.clear()
        songs.addAll(newSongs)
        this.notifyDataSetChanged()
    }

    fun addItem(song: Song){
        songs.add(song)
        this.notifyItemInserted(songs.lastIndexOf(song))
        this.notifyItemRangeInserted(songs.lastIndexOf(song), songs.size)
    }

    fun getItemPosition(song: Song): Int{
        return songs.indexOf(songs.find { tmp-> tmp.uri == song.uri })
    }
}