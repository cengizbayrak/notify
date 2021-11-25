package com.bycen.notify.model

import android.app.PendingIntent
import android.content.Context
import android.os.Parcelable
import java.io.Serializable

/**
 * notification data
 *
 * @property title notification title
 * @property message notification message
 * @property packageName notification app package name
 * @property timestamp notification timestamp
 * @constructor
 *
 * @param id sqlite db id
 */
class Notification(
    id: Int? = null,
    var title: String? = null,
    var message: String? = null,
    var packageName: String? = null,
    var timestamp: String? = null
) : Serializable {
    var id = id ?: 0

    fun openApp(context: Context) {
        packageName?.let { packageName ->
            val packageManager = context.packageManager
            packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                context.startActivity(intent)
            }
        }
    }

    companion object {
        const val KEY_SERIALIZABLE = "key_notification"
        const val KEY_ACTION = "key_notification_action"
    }
}