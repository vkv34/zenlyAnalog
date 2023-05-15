package com.example.myapplication.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


/**
 * Фрагмент с картой
 */
class MapsFragment : Fragment() {


    /**
     * регистрация запроса на получение геопозиции
     */
    private val permissionActivityResultLauncher = registerForActivityResult(RequestPermission()) {
        if (! it) {
            Toast.makeText(
                requireContext(),
                "Необходимо предоставить доступ к местоположению",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val mapKitFactory = MapKitFactory.getInstance()
    private lateinit var mapObjects: MapObjectCollection
    private var userObject: PlacemarkMapObject? = null

    data class UserWithPlaceMark(
        var user: User,
        val placemarkMapObject: PlacemarkMapObject,
    )

    private var friendsMapObjectCollection: MutableList<UserWithPlaceMark> = mutableListOf()


    private lateinit var locationManager: LocationManager

    /**
     * Проверка на влключенный GPS
     */
    private fun hasGps(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Проверка на включенную геопозицию через интернет
     */
    private fun hasNetwork(): Boolean =
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    private lateinit var yandexMaps: MapView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_maps, container, false)
        yandexMaps = view.findViewById(R.id.yandexMapView)
        mapObjects = yandexMaps.map.mapObjects
        getLocation()
        getFriends()
        return view;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (! checkPermissions()) {

            /**
             * запрос разрешения на местоположение
             */

            permissionActivityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        locationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager


    }

    override fun onStart() {
        super.onStart()

        mapKitFactory.onStart()
        yandexMaps.onStart()
    }

    override fun onStop() {
        super.onStop()

        mapKitFactory.onStop()
        yandexMaps.onStop()
    }

    /**
     * Проверка наличия разрешения на местоположение
     */
    private fun checkPermissions(): Boolean {
        return (ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (! checkPermissions())
            return
        val phone = Firebase.auth.currentUser?.phoneNumber

        if (hasGps()) {
            if (! checkPermissions())
                return
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                0f
            ) {
                if (phone != null) {

                    if (userObject == null) {
                        FirebaseDatabase.getInstance().getReference("users")
                            .child(phone)
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val user = snapshot.getValue(User::class.java)
                                if (user != null) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        getBitmapFromURL(user.imageUser)?.let { bitmap ->
                                            withContext(Dispatchers.Main) {
                                                userObject = mapObjects.addPlacemark(
                                                    Point(user.lastLatitude, user.lastLongitude),
                                                    ImageProvider.fromBitmap(getCroppedBitmap(bitmap))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                    } else {
                        userObject !!.geometry = Point(it.latitude, it.longitude)
                    }

                    FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(phone)
                        .apply {
                            child(User::lastLatitude.name)
                                .setValue(it.latitude)
                            child(User::lastLongitude.name)
                                .setValue(it.longitude)
                        }

                }
            }
        }
    }

    fun getBitmapFromURL(src: String?): Bitmap? {
        return try {
            val url = URL(src)
            val connection =
                url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val decodedBitmap = BitmapFactory.decodeStream(input)
            Bitmap.createScaledBitmap(decodedBitmap, 120, 120, false)
        } catch (e: IOException) {
            // Log exception
            null
        }
    }

    fun getCroppedBitmap(bitmap: Bitmap): Bitmap? {
        val output = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(output)
        val color = - 0xbdbdbe
        val paint = Paint()
        val rect = Rect(0, 0, bitmap.width, bitmap.height)
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(
            (bitmap.width / 2).toFloat(), (bitmap.height / 2).toFloat(),
            (
                    bitmap.width / 2).toFloat(), paint
        )
        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        canvas.drawBitmap(bitmap, rect, rect, paint)
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output
    }

    private fun getFriends() {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")
            .addValueEventListener(
                object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {

                        val users = mutableListOf<User>()
                        for (postSnapshot in snapshot.children) {
                            postSnapshot.getValue<User>()?.let { users.add(it) }
                        }

                        val usersOnMapList = users.filter {
                            var contains = false
                            friendsMapObjectCollection.forEach { friendList ->
                                if (friendList.user.uuid == it.uuid) {
                                    contains = true
                                    return@forEach
                                }
                            }
                            contains
                        }

                        val addedUsers = users.filter { user->
                            var contains = false
                            usersOnMapList.forEach {userOnMap->
                                if(userOnMap.uuid == user.uuid){
                                    contains = true
                                    return@forEach
                                }
                            }
                            !contains
                        }
                        val removedUsers = friendsMapObjectCollection.filter {
                            var contains = false
                            usersOnMapList.forEach { userOnMap ->
                                if (userOnMap.uuid == it.user.uuid) {
                                    contains = true
                                    return@forEach
                                }
                            }
                            !contains
                        }

                        removedUsers.forEach {
                            mapObjects.remove(it.placemarkMapObject)
                        }

                        addedUsers.forEach { user ->
                            CoroutineScope(Dispatchers.IO).launch {
                                getBitmapFromURL(user.imageUser)?.let { bitmap ->
                                    withContext(Dispatchers.Main) {
                                        friendsMapObjectCollection.add(
                                            UserWithPlaceMark(
                                                user = user,
                                                placemarkMapObject = mapObjects.addPlacemark(
                                                    Point(
                                                        user.lastLatitude,
                                                        user.lastLongitude
                                                    ),
                                                    ImageProvider.fromBitmap(
                                                        getCroppedBitmap(
                                                            bitmap
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        friendsMapObjectCollection.forEach {
                            it.user = users.first {
                                p->p.uuid == it.user.uuid
                            }
                        }

                        friendsMapObjectCollection.forEach {
                            //mapObjects.remove(it.placemarkMapObject)
//                            mapObjects.addPlacemark(Point(it.user.lastLatitude, it.user.lastLongitude))
                            it.placemarkMapObject.geometry =
                                Point(it.user.lastLatitude, it.user.lastLongitude)
                        }

                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                }
            )
    }
}