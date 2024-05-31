package com.playingnia.umbrellaalarm.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.playingnia.umbrellaalarm.MainActivity
import com.playingnia.umbrellaalarm.R
import com.playingnia.umbrellaalarm.databinding.DialogHomeDistanceBinding
import com.playingnia.umbrellaalarm.managers.SettingManager

class HomeDistanceDialog(private val context: Context): Dialog(context, android.R.style.Theme_Material_Dialog_NoActionBar) {
    private val binding by lazy { DialogHomeDistanceBinding.inflate(layoutInflater) }

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setCancelable(true)
        setCanceledOnTouchOutside(true)

        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        layoutParams.dimAmount = 0.7f
        window!!.attributes = layoutParams
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        window!!.attributes.width = (context.resources.displayMetrics.widthPixels - dpToPx(36)).toInt()
        window!!.attributes.height = WindowManager.LayoutParams.WRAP_CONTENT

        binding.editTextDistance.setText(SettingManager.getHomeDistance().toString())

        binding.textSave.setOnClickListener {
            val distance = binding.editTextDistance.text.toString()
            val set = SettingManager.setHomeDistance(distance)
            if (set) {
                MainActivity.getInstance().reloadDistances()
                Toast.makeText(context, context.resources.getString(R.string.saved), Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(context, context.resources.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dpToPx(dp: Int): Float {
        return context.resources.displayMetrics.density * dp
    }
}