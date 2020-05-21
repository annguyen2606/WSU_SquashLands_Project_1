package com.example.squashlandswsuproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
//pass ArrayList, Context, and onclicklistener which is declared from fragment
class CustomRecyclerViewAdapter(var songs: ArrayList<SongWithArtist>, var context: Context, private val clickListener: (SongWithArtist) -> Unit): RecyclerView.Adapter<CustomRecyclerViewAdapter.ViewHolder>(), Filterable {
    private var songsNotFiltered : ArrayList<SongWithArtist> = ArrayList(songs)

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(song: SongWithArtist, clickListener: (SongWithArtist) -> Unit) {
            val textViewSongItemName = itemView.findViewById<TextView>(R.id.songItemName)
            val textViewSongItemDuration = itemView.findViewById<TextView>(R.id.songItemDuration)
            val textViewSongItemArtist = itemView.findViewById<TextView>(R.id.songItemArtist)

            //extract artist name from uri string
            var uri = song.song.uri
            var uriArray = uri.split(Regex("/Music%20Videos/"))
            var fileName = uriArray[1].split(Regex("-"))
            var artist = fileName[0].replace("%20", " ")


            textViewSongItemArtist.text = artist

            //extract minute and second numbers from duration string
            val minute = song.song.duration.toInt() / 60
            val second = song.song.duration.toInt() % 60

            if (second == 0)
                textViewSongItemDuration.text = minute.toString() + ":00"
            else
                textViewSongItemDuration.text = minute.toString() + ":" + second.toString()
            textViewSongItemName.text = song.song.name

            //set onClickListener for the itemView
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


    override fun getFilter(): Filter {
        return songFilter
    }

    private var songFilter: Filter = object : Filter() {
        override fun performFiltering(p0: CharSequence?): FilterResults {
            var filteredSong = ArrayList<SongWithArtist>()
            if (p0 == null || p0.length ==0){
                filteredSong.addAll(songsNotFiltered)
            }else{
                val strQueryPattern = p0.toString().toLowerCase().trim()
                songsNotFiltered.forEach {
                    if (it.song.name.toLowerCase().contains(strQueryPattern) || it.artist.toLowerCase().contains(strQueryPattern))
                        filteredSong.add(it)
                }
            }
            val filterResults = FilterResults()
            filterResults.values = filteredSong
            return filterResults
        }

        override fun publishResults(p0: CharSequence?, p1: FilterResults?) {
            songs.clear()
            songs.addAll(p1!!.values as ArrayList<SongWithArtist>)
            notifyDataSetChanged()
        }
    }
}

