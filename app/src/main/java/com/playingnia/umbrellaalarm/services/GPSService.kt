package com.playingnia.umbrellaalarm.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.playingnia.umbrellaalarm.R
import com.playingnia.umbrellaalarm.managers.LocationManager

class GPSService() : Service() {

    companion object {
        var isRunning = false
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private lateinit var thread: Thread

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true

        thread = Thread(Runnable {
            run {
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    LocationManager.registerLocationEvent()
                }
            }
        })
        thread.start()

        val CHANNEL_ID = "GPS Service ID"
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, CHANNEL_ID).setContentText("").setContentTitle("GPS Service").setSmallIcon(R.drawable.ic_launcher_background)

        startForeground(1, notification.build())

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isRunning = false
        thread.interrupt()
        super.onDestroy()
    }
}