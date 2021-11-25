package com.bycen.notify.activity

import android.app.ListActivity
import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import com.bycen.notify.R
import com.bycen.notify.adapter.AppAdapter
import com.bycen.notify.databinding.ActivityFirstScreenBinding
import com.bycen.notify.model.NotificationApp
import com.bycen.notify.utils.*
import java.text.Collator
import java.util.*
import kotlin.collections.ArrayList

/**
 * first screen to show & select apps
 *
 */
class FirstScreenActivity : ListActivity(), AppCheckListener {
    private lateinit var binding: ActivityFirstScreenBinding

    private var pm: PackageManager? = null
    private var adapter: AppAdapter? = null
    private val selectedApps = ArrayList<NotificationApp>()
    private val deselectedApps = ArrayList<NotificationApp>()
    private val db = NotifyDataBaseHelper(this)
    private var progress: ProgressDialog? = null
    private var editor: SharedPreferences.Editor? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val checkIfEditAppsScreen = intent.getBooleanExtra(KEY_NOTIFY_EDIT_APPS, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (!prefs.getBoolean(KEY_NOTIFY_FIRST_TIME, false) || checkIfEditAppsScreen) {
            editor = prefs.edit()
            //Did this to avoid loading of startApp function if the user doesnt check any apps
            if (db.notificationCount > 0) {
                editor!!.putBoolean(KEY_NOTIFY_FIRST_TIME, true).apply()
            }
        } else {
            startApp()
            return
        }

        binding = ActivityFirstScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (checkIfEditAppsScreen) {
            binding.enter.text = getString(R.string.update)
        } else {
            binding.enter.text = getString(R.string.okay)
        }
        binding.enter.setOnClickListener {
            if (checkIfEditAppsScreen) {
                db.addAppsSelected(selectedApps)
                for (item in deselectedApps) {
                    db.deleteNotificationApps(item)
                }
                val intent = Intent(this, NotificationAppListActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
                finish()
            } else {
                db.addAppsSelected(selectedApps)
                startApp()
            }
            if (db.notificationCount > 0) {
                editor!!.putBoolean(KEY_NOTIFY_SPLASH, true).apply()
            }
        }
        pm = packageManager

        loadApps()
    }

    override fun setApps(
        packageName: String?,
        checkIfChecked: Boolean,
        appName: String?,
        appImage: ByteArray?
    ) {
        val app = appImage?.let { NotificationApp(appName, it, packageName) }
        if (checkIfChecked) {
            app?.let { selectedApps.add(it) }
            deselectedApps.remove(app)
            packageName?.let { Log.d("app selected", it) }
        } else {
            selectedApps.remove(app)
            app?.let { deselectedApps.add(it) }
            packageName?.let { Log.d("app deselected", it) }
        }
    }

    private fun startApp() {
        startActivity(Intent(this, NotificationAppListActivity::class.java))
        finish()
    }

    private fun loadApps() {
        runOnUiThread {
            progress = ProgressDialog.show(
                this@FirstScreenActivity,
                null,
                getString(R.string.loading_app_list)
            )
        }
        TaskRunner.singleton.executeAsync({
            val list = pm!!.getInstalledApplications(PackageManager.GET_META_DATA)
            list.sortWith { p0, p1 ->
                val l0 = pm!!.getApplicationLabel(p0).toString().uppercase(LOCALE_TR)
                val l1 = pm!!.getApplicationLabel(p1).toString().uppercase(LOCALE_TR)
                Collator.getInstance(LOCALE_TR).compare(l0, l1)
            }
            val apps = list.filter { pm!!.getLaunchIntentForPackage(it.packageName) != null }
            adapter = AppAdapter(
                this@FirstScreenActivity,
                R.layout.single_app_layout,
                apps
            )
        }) {
            progress?.dismiss()
            listAdapter = adapter
            binding.enter.visibility = View.VISIBLE
        }
    }

//    private fun checkForLaunchIntent(list: List<ApplicationInfo>): List<ApplicationInfo> {
//        val applist = ArrayList<ApplicationInfo>()
//        for (info in list) {
//            try {
//                if (null != pm!!.getLaunchIntentForPackage(info.packageName)) {
//                    applist.add(info)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//        return applist
//    }
}