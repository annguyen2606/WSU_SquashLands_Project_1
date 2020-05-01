package com.example.squashlandswsuproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomRecyclerViewAdapter(var songs: ArrayList<Song>, var context: Context, val clickListener: (Song) -> Unit): RecyclerView.Adapter<CustomRecyclerViewAdapter.ViewHolder>(), Filterable {
    private var songsNotFiltered : ArrayList<Song> = ArrayList(songs)
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(song: Song, clickListener: (Song) -> Unit) {
            val textViewSongItemName = itemView.findViewById<TextView>(R.id.songItemName)
            val textViewSongItemDuration = itemView.findViewById<TextView>(R.id.songItemDuration)
            var minute = song.duration.toInt() / 60
            var second = song.duration.toInt() % 60

            if (second == 0)
                textViewSongItemDuration.text = minute.toString() + ":00"
            else
                textViewSongItemDuration.text = minute.toString() + ":" + second.toString()
            textViewSongItemName.text = song.name
            itemView.setOnClickListener {
                clickListener(song)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.row, parent, false))
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItems(songs[position], clickListener)
    }

    fun getSongUri(position: Int): String{
        return songs[position].uri
    }

    override fun getFilter(): Filter {
        return songFilter
    }

    private var songFilter: Filter = object : Filter() {
        override fun performFiltering(p0: CharSequence?): FilterResults {
            var filteredSong = ArrayList<Song>()
            if (p0 == null || p0.length ==0){
                filteredSong.addAll(songsNotFiltered)
            }else{
                var strQueryPattern = p0.toString().toLowerCase().trim()
                songsNotFiltered.forEach {
                    if (it.name.toLowerCase().contains(strQueryPattern))
                        filteredSong.add(it)
                }
            }
            var filterResults = FilterResults()
            filterResults.values = filteredSong
            return filterResults
        }

        override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            songs.clear()
            songs.addAll(p1?.values as ArrayList<Song>)
            notifyDataSetChanged()
        }
    }
}

