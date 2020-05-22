package com.example.squashlandswsuproject

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.fragment_request_songs.*

class RequestSongFragment : Fragment(R.layout.fragment_request_songs){
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val buttonBack = activity?.findViewById<Button>(R.id.buttonBack)
        buttonBack?.visibility = Button.VISIBLE
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerViewRequest = view.findViewById<RecyclerView>(R.id.recyclerViewRequest)
        val adapter = CustomRequestRecyclerViewAdapter(MainActivity.request,view.context)
        val buttonSubmit = view.findViewById<Button>(R.id.buttonSubmitRequest)
        recyclerViewRequest.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerViewRequest.adapter = adapter
        recyclerViewRequest.layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)

        buttonSubmit.setOnClickListener {
            val tmpRequest = Request(patronName = "${view.findViewById<EditText>(R.id.editTextRequestFirstName).text} ${view.findViewById<EditText>(R.id.editTextRequestLastName).text}", songName = "${view.findViewById<EditText>(R.id.editTextRequestSongName).text}", email = "${view.findViewById<EditText>(R.id.editTextRequestEmail).text}")
            val resTmp = CustomValidator.ValidateRequest(tmpRequest)
            if(resTmp.contains("successful")){
                val alertDialogNotConnected: AlertDialog.Builder = AlertDialog.Builder(this.context!!)
                alertDialogNotConnected.setCancelable(true)
                alertDialogNotConnected.setTitle("Confirmation")
                alertDialogNotConnected.setMessage("Send this request")
                alertDialogNotConnected.setNegativeButton("No",
                    DialogInterface.OnClickListener { dialogInterface, _ ->
                        dialogInterface.dismiss()
                    })
                alertDialogNotConnected.setPositiveButton("Yes", DialogInterface.OnClickListener { _, _->
                    val jsonObj = Klaxon().toJsonString(tmpRequest)
                    if(MainActivity.request.contains(tmpRequest)){
                        Toast.makeText(view.context,"This request has been sent already",Toast.LENGTH_LONG).show()
                    }else
                        MainActivity.socket.emit("add request from tablet", jsonObj)
                })
                alertDialogNotConnected.show()
            }else{
                Toast.makeText(view.context,resTmp,Toast.LENGTH_LONG).show()
            }
        }
    }
}