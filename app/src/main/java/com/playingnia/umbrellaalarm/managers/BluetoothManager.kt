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
import com.playingnia.umbrellaalarm.services.AlarmService
import com.playingnia.umbrellaalarm.services.GPSService
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothManager {
    companion object {
        private val main by lazy { MainActivity.getInstance() }

        var isConnected = false

        private val adapter = BluetoothAdapter.getDefaultAdapter()
        var socket: BluetoothSocket? = null
        var inputStream: InputStream? = null
        private var device: BluetoothDevice? = null
        private val HC06_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val DEVICE_NAME = "HC-06_UMBRELLA"

        /***
         * Get is adapter available.
         *
         * @return If adapter is available
         */
        fun adapterAvailable(): Boolean {
            return adapter != null
        }

        /***
         * Find HC-06 device.
         *
         * @return If available device is exist
         */
        @SuppressLint("MissingPermission")
        fun findDevice(): Boolean {
            if (isConnected) {
                return false
            }

            val devices: Set<BluetoothDevice>? = adapter?.bondedDevices
            if (devices == null || devices.isEmpty()) {
                Toast.makeText(main, main.resources.getString(R.string.device_not_found), Toast.LENGTH_SHORT).show()
                return false
            }

            device = devices.first { it.name == DEVICE_NAME }
            return true
        }

        /***
         * Connect to device.
         */
        @SuppressLint("MissingPermission")
        fun connect() {
            try {
                socket = device!!.createRfcommSocketToServiceRecord(HC06_UUID)
                socket?.connect()
                adapter?.startDiscovery()
                startAlarmService()
                Toast.makeText(main, main.resources.getString(R.string.device_connected), Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(main, main.resources.getString(R.string.connection_failed), Toast.LENGTH_SHORT).show()
            }
        }

        /***
         * Start Alarm Service.
         */
        @SuppressLint("MissingPermission")
        fun startAlarmService() {
            if (AlarmService.isRunning) {
                return
            }

            val serviceIntent = Intent(main, AlarmService::class.java)
            main.startForegroundService(serviceIntent)
        }

        /***
         * Notify user to don't forget the umbrella.
         */
        fun disconnected() {
            isConnected = false
            AlarmService.sendAlarm()
        }
    }
}