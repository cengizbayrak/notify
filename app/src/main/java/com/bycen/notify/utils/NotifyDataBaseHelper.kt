package com.bycen.notify.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.bycen.notify.model.NotificationApp
import com.bycen.notify.model.Notification
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.collections.ArrayList

/**
 * sqlite database helper
 *
 * @constructor
 *
 * @param context app context
 */
class NotifyDataBaseHelper(
    context: Context?
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        val createNotificationTable = """
            CREATE TABLE $TABLE_NOTIFICATION(
                $COLUMN_NOTIFICATION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOTIFICATION_TITLE TEXT,
                $COLUMN_NOTIFICATION_MESSAGE TEXT,
                $COLUMN_NOTIFICATION_PACKAGE_NAME TEXT,
                $COLUMN_NOTIFICATION_TIMESTAMP TEXT
            )
        """.trimIndent()
        val createPackageTable = """
            CREATE TABLE $TABLE_APP(
                $COLUMN_APP_PACKAGE_NAME TEXT,
                $COLUMN_APP_NAME TEXT,
                $COLUMN_APP_IMAGE BLOB
            )
        """.trimIndent()
        db.execSQL(createNotificationTable)
        db.execSQL(createPackageTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, i: Int, i1: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_APP")
        onCreate(db)
    }

    fun addNotification(item: Notification) {
        if (!isDataInDBorNot(TABLE_NOTIFICATION, COLUMN_NOTIFICATION_MESSAGE, item.message) &&
            isAppSelected(item.packageName)
        ) {
            val values = ContentValues().apply {
                put(COLUMN_NOTIFICATION_TITLE, item.title)
                put(COLUMN_NOTIFICATION_MESSAGE, item.message)
                put(COLUMN_NOTIFICATION_PACKAGE_NAME, item.packageName)
                put(COLUMN_NOTIFICATION_TIMESTAMP, item.timestamp)
            }
            this.writableDatabase.use { db ->
                db.insert(TABLE_NOTIFICATION, null, values)
            }
        }
    }

    fun addAppsSelected(list: ArrayList<NotificationApp>) {
        this.writableDatabase.use { db ->
            for (s in list) {
                if (!isDataInDBorNot(TABLE_APP, COLUMN_APP_PACKAGE_NAME, s.packageName)) {
                    val values = ContentValues().apply {
                        put(COLUMN_APP_PACKAGE_NAME, s.packageName)
                        put(COLUMN_APP_NAME, s.name)
                        put(COLUMN_APP_IMAGE, s.image)
                    }
                    db.insert(TABLE_APP, null, values)
                }
            }
        }
    }

    fun getNotifications(packageName: String): CopyOnWriteArrayList<Notification> {
        val list = CopyOnWriteArrayList<Notification>()
        val query = "select * from $TABLE_NOTIFICATION where $COLUMN_NOTIFICATION_PACKAGE_NAME = ?"
        this.readableDatabase.rawQuery(query, arrayOf(packageName)).use { cursor ->
            while (cursor.moveToNext()) {
                val item = Notification(
                    title = cursor.getString(1),
                    message = cursor.getString(2),
                    packageName = cursor.getString(3),
                    timestamp = cursor.getString(4)
                )
                list.add(item)
            }
        }
        return list
    }

    fun isAppSelected(packageName: String?): Boolean {
        val query = "select * from $TABLE_APP where $COLUMN_APP_PACKAGE_NAME = ?"
        this.writableDatabase.rawQuery(query, arrayOf(packageName)).use { cursor ->
            if (cursor.count <= 0) {
                return false
            }
        }
        return true
    }

    fun getNotificationCount(packageName: String): Int {
        val query = "select count(*) from $TABLE_NOTIFICATION where $COLUMN_APP_PACKAGE_NAME = ?"
        return this.readableDatabase.rawQuery(query, arrayOf(packageName)).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        }
    }

    //notifData.setId(Integer.parseInt(cursor.getString(0)));
    val allNotifications: ArrayList<Notification>
        get() {
            val list = ArrayList<Notification>()
            val query = "select * from $TABLE_NOTIFICATION"
            this.writableDatabase.rawQuery(query, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val item = Notification().apply {
                            //notifData.setId(Integer.parseInt(cursor.getString(0)));
                            title = cursor.getString(1)
                            message = cursor.getString(2)
                        }
                        list.add(item)
                    } while (cursor.moveToNext())
                }
            }
            return list
        }

    //notifData.setId(Integer.parseInt(cursor.getString(0)));
    val allNotificationApps: ArrayList<NotificationApp>
        get() {
            val list = ArrayList<NotificationApp>()
            val selectQuery = "select * from $TABLE_APP"
            this.writableDatabase.rawQuery(selectQuery, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val item = NotificationApp().apply {
                            //notifData.setId(Integer.parseInt(cursor.getString(0)));
                            packageName = cursor.getString(0)
                            name = cursor.getString(1)
                            image = cursor.getBlob(2)
                        }
                        list.add(item)
                    } while (cursor.moveToNext())
                }
            }
            list.forEach {
                it.packageName?.let { packageName ->
                    it.notificationCount = getNotificationCount(packageName)
                }
            }
            return list
        }

    private fun isDataInDBorNot(table: String, field: String, value: String?): Boolean {
        val query = "select * from $table where $field = ?"
        this.writableDatabase.rawQuery(query, arrayOf(value)).use { cursor ->
            if (cursor.count <= 0) {
                cursor.close()
                return false
            }
        }
        return true
    }

    val notificationCount: Int
        get() {
            val query = "select  * from $TABLE_APP"
            this.readableDatabase.rawQuery(query, null).use { cursor ->
                return cursor.count
            }
        }

    val notificationAppCount: Int
        get() {
            val query = "select  * from $TABLE_NOTIFICATION"
            this.readableDatabase.rawQuery(query, null).use { cursor ->
                return cursor.count
            }
        }

    fun deleteNotificationApps(vararg app: NotificationApp) {
        this.writableDatabase.use { db ->
            for (item in app) {
                db.delete(
                    TABLE_APP,
                    "$COLUMN_APP_PACKAGE_NAME = ?",
                    arrayOf(item.packageName.toString())
                )
            }
        }
    }

    fun deleteNotifications(vararg notification: Notification) {
        this.writableDatabase.use { db ->
            for (item in notification) {
                db.delete(
                    TABLE_NOTIFICATION,
                    "$COLUMN_NOTIFICATION_MESSAGE = ?",
                    arrayOf(item.message.toString())
                )
            }
        }
    }

    companion object {
        private const val DATABASE_VERSION = 16
        private const val DATABASE_NAME = "notify"

        private const val TABLE_NOTIFICATION = "notification"
        private const val TABLE_APP = "app"

        private const val COLUMN_NOTIFICATION_ID = "id"
        private const val COLUMN_NOTIFICATION_TITLE = "title"
        private const val COLUMN_NOTIFICATION_MESSAGE = "message"
        private const val COLUMN_NOTIFICATION_TIMESTAMP = "timestamp"
        private const val COLUMN_NOTIFICATION_PACKAGE_NAME = "packageName"

        private const val COLUMN_APP_PACKAGE_NAME = "packageName"
        private const val COLUMN_APP_NAME = "name"
        private const val COLUMN_APP_IMAGE = "image"
    }
}