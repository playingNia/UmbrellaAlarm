package com.playingnia.umbrellaalarm.utils

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.location.Location
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.playingnia.umbrellaalarm.MainActivity
import com.playingnia.umbrellaalarm.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class LocationManager {
    companion object {
        fun SharedPreferences.Editor.putDouble(key: String, double: Double) =
            putLong(key, java.lang.Double.doubleToLongBits(double))

        fun SharedPreferences.getDouble(key: String, default: Double) =
            java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(default)))

        @SuppressLint("MissingPermission")
        fun saveLocation(textView: TextView) {
            val context = MainActivity.getInstance()

            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

            fusedLocationProviderClient.lastLocation.addOnSuccessListener { success: Location? ->
                success?.let { location ->
                    textView.text = "${location.latitude}, ${location.longitude}"
                    val sharedPreferences = context.getSharedPreferences("Location",
                        AppCompatActivity.MODE_PRIVATE
                    )

                    if (sharedPreferences != null) {
                        val lati = sharedPreferences.getDouble("latitude", 0.0)
                        val long = sharedPreferences.getDouble( "longitude", 0.0)
                        textView.text = "${lati}, ${long}\n${location.latitude}, ${location.longitude}\n${getDistance(lati, long, location)}m"
                    }

                    val editor = sharedPreferences.edit()
                    editor.putDouble("latitude", location.latitude)
                    editor.putDouble("longitude", location.longitude)
                    editor.apply()

                    Toast.makeText(context, context.resources.getString(R.string.success_to_save), Toast.LENGTH_SHORT).show()
                }}
                .addOnFailureListener { fail ->
                    Toast.makeText(context, fail.localizedMessage, Toast.LENGTH_SHORT).show()
                }
        }

        fun getDistance(lati: Double, long: Double, loc2: Location): Double {
            val EARTH_R = 6371000.0

            val lati1 = Math.toRadians(lati)
            val long1 = Math.toRadians(long)
            val lati2 = Math.toRadians(loc2.latitude)
            val long2 = Math.toRadians(loc2.longitude)

            val distLati = lati2 - lati1
            val distLong = long2 - long1

            val a = sin(distLati / 2).pow(2) + cos(lati1) * cos(lati2) * sin(distLong / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return EARTH_R * c
        }
    }
}