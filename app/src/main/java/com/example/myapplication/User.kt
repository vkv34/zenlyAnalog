package com.example.myapplication

import com.yandex.mapkit.geometry.Point
import java.time.LocalDate

data class User(
    val uuid: String = "",
    val phoneNumber: String = "",
    val userName: String = "",
    val dob: String = "",
    val imageUser: String = "",
    val lastLongitude: Double = Double.NaN,
    val lastLatitude: Double = Double.NaN,
    val lastLocationUpdate: LocalDate = LocalDate.now(),
)