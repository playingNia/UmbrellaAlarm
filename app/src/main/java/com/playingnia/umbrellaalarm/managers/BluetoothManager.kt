package com.playingnia.umbrellaalarm.managers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import com.playingnia.umbrellaalarm.MainActivity
import com.playingnia.umbrellaalarm.R
import java.io.IOException
import java.util.UUID

class BluetoothManager {
    companion object {
        private val main by lazy { MainActivity.getInstance() }

        private val adapter = BluetoothAdapter.getDefaultAdapter()
        private var socket: BluetoothSocket? = null

        private val REQUEST_ENABLE_BLUETOOTH = 1
        private val HC06_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val DEVICE_NAME = "HC-06_UMBRELLA"

        private val receiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val rssi: Int = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    if (device != null && device.name == DEVICE_NAME) {
                        val distance = calDistance(rssi)
                        main.reloadDistances(distance = distance)
                    }
                }
            }
        }

        fun adapterAvailable(): Boolean {
            return adapter != null
        }

        /***
         * HC-06 디바이스 탐색
         */
        @SuppressLint("MissingPermission")
        fun selectDevice() {
            val devices: Set<BluetoothDevice>? = adapter?.bondedDevices
            if (devices != null && devices.isNotEmpty()) {
                val device = devices.first { it.name == DEVICE_NAME }
                connect(device)
            } else {
                Toast.makeText(main, main.resources.getString(R.string.device_not_found), Toast.LENGTH_SHORT).show()
            }
        }

        /***
         * @param device HC-06 디바이스
         *
         * 디바이스 연결
         */
        @SuppressLint("MissingPermission")
        private fun connect(device: BluetoothDevice) {
            try {
                socket = device.createRfcommSocketToServiceRecord(HC06_UUID)
                socket?.connect()
                adapter?.startDiscovery()
                Toast.makeText(MainActivity.getInstance(), main.resources.getString(R.string.device_connected), Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(MainActivity.getInstance(), main.resources.getString(R.string.connection_failed), Toast.LENGTH_SHORT).show()
            }
        }

        /***
         * 모듈-핸드폰 간의 거리를 계산
         *
         * @param rssi RSSI
         * @return Double 모듈-핸드폰 간의 거리
         */
        private fun calDistance(rssi: Int): Double {
            val txPower = -59 // HC-06의 기본 Tx Power 값
            return Math.pow(10.0, ((txPower - rssi) / 20.0))
        }

        /***
         * receiver 등록
         */
        fun registerReceiver() {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            MainActivity.getInstance().registerReceiver(receiver, filter)
        }

        /***
         * receiver 등록 해제
         */
        fun unregisterReceiver() {
            MainActivity.getInstance().unregisterReceiver(receiver)
            try {
                socket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}