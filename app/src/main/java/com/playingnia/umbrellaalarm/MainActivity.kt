package com.playingnia.umbrellaalarm

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.playingnia.umbrellaalarm.databinding.ActivityMainBinding
import com.playingnia.umbrellaalarm.services.AlarmService
import com.playingnia.umbrellaalarm.utils.LocationManager
import java.io.IOException
import java.util.UUID
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    companion object {
        private lateinit var mainActivity: MainActivity

        fun getInstance(): MainActivity {
            return mainActivity
        }
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val ACCESS_FINE_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
    private val ACCESS_COARSE_LOCATION = android.Manifest.permission.ACCESS_COARSE_LOCATION
    private val BLUETOOTH_CONNECT = android.Manifest.permission.BLUETOOTH_CONNECT

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null

    private val REQUEST_ENABLE_BLUETOOTH = 1
    private val HC06_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val rssi: Int = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                if (device != null && device.name == "HC-06") {
                    val distance = calculateDistance(rssi)
                    binding.textDistanceDistance.text = "${distance}m"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        mainActivity = this

        val currentLoc = LocationManager.getCurrentLocation()
        if (currentLoc != null) {
            reloadDistances(currentLoc)
        }

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions()
        } else {
            startAlarmService()
        }

        binding.imageBluetooth.setOnClickListener { connectToDevice() }

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            finish()
        }

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        binding.textSaveLocation.setOnClickListener {
            LocationManager.saveLocation()
            reloadDistances()
        }
    }

    private fun requestPermissions() {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[ACCESS_FINE_LOCATION] == true || permissions[ACCESS_COARSE_LOCATION] == true || permissions[BLUETOOTH_CONNECT] == true) {
                startAlarmService()
            } else {
                Toast.makeText(this, resources.getString(R.string.permisson_denied), Toast.LENGTH_LONG).show()
                moveTaskToBack(true)
                finish()
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }

        requestPermissionLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
    }

    private fun startAlarmService() {
        if (AlarmService.isRunning) {
            return
        }

        val serviceIntent = Intent(this, AlarmService::class.java)
        startForegroundService(serviceIntent)
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        if (pairedDevices != null && pairedDevices.isNotEmpty()) {
            val device = pairedDevices.first { it.name == "HC-06" }
            connect(device)
        } else {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: BluetoothDevice) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(HC06_UUID)
            bluetoothSocket?.connect()
            bluetoothAdapter?.startDiscovery()
            Toast.makeText(this, device.name, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateDistance(rssi: Int): Double {
        val txPower = -59 // HC-06의 기본 Tx Power 값
        return Math.pow(10.0, ((txPower - rssi) / 20.0))
    }

    fun reloadDistances(currentLoc: Location? = LocationManager.getCurrentLocation()) {
        val distFromHome = LocationManager.getDistanceFromHome(currentLoc)
        if (distFromHome != null) {
            binding.textHomeDistance.text = "${distFromHome.toInt()}m"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}