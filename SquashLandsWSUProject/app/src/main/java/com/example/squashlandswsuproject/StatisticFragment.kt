package com.example.squashlandswsuproject

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.fragment_statistic.*
import java.io.StringReader

class StatisticFragment: Fragment(R.layout.fragment_statistic) {
    val queueds = arrayListOf<Queued>()
    var statusFlag = false
    var logoutHandler = Handler()
    lateinit var runnableLogout: Runnable
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val buttonBack = activity?.findViewById<Button>(R.id.buttonBack)
        buttonBack?.visibility = Button.VISIBLE
        MainActivity.socket.emit("request statistics log")
        MainActivity.socket.on("respond request statistic log"){
            JsonReader(StringReader(it[0].toString())).use { reader ->
                reader.beginArray {
                    while(reader.hasNext()){
                        val queuedTmp = Klaxon().parse<Queued>(reader) as Queued
                        queueds.add(queuedTmp)
                    }
                }
            }
            statusFlag = true
        }
        runnableLogout = Runnable {
            val ftTransaction = activity?.supportFragmentManager?.beginTransaction()
            activity?.runOnUiThread {
                ftTransaction?.replace(R.id.fragment_holder,HomeFragment(), "fragment_home")
                ftTransaction?.commit()
                buttonBack?.visibility = Button.INVISIBLE
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerViewStatistic = view.findViewById<RecyclerView>(R.id.recyclerViewStatistic)
        val toggleButton = view.findViewById<ToggleButton>(R.id.toggleButtonStatsMode)
        val radioGroup  = view.findViewById<RadioGroup>(R.id.radioGroupPopularityMode)
        val radioButtonPopSongMode = view.findViewById<RadioButton>(R.id.radioButtonStatsBySong)
        val radioButtonPopDateMode = view.findViewById<RadioButton>(R.id.radioButtonStatsByDate)

        val buttonLoad = view.findViewById<Button>(R.id.buttonStatisticLoad)
        recyclerViewStatistic.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recyclerViewStatistic.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        buttonLoad.setOnClickListener {
            if (statusFlag) {
                it.visibility = Button.INVISIBLE
                val adapterNormal = CustomStatisticRecyclerViewAdapter(queueds,view.context,0,null)
                recyclerViewStatistic.adapter = adapterNormal
                recyclerViewStatistic.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
                val frameHolder = view. findViewById<ConstraintLayout>(R.id.frameHolderStatisticControl)
                frameHolder.visibility = ConstraintLayout.VISIBLE
            }else
                Toast.makeText(this.context, "Statistics log is not ready yet, please wait for a second", Toast.LENGTH_LONG).show()
        }


        toggleButton.setOnClickListener {
            if (toggleButton.isChecked) {
                radioGroup.visibility = RadioGroup.VISIBLE
                if(radioButtonPopDateMode.isChecked){
                    val adapterPopDate = CustomStatisticRecyclerViewAdapter(queueds, view.context, 1, false)
                    recyclerViewStatistic.adapter = adapterPopDate
                }else if(radioButtonPopSongMode.isChecked){
                    val adapterPopSong = CustomStatisticRecyclerViewAdapter(queueds, view.context, 1, true)
                    recyclerViewStatistic.adapter = adapterPopSong
                }
            }else{
                val adapterNormal = CustomStatisticRecyclerViewAdapter(queueds,view.context,0,null)
                radioGroup.visibility = RadioGroup.INVISIBLE
                recyclerViewStatistic.adapter = adapterNormal
            }
        }

        radioButtonPopSongMode.setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isChecked){
                val adapterPopSong = CustomStatisticRecyclerViewAdapter(queueds, view.context, 1, true)
                recyclerViewStatistic.adapter = adapterPopSong
            }
        }

        radioButtonPopDateMode.setOnCheckedChangeListener { compoundButton, b ->
            if (compoundButton.isChecked){
                val adapterPopDate = CustomStatisticRecyclerViewAdapter(queueds, view.context, 1, false)
                recyclerViewStatistic.adapter = adapterPopDate
            }
        }

        startLogoutHandler()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun startLogoutHandler(){
        logoutHandler.postDelayed(runnableLogout, 30000)
    }

    fun stopLogoutHandler(){
        logoutHandler.removeCallbacks(runnableLogout)
    }

    fun resetLogoutHandler(){
        stopLogoutHandler()
        startLogoutHandler()
    }

    override fun onDestroy() {

        stopLogoutHandler()
        super.onDestroy()

    }
}