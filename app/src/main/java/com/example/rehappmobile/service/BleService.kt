package com.example.rehappmobile.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class BleService : Service() {

    inner class LocalBinder : Binder() {
        val bleService = this@BleService

    }

    override fun onBind(intent: Intent?): IBinder? {
         return LocalBinder();
    }

    fun startScan(){
        Log.e("Ble Service", "Scan start")
    }

    fun stopScan(){
        Log.e("Ble Service", "Scan stop")

    }
}