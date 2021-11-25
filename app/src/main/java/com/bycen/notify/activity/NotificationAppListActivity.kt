package com.bycen.notify.activity

import android.content.*
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.bycen.notify.R
import com.bycen.notify.adapter.NotificationAppAdapter
import com.bycen.notify.databinding.ActivityNotificationAppListBinding
import com.bycen.notify.model.Notification
import com.bycen.notify.model.NotificationApp
import com.bycen.notify.service.*
import com.bycen.notify.utils.*
import java.text.Collator
import kotlin.math.roundToInt

/**
 * notification app list
 *
 */
class NotificationAppListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationAppListBinding

    private var notifyDataBaseHelper: NotifyDataBaseHelper? = null
    private var alertDialog: AlertDialog? = null

    private val onNotify = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Notification.KEY_ACTION) {
                (intent.getSerializableExtra(Notification.KEY_SERIALIZABLE) as? Notification)?.let {
                    notifyDataBaseHelper?.addNotification(it)
                }
            }
        }
    }

    private val isNotifyServiceRunning: Boolean
        get() {
            val cn = ComponentName(this, NotifyService::class.java)
            val flat = Settings.Secure.getString(contentResolver, ENABLED_NOTIFICATION_LISTENERS)
            return flat != null && flat.contains(cn.flattenToString())
        }

//    private val isNLServiceRunning: Boolean
//        get() {
//            val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
//            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
//                if (NotifyService::class.java.name == service.service.className) {
//                    return true
//                }
//            }
//            return false
//        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNotificationAppListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!isNotifyServiceRunning) {
            alertDialog(this)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            onNotify,
            IntentFilter(Notification.KEY_ACTION)
        )
        notifyDataBaseHelper = NotifyDataBaseHelper(this)

        binding.swipe.apply {
            setOnRefreshListener {
                populate()
                isRefreshing = false
            }
        }
        populate()
    }

    override fun onResume() {
        super.onResume()
        if (!isNotifyServiceRunning) {
            startService(Intent(this, NotifyService::class.java))
        }
    }

    override fun onPause() {
        super.onPause()
        if (alertDialog?.isShowing == true) {
            alertDialog?.dismiss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var result = true
        when (item.itemId) {
            R.id.item_edit -> {
                val intent = Intent(this, FirstScreenActivity::class.java)
                intent.putExtra(KEY_NOTIFY_EDIT_APPS, true)
                startActivity(intent)
            }
            R.id.item_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
            }
            else -> {
                result = super.onOptionsItemSelected(item)
            }
        }
        return result
    }

    private fun populate() {
        TaskRunner.singleton.executeAsync({
            val list = notifyDataBaseHelper!!.allNotificationApps.sortedWith { p0, p1 ->
                val n0 = p0.name!!.uppercase(LOCALE_TR)
                val n1 = p1.name!!.uppercase(LOCALE_TR)
                Collator.getInstance(LOCALE_TR).compare(n0, n1)
            }
            ArrayList<NotificationApp>(list)
        }) { listSorted ->
            val spanCount = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_PORTRAIT -> 4
                Configuration.ORIENTATION_LANDSCAPE -> 6
                else -> 1
            }
            binding.recyclerView.apply {
                if (layoutManager == null) {
                    layoutManager = GridLayoutManager(
                        this@NotificationAppListActivity,
                        spanCount
                    )
                }
                if (adapter == null) {
                    adapter = NotificationAppAdapter(
                        this@NotificationAppListActivity,
                        listSorted
                    )
                } else {
                    (adapter as NotificationAppAdapter?)?.let { adapter ->
                        adapter.list.clear()
                        adapter.list.addAll(listSorted)
                    }
                }
                (adapter as NotificationAppAdapter?)?.notifyDataSetChanged()
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).roundToInt()
    }

    private fun alertDialog(context: Context) {
        val cancel = getString(R.string.notification_cancel)
        val settings = getString(R.string.notification_settings)
        alertDialog = AlertDialog.Builder(context)
            .setMessage(getString(R.string.app_notification_listen_permission_info))
            .setCancelable(false)
            .setTitle(getString(R.string.hello))
            .setPositiveButton(cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
            .setNegativeButton(settings) { _: DialogInterface?, _: Int ->
                val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    KEY_ANDROID_ACTION_NOTIFICATION_LISTENER_SETTINGS
                } else {
                    KEY_ANDROID_SETTINGS_ACTION_NOTIFICATION_LISTENER_SETTINGS
                }
                val intent = Intent(action)
                startActivity(intent)
            }.create()
        alertDialog?.show()
    }

    class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean
    ) : ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view) // item position
            val column = position % spanCount // item column
            if (includeEdge) {
                outRect.left =
                    spacing - column * spacing / spanCount // spacing - column * ((1f / spanCount) * spacing)
                outRect.right =
                    (column + 1) * spacing / spanCount // (column + 1) * ((1f / spanCount) * spacing)
                if (position < spanCount) { // top edge
                    outRect.top = spacing
                }
                outRect.bottom = spacing // item bottom
            } else {
                outRect.left = column * spacing / spanCount // column * ((1f / spanCount) * spacing)
                outRect.right =
                    spacing - (column + 1) * spacing / spanCount // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing // item top
                }
            }
        }
    }
}