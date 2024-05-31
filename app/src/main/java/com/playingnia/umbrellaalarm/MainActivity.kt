package com.playingnia.umbrellaalarm

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.playingnia.umbrellaalarm.databinding.ActivityMainBinding
import com.playingnia.umbrellaalarm.managers.BluetoothManager
import com.playingnia.umbrellaalarm.services.AlarmService
import com.playingnia.umbrellaalarm.managers.LocationManager
import java.io.IOException
import java.util.UUID

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

        binding.imageBluetooth.setOnClickListener { BluetoothManager.selectDevice() }

        if (!BluetoothManager.adapterAvailable()) {
            Toast.makeText(this, resources.getString(R.string.bluetooth_not_available), Toast.LENGTH_LONG).show()
            close()
        }

        BluetoothManager.registerReceiver()

        binding.textSaveLocation.setOnClickListener {
            LocationManager.saveLocation()
            reloadDistances()
        }
    }

    /***
     * 필수 권한 요청
     */
    private fun requestPermissions() {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[ACCESS_FINE_LOCATION] == true || permissions[ACCESS_COARSE_LOCATION] == true || permissions[BLUETOOTH_CONNECT] == true) {
                startAlarmService()
            } else {
                Toast.makeText(this, resources.getString(R.string.permisson_denied), Toast.LENGTH_LONG).show()
                close()
            }
        }

        requestPermissionLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH_CONNECT))
    }

    /***
     * 알람 서비스 생성
     */
    private fun startAlarmService() {
        if (AlarmService.isRunning) {
            return
        }

        val serviceIntent = Intent(this, AlarmService::class.java)
        startForegroundService(serviceIntent)
    }

    /***
     * 거리 정보 리프레쉬
     */
    fun reloadDistances(currentLoc: Location? = LocationManager.getCurrentLocation(), distance: Double = 0.0) {
        val distFromHome = LocationManager.getDistanceFromHome(currentLoc)
        if (distFromHome != null) {
            binding.textHomeDistance.text = "${distFromHome.toInt()}m"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        BluetoothManager.unregisterReceiver()
    }

    /***
     * 어플리케이션 종료
     */
    private fun close() {
        moveTaskToBack(true)
        finish()
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}