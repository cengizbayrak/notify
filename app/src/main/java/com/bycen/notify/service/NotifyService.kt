package com.bycen.notify.service

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Parcel
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bycen.notify.model.Notification
import com.bycen.notify.utils.*
import com.bycen.notify.utils.extensions.TAG
import kotlin.reflect.full.memberProperties
import android.os.IBinder

import android.os.IInterface

import android.app.PendingIntent
import android.os.RemoteException
import java.lang.ClassCastException
import java.lang.IllegalStateException
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method


/**
 * notification listener service
 *
 */
class NotifyService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras

//        if (sbn.isClearable) {
        val contentIntent = sbn.notification.contentIntent
        contentIntent?.let {
            Log.d(TAG, "onNotificationPosted: content intent")
//            try {
//                it::class.memberProperties.forEach {
//                    Log.d(TAG, "onNotificationPosted: ${it.name}")
//                    Log.d(TAG, "onNotificationPosted: ${it.getter.call(contentIntent)}")
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e(TAG, "onNotificationPosted: reflection exception: ", e)
//            }
//            try {
//                val intent = getIntent(it)
//                Log.d(TAG, "onNotificationPosted: intent: ${intent?.action}")
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e(TAG, "onNotificationPosted: intent exception: ", e)
//            }
        }
//        }
//        val contentParcel = Parcel.obtain()
//        contentParcel.writeValue(contentIntent)
//        val contentBytes = contentParcel.marshall()
//        contentParcel.recycle()

        val packageName = sbn.packageName
        val title = extras.getString(KEY_ANDROID_TITLE)
        val text = extras.getCharSequence(KEY_ANDROID_TEXT).toString()
        val timeStamp = sbn.postTime
//        val timeStamp = System.currentTimeMillis()
        if (packageName != KEY_ANDROID) {
            val notification = Notification(
                title = title,
                message = text,
                packageName = packageName,
                timestamp = timeStamp.toString()
            )
            val intent = Intent(Notification.KEY_ACTION).apply {
                putExtra(Notification.KEY_SERIALIZABLE, notification)
            }
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }

    /**
     * Return the Intent for PendingIntent.
     * Return null in case of some (impossible) errors: see Android source.
     * @throws IllegalStateException in case of something goes wrong.
     * See [Throwable.getCause] for more details.
     */
