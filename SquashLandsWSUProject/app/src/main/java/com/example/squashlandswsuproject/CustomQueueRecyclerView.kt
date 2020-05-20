package com.example.squashlandswsuproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.squashlandswsuproject.databinding.RowQueuePlayingBinding

class CustomQueueRecyclerViewAdapter (var songs: ArrayList<Song>, var context: Context): RecyclerView.Adapter<CustomQueueRecyclerViewAdapter.ViewHolder>(){
    val type_playing = 0
    val type_queue = 1
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(song: Song) {
            val textViewSongItemName = itemView.findViewById<TextView>(R.id.songItemQueueName)
            val textViewSongItemDuration = itemView.findViewById<TextView>(R.id.songItemQueueDuration)
            val textViewSongItemArtist = itemView.findViewById<TextView>(R.id.songItemQueueArtist)

            //extract artist name
            var uri = song.uri
            var uriArray = uri.split(Regex("/Music%20Videos/"))
            var fileName = uriArray[1].split(Regex("-"))
            var artist = fileName[0].replace("%20", " ")
            textViewSongItemArtist.text = artist

            //extract minute and second number
            val minute = song.duration.toInt() / 60
            val second = song.duration.toInt() % 60

            if (second == 0)
                textViewSongItemDuration.text = minute.toString() + ":00"
            else
                textViewSongItemDuration.text = minute.toString() + ":" + second.toString()
            textViewSongItemName.text = song.name
        }

        fun bindPlayingItem(song: Song){
            var status = true
            val textViewPlaying = itemView.findViewById<TextView>(R.id.textViewSettingPlayingSong)
            val textViewTime  = itemView.findViewById<TextView>(R.id.textViewSettingPlayingTime)
            val minute = song.duration.toInt() / 60
            val second = song.duration.toInt() % 60
            val rotation = AnimationUtils.loadAnimation(itemView.context,R.anim.rotate)
            val indicator = itemView.findViewById<ImageView>(R.id.imageViewSettingLogo)
            val artistTextView = itemView.findViewById<TextView>(R.id.textViewQueuePlayingArtist)

            var uri = song.uri
            var uriArray = uri.split(Regex("/Music%20Videos/"))
            var fileName = uriArray[1].split(Regex("-"))
            var artist = fileName[0].replace("%20", " ")
            artistTextView.text = artist
            rotation.interpolator = LinearInterpolator()
            rotation.fillAfter = true
            textViewPlaying.text = song.name
            indicator.startAnimation(rotation)
            if (second == 0)
                textViewTime.text = minute.toString() + ":00"
            else
                textViewTime.text = minute.toString() + ":" + second.toString()

            //process logo animation on 'sync status' event
            MainActivity.socket.on("sync status"){
                if(MainActivity.playingStatus != status){
                    status = MainActivity.playingStatus
                    if(status){
                        indicator.startAnimation(rotation)
                    }else
                        indicator.clearAnimation()
                }
            }
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if(viewType == type_queue)
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_queue, parent, false))
        else{

            //declare binding variables and process on binding variables on 'sync status' event
            val inflater = LayoutInflater.from(context)
            var rowQueuePlayingBinding:RowQueuePlayingBinding = DataBindingUtil.inflate(inflater, R.layout.row_queue_playing, parent, false )
            rowQueuePlayingBinding.time = CurrentTime(MainActivity.time)
            rowQueuePlayingBinding.status = CurrentStatus(MainActivity.playingStatus)
            rowQueuePlayingBinding.currentProgress = CurrentProgress(MainActivity.currentProgress, MainActivity.currentMaxSecond)
            MainActivity.socket.on("sync status"){
                rowQueuePlayingBinding.time!!.setTimeStr(MainActivity.time)
                rowQueuePlayingBinding.status!!.setStatus(MainActivity.playingStatus)
                rowQueuePlayingBinding.currentProgress!!.setCurrentSecond(MainActivity.currentProgress)
                rowQueuePlayingBinding.invalidateAll()
            }

            //set new max values for progress bar in row_queue_playing.xml
            MainActivity.socket.on("respond to sync"){
                if(MainActivity.connectStatus){
                    rowQueuePlayingBinding.currentProgress!!.setMaxSecond(MainActivity.currentMaxSecond)
                }
            }
            return ViewHolder(rowQueuePlayingBinding.root)
        }
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
            holder.bindItems(songs[position])
        else{
            holder.bindPlayingItem(songs[position])
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