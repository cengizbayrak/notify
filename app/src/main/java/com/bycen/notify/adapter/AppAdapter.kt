package com.bycen.notify.adapter

import android.content.Context
import android.content.pm.ApplicationInfo
import com.bycen.notify.utils.NotifyDataBaseHelper
import android.view.ViewGroup
import android.view.LayoutInflater
import com.bycen.notify.R
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.view.View
import android.widget.*
import androidx.core.graphics.drawable.toBitmap
import com.bycen.notify.activity.FirstScreenActivity
import java.io.ByteArrayOutputStream

/**
 * Created by Avinash on 01/05/17.
 */
class AppAdapter(
    private val activity: FirstScreenActivity,
    textViewResourceId: Int,
    private val list: List<ApplicationInfo>
) : ArrayAdapter<ApplicationInfo?>(activity, textViewResourceId, list) {
    private val packageManager = activity.packageManager
    private val listener = activity
    private val checkedItems = BooleanArray(list.size)
    private val alreadyCheckedItems = BooleanArray(list.size)
    private val notifyDB = NotifyDataBaseHelper(activity)
    private var checkBox: CheckBox? = null
    private var check = false

    override fun getCount(): Int {
        return list.size
    }

    override fun getItem(position: Int): ApplicationInfo {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (null == view) {
            val layoutInflater = activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE
            ) as LayoutInflater
            view = layoutInflater.inflate(R.layout.single_app_layout, null)
        }
        val item = list[position]
        val appName = view!!.findViewById<TextView>(R.id.app_name)
        val iconView = view.findViewById<ImageView>(R.id.app_icon)
        checkBox = view.findViewById(R.id.app_checkbox)
        appName.text = item.loadLabel(packageManager)
        iconView.setImageDrawable(item.loadIcon(packageManager))
        checkBox?.setOnCheckedChangeListener { compoundButton: CompoundButton, checked: Boolean ->
            if (checked) {
                checkedItems[position] = true
                listener.setApps(
                    item.packageName,
                    true,
                    item.loadLabel(packageManager) as String,
                    getBytes(item.loadIcon(packageManager))
                )
            } else {
                if (compoundButton.isPressed) {
                    checkedItems[position] = false
                    listener.setApps(
                        item.packageName,
                        false,
                        item.loadLabel(packageManager) as String,
                        getBytes(item.loadIcon(packageManager))
                    )
                    if (notifyDB.isAppSelected(item.packageName)) {
                        check = true
                    }
                    checkBox!!.setOnCheckedChangeListener(null)
                }
            }
        }
        checkBox?.isChecked = checkedItems[position]
        if (notifyDB.isAppSelected(item.packageName) && !check) {
            checkAlreadySelectedApps(item.packageName, position)
        }
        return view
    }

    private fun checkAlreadySelectedApps(packageName: String, position: Int) {
        if (notifyDB.notificationCount > 0) {
            if (notifyDB.isAppSelected(packageName)) {
                alreadyCheckedItems[position] = true
                checkBox?.isChecked = alreadyCheckedItems[position]
            }
        }
    }

    private fun getBytes(drawable: Drawable): ByteArray {
        val bitmap = when {
            drawable is BitmapDrawable -> drawable.bitmap
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                (drawable as AdaptiveIconDrawable).toBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    null
                )
            }
            else -> {
                TODO("VERSION.SDK_INT < O")
            }
        }
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }
}