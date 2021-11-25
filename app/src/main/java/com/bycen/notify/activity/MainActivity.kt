package com.bycen.notify.activity

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.bycen.notify.adapter.NotificationAdapter
import com.bycen.notify.R
import com.bycen.notify.databinding.ActivityMainBinding
import com.bycen.notify.model.Notification
import com.bycen.notify.model.NotificationApp
import com.bycen.notify.utils.KEY_NOTIFY_EDIT_APPS
import com.bycen.notify.utils.NotifyDataBaseHelper
import com.bycen.notify.utils.SwipeToDeleteCallback
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var actionBarHeader: TextView? = null

    private val list = ArrayList<Notification>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupActionBar()
    }

    override fun onResume() {
        super.onResume()

        (intent.getSerializableExtra(NotificationApp.KEY_SERIALIZABLE) as? NotificationApp)?.let {
            actionBarHeader?.text = it.name
            it.packageName?.let { packageName ->
                list.clear()
                list.addAll(NotifyDataBaseHelper(this).getNotifications(packageName))
            }
        }
        if (list.isNotEmpty()) {
            list.reverse()
            binding.recyclerView.apply {
                setHasFixedSize(true)
                adapter = NotificationAdapter(list)
                layoutManager = LinearLayoutManager(this@MainActivity)
                itemAnimator = DefaultItemAnimator()
                addItemDecoration(SimpleDividerItemDecoration(this@MainActivity))
                ItemTouchHelper(object : SwipeToDeleteCallback(this@MainActivity) {
                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                        (adapter as? NotificationAdapter)?.let { adapter ->
                            adapter.list?.let { list ->
                                val item = list[viewHolder.bindingAdapterPosition]
                                NotifyDataBaseHelper(this@MainActivity).deleteNotifications(item)
                                list.remove(item)
                                adapter.notifyItemRemoved(viewHolder.bindingAdapterPosition)
                            }
                        }
                    }
                }).attachToRecyclerView(this)
            }
        } else {
            binding.noNotifText.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notification, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var result = true
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.item_delete -> {
                with(NotifyDataBaseHelper(this)) {
                    if (list.isNotEmpty()) {
                        val notification = list[0]
                        notification.packageName?.let { packageName ->
                            val list = getNotifications(packageName)
                            deleteNotifications(*list.toTypedArray<Notification>())
                            (binding.recyclerView.adapter as NotificationAdapter).let { adapter ->
                                list.clear()
                                adapter.list?.clear()
                                adapter.notifyDataSetChanged()
                            }
                            binding.noNotifText.visibility = View.VISIBLE
                        }
                    }
                }
            }
            R.id.item_edit -> {
                val intent = Intent(this, FirstScreenActivity::class.java).apply {
                    putExtra(KEY_NOTIFY_EDIT_APPS, true)
                }
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

    private fun setupActionBar() {
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.action_bar_layout)
        actionBarHeader = supportActionBar?.customView?.findViewById(R.id.actionBarText)
        supportActionBar?.elevation = 0f
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SimpleDividerItemDecoration internal constructor(context: Context) : ItemDecoration() {
        private val divider = ContextCompat.getDrawable(context, R.drawable.line_divider)!!

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            val left = parent.paddingLeft
            val right = parent.width - parent.paddingRight
            val childCount = parent.childCount
            for (i in 0 until childCount) {
                val child = parent.getChildAt(i)
                val params = child.layoutParams as RecyclerView.LayoutParams
                val top = child.bottom + params.bottomMargin
                val bottom = top + divider.intrinsicHeight
                divider.setBounds(left, top, right, bottom)
                divider.draw(c)
            }
        }
    }
}