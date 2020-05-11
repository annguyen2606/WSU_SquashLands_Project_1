package com.example.squashlandswsuproject

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.DialogInterface
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.fragment_request_songs.*
import kotlinx.android.synthetic.main.fragment_settings.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs


@Suppress("DEPRECATION")
class SettingFragment: Fragment(R.layout.fragment_settings) {
    var queueSettingTmp = arrayListOf<Song>()
    lateinit var recyclerViewAdapter: CustomSettingRecyclerViewAdapter
    var logoutHandler = Handler()
    lateinit var runnableLogout: Runnable
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val buttonBack = activity?.findViewById<Button>(R.id.buttonBack)
        buttonBack?.visibility = Button.VISIBLE
        queueSettingTmp.addAll(MainActivity.queueSetting)
        val ftTransaction = activity?.supportFragmentManager?.beginTransaction()
        runnableLogout = Runnable {
            activity?.runOnUiThread {
                ftTransaction?.replace(R.id.fragment_holder,HomeFragment(), "fragment_home")
                ftTransaction?.commit()
                buttonBack?.visibility = Button.INVISIBLE
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @SuppressLint("CommitPrefEdits")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerViewAdapter = CustomSettingRecyclerViewAdapter(MainActivity.queueSetting, view.context)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewSetting)

        recyclerView.adapter = recyclerViewAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL))
        recyclerView.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        val itemTouchHelper = ItemTouchHelper(createItemTouchHelper()!!)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        recyclerView.addOnItemTouchListener(CustomRecyclerViewOnItemTouchListener(activity, recyclerView, object : ClickListener {
            override fun onClick(view: View?, position: Int) {

            }

            override fun onClickUp(view: View?, position: Int) {

            }

            override fun onLongClick(view: View?, position: Int) {

            }
        })
        )

        buttonSubmitQueue.setOnClickListener {
            val jsonObj = Klaxon().toJsonString(recyclerViewAdapter.songs)
            MainActivity.socket.emit("modify queue", jsonObj)
            queueSettingTmp.clear()
            queueSettingTmp.addAll(recyclerViewAdapter.songs)
            buttonSubmitQueue.visibility = Button.INVISIBLE
            Toast.makeText(view.context,"Queue Modified Successfully", Toast.LENGTH_LONG).show()
        }
        val editTextAnnouncement = view.findViewById<EditText>(R.id.editTextSettingAnnouncement)
        val editTextIdleInterval = view.findViewById<EditText>(R.id.editTextSettingIdleInterval)
        val radioButtonRotation = view.findViewById<RadioButton>(R.id.radioButtonSettingScreenSaverRotation)
        val radioButtonPumping = view.findViewById<RadioButton>(R.id.radioButtonSettingScreenSaverPumping)
        val buttonSettingApply = view.findViewById<Button>(R.id.buttonSettingApply)
        val buttonSettingLoadDefault = view.findViewById<Button>(R.id.buttonSettingLoadDefault)
        val sharedReferencesEditor  = activity?.getSharedPreferences(MainActivity.preferencesStr, MODE_PRIVATE)!!.edit()

        buttonSettingApply.setOnClickListener {
            sharedReferencesEditor.putString(MainActivity.announcementStr, editTextAnnouncement.text.toString())
            sharedReferencesEditor.putInt(MainActivity.idleIntervalStr, editTextIdleInterval.text.toString().toInt())
            if(radioButtonRotation.isChecked) {
                sharedReferencesEditor.putString(
                    MainActivity.screenSaverAnimation,
                    radioButtonRotation.text.toString()
                )
                MainActivity.screenSaverAnimation = radioButtonRotation.text.toString()
            }else if(radioButtonPumping.isChecked) {
                sharedReferencesEditor.putString(
                    MainActivity.screenSaverAnimationStr,
                    radioButtonPumping.text.toString()
                )
                MainActivity.screenSaverAnimation = radioButtonPumping.text.toString()
            }

            sharedReferencesEditor.apply()
            val textViewAnnouncement = activity?.findViewById<TextView>(R.id.textViewAnnouncement)
            textViewAnnouncement?.text = editTextAnnouncement.text.toString()

            MainActivity.announcement = editTextAnnouncement.text.toString()
            MainActivity.idleInterval = editTextIdleInterval.text.toString().toInt()
        }

        buttonSettingLoadDefault.setOnClickListener {
            editTextAnnouncement.setText(resources.getText(R.string.default_announcement))
            editTextIdleInterval.setText("2")
            radioButtonRotation.isChecked = true
        }

        editTextAnnouncement.setText(MainActivity.announcement)
        editTextIdleInterval.setText(MainActivity.idleInterval.toString())
        if(MainActivity.screenSaverAnimation.contentEquals("Rotation"))
            radioButtonRotation.isChecked = true
        else if(MainActivity.screenSaverAnimation.contentEquals("Pumping"))
            radioButtonPumping.isChecked = true

        super.onViewCreated(view, savedInstanceState)
    }

    fun startLogoutHandler(){
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
        val queueTmp = arrayListOf<Song>()
        queueTmp.addAll(MainActivity.queue)
        if(queueTmp.size > 0)
            queueTmp.removeAt(0)

        if(queueSettingTmp != MainActivity.queueSetting){
            if(queueSettingTmp == queueTmp)
                MainActivity.queueSetting = queueSettingTmp
            else
                MainActivity.queueSetting = queueTmp
        }
        super.onDestroy()
    }

    private fun createItemTouchHelper(): ItemTouchHelper.Callback? {
        return object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                recyclerViewAdapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onMoved(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, fromPos: Int, target: RecyclerView.ViewHolder, toPos: Int, x: Int, y: Int
            ) {

                if(queueSettingTmp == MainActivity.queueSetting){
                    buttonSubmitQueue.visibility = Button.INVISIBLE
                }else
                    buttonSubmitQueue.visibility = Button.VISIBLE
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y)
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val alertDialog: AlertDialog.Builder = AlertDialog.Builder(activity!!)
                alertDialog.setCancelable(true)
                alertDialog.setTitle("Confirm")
                alertDialog.setMessage("Delete ${recyclerViewAdapter.getSongAt(viewHolder.adapterPosition).name} ?")
                alertDialog.setNegativeButton("No",
                    DialogInterface.OnClickListener { dialogInterface, _ ->
                        dialogInterface.cancel()
                        recyclerViewAdapter.notifyItemChanged(viewHolder.adapterPosition)
                    }
                )
                alertDialog.setPositiveButton("Yes",
                    DialogInterface.OnClickListener { _, _ ->
                        MainActivity.socket.emit("remove song from queue", Klaxon().toJsonString(recyclerViewAdapter.getSongAt(viewHolder.adapterPosition)))
                        recyclerViewAdapter.removeItem(viewHolder.adapterPosition)
                        queueSettingTmp.clear()
                        queueSettingTmp.addAll(recyclerViewAdapter.songs)
                        buttonSubmitQueue.visibility = Button.INVISIBLE
                    }
                )

                alertDialog.show()
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val alpha = 1 - abs(dX) / recyclerView.width
                    viewHolder.itemView.alpha = alpha
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder
            ): Int {
                val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END
                return ItemTouchHelper.Callback.makeMovementFlags(dragFlags, swipeFlags)
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return true
            }
        }
    }
}