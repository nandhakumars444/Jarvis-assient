package com.jarvis.assistant.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jarvis.assistant.R
import com.jarvis.assistant.viewmodel.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) = a.time == b.time
            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) = a == b
        }
        private val TIME_FMT = SimpleDateFormat("HH:mm", Locale.getDefault())

        const val TYPE_USER   = 0
        const val TYPE_JARVIS = 1
        const val TYPE_SYSTEM = 2
    }

    override fun getItemViewType(pos: Int) = when (getItem(pos).role) {
        "user"   -> TYPE_USER
        "jarvis" -> TYPE_JARVIS
        else     -> TYPE_SYSTEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layout = when (viewType) {
            TYPE_USER   -> R.layout.item_msg_user
            TYPE_JARVIS -> R.layout.item_msg_jarvis
            else        -> R.layout.item_msg_system
        }
        return VH(LayoutInflater.from(parent.context).inflate(layout, parent, false))
    }

    override fun onBindViewHolder(holder: VH, pos: Int) = holder.bind(getItem(pos))

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView  = itemView.findViewById(R.id.tvText)
        private val tvTime: TextView? = itemView.findViewById(R.id.tvTime)

        fun bind(msg: ChatMessage) {
            tvText.text = msg.text
            tvTime?.text = TIME_FMT.format(Date(msg.time))
        }
    }
}
