package com.playingnia.umbrellaalarm.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.playingnia.umbrellaalarm.MainActivity
import com.playingnia.umbrellaalarm.R
import com.playingnia.umbrellaalarm.enums.STATUS
import com.playingnia.umbrellaalarm.managers.BluetoothManager
import com.playingnia.umbrellaalarm.managers.LocationManager
import java.io.IOException

class AlarmService() : Service() {

    companion object {
        var isRunning = false

        @SuppressLint("MissingPermission")
        fun sendAlarm() {
            if (LocationManager.status == STATUS.INSIDE) {
                return
            }

            val main = MainActivity.getInstance()
            val CHANNEL_ID = "Bluetooth Manager ID"
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager = main.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val builder = NotificationCompat.Builder(main, CHANNEL_ID).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(main.resources.getString(R.string.app_name)).setContentText(main.resources.getString(R.string.alarm_message)).setPriority(NotificationCompat.PRIORITY_HIGH)
            with(NotificationManagerCompat.from(main)) {
                notify(3, builder.build())
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private lateinit var thread: Thread

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true

        thread = Thread {
            while (true) {
                Thread.sleep(5000)

                if (BluetoothManager.isConnected && (BluetoothManager.socket == null || BluetoothManager.socket?.isConnected == false)) {
                    BluetoothManager.disconnected()
                    continue
                }

                try {
                    BluetoothManager.inputStream = BluetoothManager.socket?.inputStream
                    val buffer = ByteArray(1024)
                    val bytes = BluetoothManager.inputStream?.read(buffer) ?: -1

                    if (BluetoothManager.isConnected && bytes <= 0) {
                        BluetoothManager.disconnected()
                        continue
                    }

                    if (!BluetoothManager.isConnected && bytes > 0) {
                        BluetoothManager.isConnected = true
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    if (BluetoothManager.isConnected) {
                        BluetoothManager.disconnected()
                    }
                }
            }
        }
        thread.start()

        val CHANNEL_ID = "Alarm Service ID"
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, CHANNEL_ID).setContentText("").setContentTitle("Alarm Service").setSmallIcon(R.drawable.ic_launcher_background)

        startForeground(2, notification.build())

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        isRunning = false
        thread.interrupt()

        try {
            BluetoothManager.inputStream?.close()
            BluetoothManager.socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}