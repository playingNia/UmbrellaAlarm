package com.playingnia.umbrellaalarm.managers

import androidx.appcompat.app.AppCompatActivity
import com.playingnia.umbrellaalarm.MainActivity

class SettingManager {
    companion object {
        private val main by lazy { MainActivity.getInstance() }

        fun getHomeDistance(): Int {
            val sharedPreferences = main.getSharedPreferences("Setting", AppCompatActivity.MODE_PRIVATE)
            return sharedPreferences.getInt("home", 50)
        }

        fun setHomeDistance(distance: String): Boolean {
            if (distance == "" || distance.toInt() <= 0) {
                return false
            }

            val sharedPreferences = main.getSharedPreferences("Setting", AppCompatActivity.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putInt("home", distance.toInt())
            editor.apply()
            return true
        }
    }
}