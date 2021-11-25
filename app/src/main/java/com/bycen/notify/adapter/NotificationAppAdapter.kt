package com.bycen.notify.adapter

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.bycen.notify.R
import com.bycen.notify.activity.MainActivity
import com.bycen.notify.databinding.ItemNotificationAppListBinding
import com.bycen.notify.model.Notification
import com.bycen.notify.model.NotificationApp
import com.bycen.notify.utils.LOCALE_TR
import com.bycen.notify.utils.NotifyDataBaseHelper
import com.google.android.material.snackbar.Snackbar


/**
 * Created by Avinash on 04/06/17.
 */
class NotificationAppAdapter(
    private val context: Context,
    list: ArrayList<NotificationApp>?
) : RecyclerView.Adapter<NotificationAppAdapter.ViewHolder>() {
    val list = ArrayList<NotificationApp>()

    init {
        list?.let {
            this.list.clear()
            this.list.addAll(it)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemNotificationAppListBinding.inflate(inflater)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class ViewHolder(private val binding: ItemNotificationAppListBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener, View.OnLongClickListener {
        private lateinit var item: NotificationApp

        init {
            binding.apply {
                arrayOf(root, thumbnail, title, count).forEach {
                    it.setOnClickListener(this@ViewHolder)
                    it.setOnLongClickListener(this@ViewHolder)
                }
            }
        }

        fun bind(item: NotificationApp) {
            this.item = item
            binding.apply {
                title.text = item.name
                item.image?.let {
                    val image = BitmapDrawable(
                        context.resources,
                        BitmapFactory.decodeByteArray(it, 0, it.size)
                    )
                    thumbnail.setImageDrawable(image)
                    thumbnail.scaleType = ImageView.ScaleType.FIT_CENTER
                }
                count.visibility = View.GONE
                count.text = item.notificationCount.toString()
                if (item.notificationCount > 0) {
                    count.visibility = View.VISIBLE
                    count.text = item.notificationCount.toString()
                }
            }
        }

        override fun onClick(view: View) {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(NotificationApp.KEY_SERIALIZABLE, item)
            }
            context.startActivity(intent)
        }

        override fun onLongClick(view: View): Boolean {
            val context = view.context
            val openApp = context.getString(R.string.open_the_app)
            val deleteNotifications = context.getString(R.string.delete_notifications)
            val delete = context.getString(R.string.delete)
            PopupMenu(context, view).apply {
                menu.add(openApp)
                menu.add(deleteNotifications)
                menu.add(delete)
                setOnMenuItemClickListener {
                    when (it.title.toString().uppercase(LOCALE_TR)) {
                        openApp.uppercase(LOCALE_TR) -> {
                            item.openApp(context)
                        }
                        deleteNotifications.uppercase(LOCALE_TR) -> {
                            with(NotifyDataBaseHelper(context)) {
                                item.packageName?.let { packageName ->
                                    val list = getNotifications(packageName)
                                    deleteNotifications(*list.toTypedArray<Notification>())
                                    notifyItemChanged(bindingAdapterPosition)
                                    item.notificationCount = 0
                                }
                            }
                        }
                        delete.uppercase(LOCALE_TR) -> {
                            NotifyDataBaseHelper(context).deleteNotificationApps(item)
                            list.remove(item)
                            notifyItemRemoved(bindingAdapterPosition)
                        }
                        else -> {
                            Snackbar.make(view, item.name!!, Snackbar.LENGTH_LONG).show()
                        }
                    }
                    true
                }
            }.show()
            return true
        }
    }
}