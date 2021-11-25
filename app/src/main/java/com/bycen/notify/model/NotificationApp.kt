package com.bycen.notify.model

import android.content.Context
import java.io.Serializable

/**
 * Created by a1274544 on 04/06/17.
 */
class NotificationApp(
    var name: String? = null,
    var image: ByteArray? = null,
    var packageName: String? = null
) : Serializable {
    var notificationCount = 0

    fun openApp(context: Context) {
        packageName?.let { packageName ->
            val packageManager = context.packageManager
            packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                context.startActivity(intent)
            }
        }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is NotificationApp) {
            return false
        }
        return name == other.name &&
                packageName == other.packageName &&
                image.contentEquals(other.image)
    }

    companion object {
        const val KEY_SERIALIZABLE = "key_notification_app"
    }
}