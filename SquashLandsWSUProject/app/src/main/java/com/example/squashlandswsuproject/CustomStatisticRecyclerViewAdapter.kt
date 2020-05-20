package com.example.squashlandswsuproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomStatisticRecyclerViewAdapter(var queueds: ArrayList<Queued>, var context: Context, var type: Int, var isSongStats: Boolean?):
    RecyclerView.Adapter<CustomStatisticRecyclerViewAdapter.ViewHolder>() {

    //private values for item type
    private val type_header = 0
    private val type_item = 1

    //private lists after performing counting on 'queueds'
    private val songPopularity = queueds.groupingBy { it.songName }.eachCount().toList().sortedBy { it.second }.reversed()
    private val datePopularity = queueds.groupingBy { it.timeStamp }.eachCount().toList().sortedBy { it.second }.reversed()

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        fun bindHeaderPopularity(songMaxCount: Pair<String, Int>?, dateMaxCount: Pair<String, Int>?){
            val songName = itemView.findViewById<TextView>(R.id.textViewStatisticMostQueuedSong)
            val songCount = itemView.findViewById<TextView>(R.id.textViewStatisticMostQueuedSongCount)

            val date = itemView.findViewById<TextView>(R.id.textViewStatisticMostQueuedDate)
            val dateCount = itemView.findViewById<TextView>(R.id.textViewStatisticMostQueuedDateCount)

            songName.text = songMaxCount?.first
            songCount.text = songMaxCount?.second.toString()

            date.text = dateMaxCount?.first
            dateCount.text = dateMaxCount?.second.toString()
        }

        fun bindItem(queued: Queued){
            val textViewSongName = itemView.findViewById<TextView>(R.id.textViewRowStatisticNormalSongName)
            val textViewDate = itemView.findViewById<TextView>(R.id.textViewRowStatisticNormalDate)
            val textViewQueuer = itemView.findViewById<TextView>(R.id.textViewRowStatisticNormalQueuer)

            textViewSongName.text = queued.songName
            textViewDate.text = queued.timeStamp
            textViewQueuer.text = queued.queuer
        }

        fun bindItemSongPopularity(pair: Pair<String, Int>){
            //TODO
            val textViewSongName = itemView.findViewById<TextView>(R.id.textViewRowStatisticSongPopSong)
            val textViewSongCount = itemView.findViewById<TextView>(R.id.textViewRowStatisticSongPopCount)

            textViewSongName.text = pair.first
            textViewSongCount.text = pair.second.toString()
        }

        fun bindItemDatePopularity(pair: Pair<String, Int>){
            val textViewDate = itemView.findViewById<TextView>(R.id.textViewRowStatisticDatePopDate)
            val textViewDateCount = itemView.findViewById<TextView>(R.id.textViewRowStatisticDatePopCount)

            textViewDate.text = pair.first
            textViewDateCount.text = pair.second.toString()
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if(type == 0)
            type_item
        else{
            if(position == 0)
                type_header
            else
                type_item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        //if type is 0 inflate normal layout; else inflate header and popularity layouts
        return if (type == 0)
            ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_statistic_item_normal, parent, false))
        else{
            if (viewType == type_header)
                ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_statistic_header_popularity, parent, false))
            else {
                if(isSongStats!!)
                    ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_statistic_item_song_popularity, parent, false))
                else
                    ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_statistic_item_date_popularity, parent, false))
            }
        }
    }

    override fun getItemCount(): Int {
        //as if type == 1 there will be header, size needs to be +1
        return if(type == 1){
            if (isSongStats!!)
                songPopularity.size + 1
            else
                datePopularity.size + 1
        } else
            queueds.size
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //if type == 1 perform normal binding; else, at first position, perform header binding and perform counting item binding in next positions
        if(type == 0)
            holder.bindItem(queueds[position])
        else{
            if(position == 0)
                holder.bindHeaderPopularity(songPopularity.maxBy { it.second }, datePopularity.maxBy { it.second })
            else{
                if(isSongStats!!)
                    holder.bindItemSongPopularity(songPopularity.toList()[position-1])
                else
                    holder.bindItemDatePopularity(datePopularity.toList()[position-1])
            }
        }
    }
}