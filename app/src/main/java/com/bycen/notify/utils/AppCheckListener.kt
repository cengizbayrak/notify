package com.bycen.notify.utils

/**
 * Created by a1274544 on 07/05/17.
 */
interface AppCheckListener {
    fun setApps(packageName: String?, checkIfChecked: Boolean, appName: String?, appImage: ByteArray?)
}