//    @Throws(IllegalStateException::class)
//    fun getIntent(pendingIntent: PendingIntent?): Intent? {
//        return try {
//            val getIntent = PendingIntent::class.java.getDeclaredMethod("getIntent")
//            getIntent.invoke(pendingIntent) as Intent
//        } catch (e: NoSuchMethodException) {
//            throw IllegalStateException(e)
//        } catch (e: InvocationTargetException) {
//            throw IllegalStateException(e)
//        } catch (e: IllegalAccessException) {
//            throw IllegalStateException(e)
//        }
//    }

    /**
     * Return the Intent for PendingIntent.
     * Return null in case of some (impossible) errors: see Android source.
     * @throws IllegalStateException in case of something goes wrong.
     * See [Throwable.getCause] and [Throwable.getMessage] for more details.
     */
    @Throws(IllegalStateException::class)
    fun getIntent(pendingIntent: PendingIntent): Intent? {
        return try {
            val getIntent: Method = PendingIntent::class.java.getDeclaredMethod("getIntent")
            getIntent.invoke(pendingIntent) as Intent
        } catch (e: NoSuchMethodException) {
            getIntentDeep(pendingIntent)
        } catch (e: InvocationTargetException) {
            throw IllegalStateException(e)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(e)
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    @Throws(IllegalStateException::class)
    private fun getIntentDeep(pendingIntent: PendingIntent): Intent? {
        return try {
            val activityManagerNativeClass = Class.forName("android.app.ActivityManagerNative")
            val getDefault: Method = activityManagerNativeClass.getDeclaredMethod("getDefault")
            val defaultManager: Any = getDefault.invoke(null)
                ?: throw IllegalStateException("ActivityManagerNative.getDefault() returned null")
            val mTargetField: Field = PendingIntent::class.java.getDeclaredField("mTarget")
            mTargetField.setAccessible(true)
            val mTarget: Any = mTargetField.get(pendingIntent)
                ?: throw IllegalStateException("PendingIntent.mTarget field is null")
            val defaultManagerClassName = defaultManager.javaClass.name
            when (defaultManagerClassName) {
                "android.app.ActivityManagerProxy" -> {
                    return try {
                        getIntentFromProxy(defaultManager, mTarget)
                    } catch (e: RemoteException) {
                        // Note from PendingIntent.getIntent(): Should never happen.
                        null
                    }
                    getIntentFromService(mTarget)
                }
                "com.android.server.am.ActivityManagerService" -> getIntentFromService(mTarget)
                else -> throw IllegalStateException("Unsupported IActivityManager inheritor: $defaultManagerClassName")
            }
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(e)
        } catch (e: NoSuchMethodException) {
            throw IllegalStateException(e)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(e)
        } catch (e: InvocationTargetException) {
            throw IllegalStateException(e)
        } catch (e: NoSuchFieldException) {
            throw IllegalStateException(e)
        }
    }

    @Throws(RemoteException::class)
    private fun getIntentFromProxy(defaultManager: Any, sender: Any): Intent? {
        val activityManagerProxyClass: Class<*>
        val mRemote: IBinder
        val GET_INTENT_FOR_INTENT_SENDER_TRANSACTION = IBinder.FIRST_CALL_TRANSACTION + 160
        val iActivityManagerDescriptor = "android.app.IActivityManager"
        try {
            activityManagerProxyClass = Class.forName("android.app.ActivityManagerProxy")
            val mRemoteField: Field = activityManagerProxyClass.getDeclaredField("mRemote")
            mRemoteField.isAccessible = true
            mRemote = mRemoteField.get(defaultManager) as IBinder
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(e)
        } catch (e: NoSuchFieldException) {
            throw IllegalStateException(e)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(e)
        }

        // From ActivityManagerProxy.getIntentForIntentSender()
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        data.writeInterfaceToken(iActivityManagerDescriptor)
        data.writeStrongBinder((sender as IInterface).asBinder())
        transact(mRemote, data, reply, 0)
        reply.readException()
        val res = if (reply.readInt() != 0) Intent.CREATOR.createFromParcel(reply) else null
        data.recycle()
        reply.recycle()
        return res
    }

    private fun transact(remote: IBinder, data: Parcel, reply: Parcel, i: Int): Boolean {
        // TODO: Here must be some native call to convert ((BinderProxy) remote).mObject int
        // to IBinder* native pointer and do some more magic with it.
        // See android_util_Binder.cpp: android_os_BinderProxy_transact() in the Android sources.
        return true
    }

    private fun getIntentFromService(sender: Any): Intent? {
        val pendingIntentRecordClassName = "com.android.server.am.PendingIntentRecord"
        if (sender.javaClass.name != pendingIntentRecordClassName) {
            return null
        }
        try {
            val pendingIntentRecordClass = Class.forName(pendingIntentRecordClassName)
            val keyField: Field = pendingIntentRecordClass.getDeclaredField("key")
            val key: Any = keyField.get(sender)
            val keyClass = Class.forName("com.android.server.am.PendingIntentRecord\$Key")
            val requestIntentField: Field = keyClass.getDeclaredField("requestIntent")
            requestIntentField.isAccessible = true
            val requestIntent = requestIntentField.get(key) as Intent
            return Intent(requestIntent)
        } catch (e: ClassCastException) {
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException(e)
        } catch (e: NoSuchFieldException) {
            throw IllegalStateException(e)
        } catch (e: IllegalAccessException) {
            throw IllegalStateException(e)
        }
        return null
    }
}