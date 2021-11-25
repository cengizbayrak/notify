package com.bycen.notify.activity

import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import com.bycen.notify.R
import com.bycen.notify.databinding.ActivityAboutBinding

/**
 * about
 *
 */
class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    private var actionBarHeader: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setup()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setup() {
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.action_bar_layout)
        actionBarHeader = supportActionBar?.customView?.findViewById(R.id.actionBarText)
        supportActionBar?.elevation = 0f
        actionBarHeader?.text = getString(R.string.about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}