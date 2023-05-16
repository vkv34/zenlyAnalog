package com.example.myapplication.services

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

private const val TAG = "LocationService"

class LocationService : Service() {

    /**
     * Интервал проверки геопозиции в фоне
     */
    private val interval = TimeUnit.SECONDS.toMillis(10)

    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val NOTIFICATION_ID = 123

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    /**
     * Создание канала уведомлений
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Создание уведомления
     */
    private fun buildNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Service")
            .setContentText("Running in the background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
        // Другие настройки уведомления

        return notificationBuilder.build()
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Проверка разрешений
            return START_NOT_STICKY
        }

        //добавление слушателя на геопозицию
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
        //создание канала с уведомлением
        createNotificationChannel()
        val notification: Notification = buildNotification()
        //проверка на возможность работы в фоне и запуск
        if (checkBackgroundLocationPermission() && checkForgeGroundServicePermission()) {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    /**
     * Проверка разрешения на получение геопозиции в фоне
     */
    private fun checkBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ContextCompat.checkSelfPermission(
                this,
                backgroundLocationPermission
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Для версий Android ниже 10 (API 29) разрешение не требуется
            true
        }
    }

    /**
     * Проверка разрешения на работу в фоне
     */
    private fun checkForgeGroundServicePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
        else true
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Создание запроса на местоположение
     */
    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            interval
        ).build()
    }

    /**
     * Слушатель местоположения
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                // Обработка новых координат
                val latitude = location.latitude
                val longitude = location.longitude

                Log.i(TAG, "onLocationResult: $locationResult")
                val phone = Firebase.auth.currentUser?.phoneNumber
                if (phone != null) {

                    FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(phone)
                        .apply {
                            child(User::lastLatitude.name)
                                .setValue(latitude)
                            child(User::lastLongitude.name)
                                .setValue(longitude)
                        }
                }
            }
        }
    }


}