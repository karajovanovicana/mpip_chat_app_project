package com.example.chatapp.data

import com.example.chatapp.data.NotificationData

data class PushNotification(
    val data: NotificationData,
    val to: String?
)

