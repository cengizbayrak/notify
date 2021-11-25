package com.bycen.notify.adapter

import com.bycen.notify.model.Notification
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.PopupMenu
import com.bycen.notify.R
import com.bycen.notify.databinding.ItemNotificationDetailBinding
import com.bycen.notify.utils.LOCALE_TR
import com.bycen.notify.utils.NotifyDataBaseHelper
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Avinash on 19/03/17.
 */
class NotificationAdapter(
    val list: ArrayList<Notification>?
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNotificationDetailBinding.inflate(inflater)
        binding.root.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list!![position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    inner class ViewHolder(
        private val binding: ItemNotificationDetailBinding
    ) : RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {
        private lateinit var item: Notification

        init {
            binding.apply {
                arrayOf(root, title, content, timestamp).forEach {
                    it.setOnClickListener(this@ViewHolder)
                    it.setOnLongClickListener(this@ViewHolder)
                }
            }
        }

        fun bind(item: Notification) {
            this.item = item

            binding.apply {
                title.text = item.title
                content.text = item.message
                timestamp.text = getDate(item.timestamp!!.toLong())
            }
        }

        private fun getDate(timeStamp: Long): String {
            return try {
                val date = Date(timeStamp)
                val format = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
                format.format(date)
            } catch (e: Exception) {
                "xx"
            }
        }

        override fun onClick(view: View) {
            val context = view.context
            val packageManager = context.packageManager
            item.packageName?.let { packageName ->
                packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                    context.startActivity(intent)
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            val context = view.context
            val openApp = context.getString(R.string.open_the_app)
            val delete = context.getString(R.string.delete)
            PopupMenu(context, view).apply {
                menu.add(openApp)
                menu.add(delete)
                setOnMenuItemClickListener {
                    when (it.title.toString().uppercase(LOCALE_TR)) {
                        openApp.uppercase(LOCALE_TR) -> {
                            item.openApp(context)
                        }
                        delete.uppercase(LOCALE_TR) -> {
                            NotifyDataBaseHelper(context).deleteNotifications(item)
                            list?.remove(item)
                            notifyItemRemoved(bindingAdapterPosition)
                        }
                    }
                    true
                }
            }.show()
            return true
        }
    }
}