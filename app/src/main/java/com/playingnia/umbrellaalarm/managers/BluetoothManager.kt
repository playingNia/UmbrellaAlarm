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

        private val adapter = BluetoothAdapter.getDefaultAdapter()
        var socket: BluetoothSocket? = null
        var inputStream: InputStream? = null
        private lateinit var device: BluetoothDevice
        var isConnected = false
        private val HC06_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val DEVICE_NAME = "HC-06_UMBRELLA"

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
                startAlarmService()
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
        fun startAlarmService() {
            if (AlarmService.isRunning) {
                return
            }

            val serviceIntent = Intent(main, AlarmService::class.java)
            main.startForegroundService(serviceIntent)
        }

        fun disconnected() {
            isConnected = false
            AlarmService.notify("우산 알림이", "우산을 두고 간 것으로 예상됩니다! 우산을 꼭 챙겨가세요!")
        }
    }
}