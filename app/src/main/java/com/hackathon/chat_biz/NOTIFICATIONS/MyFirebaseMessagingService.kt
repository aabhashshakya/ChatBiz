package com.hackathon.chat_biz.NOTIFICATIONS

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.hackathon.chat_biz.HILT.MyApplication
import com.hackathon.chat_biz.VIEW.MainActivity
import com.hackathon.chat_biz.VIEW.TOKEN
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "FBMessagingService"

//this sould extends the FirebaseMessagingService
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var db: FirebaseDatabase
    var notificationManager: NotificationManager? = null


    companion object MessagingServiceConstants {

        const val SENDER = "sender"
        const val SENDER_PIC = "senderPic"
        const val TITLE = "title"
        const val BODY = "body"
        const val IMAGE_URL = "imageUrl"
        const val NOTIFICATION_GROUP_NAME = "group"
        const val CHANNEL_ID = "messages"

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "New Remote message notification was received from firebase ")
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val body = remoteMessage.data[BODY]
        val imageUrl = remoteMessage.data[IMAGE_URL]
        if((body=="" && imageUrl=="") || (body == null && imageUrl==null)){
            displayOreoNotification(remoteMessage)

        }else {

            //only display notification if app is in the background
            if (MyApplication.isInBackground == true) {

                displayOreoNotification(remoteMessage)

            }
        }


    }

    //firebase sends an unique token for each device, so we need to get that token first to communicate with firebase messaging
    //this token is changed when app is uninstalled/app data cleared/app restored on a new device/you delete the token
    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Log.d(TAG, "onNewToken: $p0")

        //updating the token//adding it to our database
        //each user device has his/her own unique token
        //this token allows us to know to which device to send the notification to
        if (auth.currentUser != null) {
            db.getReference(TOKEN).child(auth.currentUser!!.uid).setValue(Token(p0))
        }

    }

    //NOTIFICATIONS FOR DEVICES RUNNING OREO OR GREATER

    @RequiresApi(Build.VERSION_CODES.O)
    private fun displayOreoNotification(remoteMessage: RemoteMessage) {
        //retrieving info from the remote message so we can show them as notification

        val sender = remoteMessage.data[SENDER]
        val title = remoteMessage.data[TITLE]
        val body = remoteMessage.data[BODY]
        val senderPic = remoteMessage.data[SENDER_PIC]
        val imageUrl = remoteMessage.data[IMAGE_URL]

        Log.d(
            TAG,
            "sendOreoNotification: Notification message details: sender:$sender title:$title body:$body"
        )


        // regular expression \\D matches any decimal number. here it is replaced by nothing//just trying to make the request code unique
        //thats it
        val requestCode = sender!!.replace("[\\D]".toRegex(), "").toInt()

        val intent = Intent(this, MainActivity::class.java)

        val bundle = Bundle()
        bundle.putString("senderID", sender)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent =
            PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_ONE_SHOT)

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        //we do this because if the notification request code is the same, that notification will be replaced
        //this is bad as if we have not cancelled the previous notification and if another notification is received, the previous
        //notification will be replaced

        //in our use case since the notificationRequest code = requestCode which is just the sender's id manipulated(see above)
        //the notfications from same sender gets replaced, but if its a notificaiton from a different sender, they dont get replaced
        var notificationRequestCode = 0
        if (requestCode > 0) {
            notificationRequestCode = requestCode
        }

        //SENDING THE NOTIFICATIONs
        //first creating a channel
        val channel =
            NotificationChannel(CHANNEL_ID, "ChatBiz", NotificationManager.IMPORTANCE_HIGH)
        channel.enableLights(true)
        channel.enableVibration(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager?.createNotificationChannel(channel)


        val myNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentIntent(pendingIntent)
            .setContentTitle(title)
            .setSmallIcon(com.hackathon.chat_biz.R.drawable.ic_stat_name)
            .setContentText(body)
            .setSound(defaultSound)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_NAME)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        //checking if it was a photo message
        if (imageUrl != null && imageUrl != "") {
            //load the image message
            val futureTarget = Glide.with(this)
                .asBitmap()
                .load(imageUrl)
                .submit()
            val bitmap = futureTarget.get()
            myNotification.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
            Glide.with(this).clear(futureTarget)

        }

        //for loading the sender's image into the notification
        val futureTarget = Glide.with(this)
            .asBitmap()
            .load(senderPic)
            .submit()

        val bitmap = futureTarget.get()
        myNotification.setLargeIcon(bitmap)

        Glide.with(this).clear(futureTarget)

        notificationManager?.notify(notificationRequestCode, myNotification.build())


    }

}