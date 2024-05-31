package com.playingnia.umbrellaalarm.managers

import androidx.appcompat.app.AppCompatActivity
import com.playingnia.umbrellaalarm.MainActivity

class SettingManager {
    companion object {
        private val main by lazy { MainActivity.getInstance() }

        /***
         * Get the standard distance between outside and inside.
         *
         * @return Standard distance between outside and inside
         */
        fun getHomeDistance(): Int {
            val sharedPreferences = main.getSharedPreferences("Setting", AppCompatActivity.MODE_PRIVATE)
            return sharedPreferences.getInt("home", 50)
        }

        /***
         * Set the standard distance between outside and inside.
         *
         * @param distance standard distance between outside and inside
         */
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