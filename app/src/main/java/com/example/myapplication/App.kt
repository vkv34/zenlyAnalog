package com.example.myapplication

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class App: Application() {

    override fun onCreate() {
        super.onCreate()

        MapKitFactory.setApiKey("b560b922-c032-4658-a7c4-0e72c3eefcb3")
        MapKitFactory.initialize(this)
    }

}