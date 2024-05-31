package com.playingnia.umbrellaalarm

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.playingnia.umbrellaalarm.databinding.ActivityMainBinding
import com.playingnia.umbrellaalarm.dialogs.HomeDistanceDialog
import com.playingnia.umbrellaalarm.enums.STATUS
import com.playingnia.umbrellaalarm.managers.BluetoothManager
import com.playingnia.umbrellaalarm.services.GPSService
import com.playingnia.umbrellaalarm.managers.LocationManager
import com.playingnia.umbrellaalarm.managers.SettingManager

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
    private val BLUETOOTH_SCAN = android.Manifest.permission.BLUETOOTH_SCAN
    private val POST_NOTIFICATIONS = android.Manifest.permission.POST_NOTIFICATIONS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        mainActivity = this

        updateHomeDistance()

        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions()
        } else {
            startAlarmService()
        }

        binding.imageBluetooth.setOnClickListener { BluetoothManager.selectDevice() }

        if (!BluetoothManager.adapterAvailable()) {
            Toast.makeText(this, resources.getString(R.string.bluetooth_not_available), Toast.LENGTH_LONG).show()
            close()
        }

        binding.layoutHome.setOnClickListener {
            HomeDistanceDialog(this).show()
        }

        binding.textSaveLocation.setOnClickListener {
            LocationManager.saveLocation()
        }
    }

    /***
     * 필수 권한 요청
     */
    private fun requestPermissions() {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[ACCESS_FINE_LOCATION] == true || permissions[ACCESS_COARSE_LOCATION] == true || permissions[BLUETOOTH_CONNECT] == true || permissions[BLUETOOTH_SCAN] == true || permissions[POST_NOTIFICATIONS] == true) {
                startAlarmService()
            } else {
                Toast.makeText(this, resources.getString(R.string.permisson_denied), Toast.LENGTH_LONG).show()
                close()
            }
        }

        requestPermissionLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, BLUETOOTH_CONNECT, BLUETOOTH_SCAN, POST_NOTIFICATIONS))
    }

    fun updateHomeDistance() {
        LocationManager.refreshStatus()
        binding.textHomeDistance.text = "${SettingManager.getHomeDistance()}m"
    }

    /***
     * 알람 서비스 생성
     */
    private fun startAlarmService() {
        if (GPSService.isRunning) {
            return
        }

        val serviceIntent = Intent(this, GPSService::class.java)
        startForegroundService(serviceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
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