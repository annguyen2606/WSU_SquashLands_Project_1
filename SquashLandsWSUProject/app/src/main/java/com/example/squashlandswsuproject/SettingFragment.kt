package com.example.squashlandswsuproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration

class SettingFragment: Fragment(R.layout.fragment_settings) {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val buttonBack = activity?.findViewById<Button>(R.id.buttonBack)
        buttonBack?.visibility = Button.VISIBLE
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerViewAdapter = CustomSettingRecyclerViewAdapter(MainActivity.queueSetting, view.context)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewSetting)

        recyclerView.adapter = recyclerViewAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        super.onViewCreated(view, savedInstanceState)
    }
}