package com.example.myapplication

import android.content.ClipData.Item
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract.Profile
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myapplication.maps.MapsFragment
import com.example.myapplication.profile.ProfileFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {

    //lateinit var logitem : View
    lateinit var openDrawer: FloatingActionButton
    lateinit var drawerLayout: DrawerLayout
    lateinit var navView : NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val firebaseAuth = FirebaseAuth.getInstance()
        init()
        getUserData(firebaseAuth.currentUser?.phoneNumber)
        openDrawer.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView = findViewById(R.id.nav_view)

        //Переход по пунктам меню
        navView.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.maps_item ->{
                    setFragment(MapsFragment())
                }
                R.id.friends_item ->{
                    Toast.makeText(this, "Скоро тут будут друзья", Toast.LENGTH_SHORT).show()
                }
                R.id.messages_item ->{
                    Toast.makeText(this, "Скоро тут будут сообщения", Toast.LENGTH_SHORT).show()
                }
                R.id.info_item ->{
                    Toast.makeText(this, "Скоро тут будет информация", Toast.LENGTH_SHORT).show()
                }
                R.id.log_item ->{
                    firebaseAuth.signOut()
                    val intent = Intent(this, AuthActivity::class.java)
                    startActivity(intent)
                    finish()
                }

            }
            true
        }

        //Переход на профиль
        navView.getHeaderView(0).setOnClickListener {
            setFragment(ProfileFragment())
        }

    }




    private fun getUserData(phoneNumber: String?) {
        FirebaseDatabase.getInstance().getReference("users").child(phoneNumber!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var user = snapshot.getValue(User::class.java)!!
                    navView = findViewById(R.id.nav_view)
                    val view: View = navView.getHeaderView(0)
                    val nickTv = view.findViewById<TextView>(R.id.nick_user_header)
                    val userImage = view.findViewById<CircleImageView>(R.id.image_user_header)

                    nickTv.text = user.userName
                    Glide.with(this@MainActivity).load(user.imageUser).into(userImage)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })

        //logitem.setOnClickListener { //Переход из одного окна в другое
        //  val intent = Intent(this, AuthActivity::class.java)
        //startActivity(intent)
        //finish()

    }


    private fun init() {
        //logitem = findViewById(R.id.log_item)
        openDrawer = findViewById(R.id.laucn_menu_btn)
        drawerLayout = findViewById(R.id.drawerLayout)
    }

    fun setFragment(fragment: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, fragment, null)
            .commit()
    }


}