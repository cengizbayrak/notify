package com.bycen.notify.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.bycen.notify.databinding.ActivitySplashScreenBinding
import com.bycen.notify.utils.KEY_NOTIFY_SPLASH

/**
 * splash screen
 *
 */
class SplashScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = if (!prefs.getBoolean(KEY_NOTIFY_SPLASH, false)) {
                Intent(this, FirstScreenActivity::class.java)
            } else {
                Intent(this, NotificationAppListActivity::class.java)
            }
            startActivity(intent)
            finish()
        }, 3000L)
    }
}