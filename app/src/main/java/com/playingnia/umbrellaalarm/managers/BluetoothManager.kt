package com.playingnia.umbrellaalarm.managers

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.playingnia.umbrellaalarm.MainActivity
import com.playingnia.umbrellaalarm.R
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothManager {
    companion object {
        private val main by lazy { MainActivity.getInstance() }

        private val adapter = BluetoothAdapter.getDefaultAdapter()
        private var socket: BluetoothSocket? = null
        private var inputStream: InputStream? = null
        private lateinit var device: BluetoothDevice
        private var isConnected = false
        private val HC06_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val DEVICE_NAME = "HC-06_UMBRELLA"

        private lateinit var thread: Thread
        private var isThreadRunning = false

        fun adapterAvailable(): Boolean {
            return adapter != null
        }

        /***
         * HC-06 디바이스 탐색
         */
        @SuppressLint("MissingPermission")
        fun selectDevice() {
            if (isConnected) {
                return
            }

            val devices: Set<BluetoothDevice>? = adapter?.bondedDevices
            if (devices != null && devices.isNotEmpty()) {
                device = devices.first { it.name == DEVICE_NAME }
                connect()
            } else {
                Toast.makeText(main, main.resources.getString(R.string.device_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        /***
         * 디바이스 연결
         */
        @SuppressLint("MissingPermission")
        private fun connect() {
            try {
                socket = device.createRfcommSocketToServiceRecord(HC06_UUID)
                socket?.connect()
                adapter?.startDiscovery()
                startThread()
                Toast.makeText(MainActivity.getInstance(), main.resources.getString(R.string.device_connected), Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(MainActivity.getInstance(), main.resources.getString(R.string.connection_failed), Toast.LENGTH_SHORT).show()
            }
        }

        /***
         * receiver 등록
         */
        @SuppressLint("MissingPermission")
        fun startThread() {
            if (isThreadRunning) {
                return
            }

            inputStream = socket?.inputStream
            thread = Thread {
                while (true) {
                    Thread.sleep(5000)

                    if (socket == null || socket?.isConnected == false) {
                        disconnected()
                    } else {
                        try {
                            val inputStream = socket?.inputStream
                            val buffer = ByteArray(1024)
                            val bytes = inputStream?.read(buffer) ?: -1

                            if (!isConnected && bytes > 0) {
                                isConnected = true
//                                handler.post {
//                                    Toast.makeText(main, "연결 됨", Toast.LENGTH_SHORT).show()
//                                }
                            } else if(isConnected && bytes <= 0) {
                                disconnected()
                            }

                        } catch (e: IOException) {
                            e.printStackTrace()
                            if (isConnected) {
                                disconnected()
                            }
                        }
                    }
                }
            }
            thread.start()
            isThreadRunning = true
        }

        /***
         * receiver 등록 해제
         */
        fun interruptThread() {
            isThreadRunning = false
            thread.interrupt()

            try {
                inputStream?.close()
                socket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun disconnected() {
            isConnected = false
            notify("우산 알림이", "우산을 두고 간 것으로 예상됩니다! 우산을 꼭 챙겨가세요!")
        }

        @SuppressLint("MissingPermission")
        private fun notify(title: String, text: String) {
            val CHANNEL_ID = "Bluetooth Manager ID"
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager = main.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val builder = NotificationCompat.Builder(main, CHANNEL_ID).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title).setContentText(text).setPriority(NotificationCompat.PRIORITY_HIGH)
            with(NotificationManagerCompat.from(main)) {
                notify(1, builder.build())
            }
        }
    }
}