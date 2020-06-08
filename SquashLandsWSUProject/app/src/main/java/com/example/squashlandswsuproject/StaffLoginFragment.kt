package com.example.squashlandswsuproject

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class StaffLoginFragment : Fragment(R.layout.fragment_staff_login){
    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val buttonBack = activity?.findViewById<Button>(R.id.buttonBack)
        buttonBack?.visibility = Button.VISIBLE
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val buttonStaffLogin = view.findViewById<Button>(R.id.buttonStaffLogin)
        val editTextStaffLogin = view.findViewById<EditText>(R.id.editTextStaffLoginPin)
        if(MainActivity.pinNumber.contentEquals("None")){
            buttonStaffLogin.visibility = Button.INVISIBLE
            editTextStaffLogin.hint = "No PIN Generated Yet"
            editTextStaffLogin.focusable = EditText.NOT_FOCUSABLE
        }

        buttonStaffLogin.setOnClickListener {
            if(editTextStaffLogin.text.toString().contentEquals(MainActivity.pinNumber)){
                val fTransaction = this.activity?.supportFragmentManager?.beginTransaction()
                fTransaction?.replace(R.id.fragment_holder, SettingFragment(), "fragment_settings")
                fTransaction?.commit()
            }else{
                Toast.makeText(this.context, "not matched", Toast.LENGTH_LONG).show()
            }
        }
    }
}