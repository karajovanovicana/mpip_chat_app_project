package com.example.chatapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.chatapp.R
import com.example.chatapp.data.Message
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(val context: Context, val messageList: ArrayList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val ITEM_RECEIVED = 1
    val ITEM_SENT = 2
    val IMAGE_RECEIVED = 3
    val IMAGE_SENT = 4


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 1) {
            val view: View = LayoutInflater.from(context).inflate(R.layout.received, parent, false)
            return ReceivedViewHolder(view)
        } else if (viewType == 2) {
            val view: View = LayoutInflater.from(context).inflate(R.layout.sent, parent, false)
            return SentViewHolder(view)
        } else if (viewType == 3) {
            val view: View =
                LayoutInflater.from(context).inflate(R.layout.received_image, parent, false)
            return ImageReceivedViewHolder(view)
        } else {
            val view: View =
                LayoutInflater.from(context).inflate(R.layout.sent_image, parent, false)
            return ImageSentViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val currentMessage = messageList[position]
        if (holder.javaClass == SentViewHolder::class.java) {
            val viewHolder = holder as SentViewHolder
            holder.sentMessage.text = currentMessage.message

        } else if (holder.javaClass == ImageSentViewHolder::class.java) {
            val viewHolder = holder as ImageSentViewHolder
            Glide.with(context)
                .load(currentMessage.message)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.blank_image)
                )
                .into(holder.sentImage)

        } else if (holder.javaClass == ImageReceivedViewHolder::class.java) {
            val viewHolder = holder as ImageReceivedViewHolder

            Glide.with(context)
                .load(currentMessage.message)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.blank_image)
                )
                .into(holder.receivedImage)
        } else {
            val viewHolder = holder as ReceivedViewHolder
            holder.receivedMessage.text = currentMessage.message
        }
    }

    override fun getItemViewType(position: Int): Int {
        val currentMessage = messageList[position]

        if (FirebaseAuth.getInstance().currentUser?.uid.equals(currentMessage.senderId)) {
            if (URLUtil.isValidUrl(currentMessage.message))
                return IMAGE_SENT
            else
                return ITEM_SENT
        } else {
            if (URLUtil.isValidUrl(currentMessage.message))
                return IMAGE_RECEIVED
            else
                return ITEM_RECEIVED
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage = itemView.findViewById<TextView>(R.id.txt_sentMessage)

    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receivedMessage = itemView.findViewById<TextView>(R.id.txt_receivedMessage)
    }

    class ImageSentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentImage = itemView.findViewById<ImageView>(R.id.sentImage)

    }

    class ImageReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receivedImage = itemView.findViewById<ImageView>(R.id.receivedImage)
    }


}