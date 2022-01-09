package com.hackathon.chat_biz.NOTIFICATIONS


//data we send as remote message to FCM
data class NotificationData
    (
    val sender: String = "",
    val senderPic: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val receiver: String = ""
) {
}