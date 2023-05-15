package com.example.myapplication.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.mapview.MapView


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
    private lateinit var locationManager: LocationManager

    /**
     * Проверка на влключенный GPS
     */
    private fun hasGps(): Boolean{
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
        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        getLocation()

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
                    FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(phone)
                        .child(User::lastLatitude.name)
                        .setValue(it.latitude)
                        .addOnSuccessListener { }

                    FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(phone)
                        .child(User::lastLongitude.name)
                        .setValue(it.latitude)
                        .addOnSuccessListener { }

                }
            }
        }
    }

    /* private fun getFriends():List<User>{
         val usersRef = FirebaseDatabase.getInstance().getReference("users")

         usersRef.get().addOnSuccessListener {result->

         }
     }*/
}