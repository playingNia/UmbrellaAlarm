package com.playingnia.umbrellaalarm.managers

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.playingnia.umbrellaalarm.MainActivity
import com.playingnia.umbrellaalarm.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LocationManager {
    companion object {

        private val context by lazy { MainActivity.getInstance() }

        fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
            putLong(key, java.lang.Double.doubleToLongBits(double))

        fun SharedPreferences.Editor.putLocation(loc: Location) {
            putDouble("latitude", loc.latitude)
            putDouble("longitude", loc.longitude)
            this.apply()
        }

        fun SharedPreferences.getDouble(key: String, default: Double) =
            java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))

        fun SharedPreferences.getLocation(): Location {
            val loc = Location("provider")
            loc.latitude = getDouble("latitude", 0.0)
            loc.longitude = getDouble("longitude", 0.0)
            return loc
        }

        /***
         * 현재 위치 반환
         *
         * @return Location? 현재 위치
         */
        @SuppressLint("MissingPermission")
        fun getCurrentLocation(): Location? {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = locationManager.getProviders(true)
            var bestLoc: Location? = null
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLoc == null || loc.accuracy < bestLoc.accuracy) {
                    bestLoc = loc
                }
            }
            return bestLoc
        }

        /***
         * 집 위치 반환
         *
         * @return Location 집의 위치
         */
        private fun getHomeLocation(): Location {
            val sharedPreferences = context.getSharedPreferences("Location", AppCompatActivity.MODE_PRIVATE)
            return sharedPreferences.getLocation()
        }

        /***
         * 집 위치 저장
         */
        @SuppressLint("MissingPermission")
        fun saveLocation() {
            val sharedPreferences = context.getSharedPreferences("Location", AppCompatActivity.MODE_PRIVATE)
            val loc = getCurrentLocation()
            sharedPreferences.edit().putLocation(loc!!)
            Toast.makeText(context, context.resources.getString(R.string.success_to_save), Toast.LENGTH_SHORT).show()
        }

        /***
         * 두 위치 간의 거리 반환
         *
         * @param loc1 위치 1
         * @param loc2 위치 2
         * @return Double 두 위치 간의 거리
         */
        private fun getDistance(loc1: Location, loc2: Location): Double {
            val EARTH_R = 6371.0

            val lati1 = Math.toRadians(loc1.latitude)
            val long1 = Math.toRadians(loc1.longitude)
            val lati2 = Math.toRadians(loc2.latitude)
            val long2 = Math.toRadians(loc2.longitude)

            val distLati = lati2 - lati1
            val distLong = long2 - long1

            val a = sin(distLati / 2).pow(2) + cos(lati1) * cos(lati2) * sin(distLong / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return EARTH_R * c * 1000
        }

        /***
         * 집과 현재 위치 간의 거리
         *
         * @param currentLoc 현재 위치
         * @return Double? 집과 현재 위치 간의 거리
         */
        @SuppressLint("MissingPermission")
        fun getDistanceFromHome(currentLoc: Location?): Double? {
            if (currentLoc == null) {
                return null
            }

            val sharedPreferences = context.getSharedPreferences("Location", AppCompatActivity.MODE_PRIVATE)
            if (!sharedPreferences.contains("latitude") || !sharedPreferences.contains("longitude")) {
                return null
            }

            val loc1 = getHomeLocation()
            val dist = getDistance(loc1, currentLoc)
            return dist
        }

        /***
         * GPS 업데이트, 이벤트 등록
         */
        @SuppressLint("MissingPermission")
        fun registerLocationEvent() {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1f, object: LocationListener {
                override fun onLocationChanged(location: Location) {
                    MainActivity.getInstance().reloadDistances(location)
                }
            })
        }
    }
}