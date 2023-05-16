package com.example.myapplication.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import de.hdodenhof.circleimageview.CircleImageView


class ProfileFragment : Fragment() {

    lateinit var imageUser : CircleImageView
    lateinit var nickname : EditText
    lateinit var phone : TextView
    lateinit var dob : TextView
    lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view =  inflater.inflate(R.layout.fragment_profile, container, false)
        imageUser = view.findViewById(R.id.image_user_prof)
        nickname = view.findViewById(R.id.nickname_input_profile)
        phone = view.findViewById(R.id.phone_out)
        dob = view.findViewById(R.id.dob_out)
        saveButton = view.findViewById(R.id.save_btn)
        saveButton.setOnClickListener{
            update()
        }
        val firebaseAuth = FirebaseAuth.getInstance()
        getUserData(firebaseAuth.currentUser?.phoneNumber)
        return view
    }

    //Выгрузка данных из Firebase
    private fun getUserData(phoneNumber: String?) {
        FirebaseDatabase.getInstance().getReference("users").child(phoneNumber!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var user = snapshot.getValue(User::class.java)!!
                    nickname.setText(user.userName)
                    phone.text = user.phoneNumber
                    dob.text = user.dob
                    Glide.with(context!!).load(user.imageUser).into(imageUser)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })



    }

    //Обновление пользователя
    fun update(){
        val phone = Firebase.auth.currentUser?.phoneNumber
        if (phone != null) {

            FirebaseDatabase.getInstance()
                .getReference("users")
                .child(phone)
                .apply {
                    child(User::userName.name)
                        .setValue(nickname.text.toString())
                    /*child(User::phoneNumber.name)
                        .setValue(phone)
                    child(User::dob.name)
                        .setValue(dob.text)*/
                }
        }

    }

}