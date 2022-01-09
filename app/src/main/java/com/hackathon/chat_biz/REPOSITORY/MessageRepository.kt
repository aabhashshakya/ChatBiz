package com.hackathon.chat_biz.REPOSITORY

import android.net.Uri
import android.util.Log
import com.hackathon.chat_biz.MODEL.*
import com.hackathon.chat_biz.NOTIFICATIONS.*
import com.hackathon.chat_biz.VIEW.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import javax.inject.Inject


private const val TAG = "Message Repository"

class MessageRepository @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseDatabase,
    val storage: FirebaseStorage,
    val retrofit: Retrofit
) {
    private val fcmApiService: FCMAPIService = retrofit.create(FCMAPIService::class.java)

    //we need Retrofit for sending FCM,  we have given instructions to HILT on how to create Retrofit object in Module

    fun sendMessageToUser(message: String, receiverUserID: String, chatUID: String) {

        //chats will be stored in this directory
        val reference = db.getReference(CHATS).child(chatUID)

        //.push() creates a unique child, this is so that we can uniquely identify each message
        val messageKey = reference.push().key//getting that unique key and storing it

        //we put seen as null as if no internet, the message wont actually be sent, but only stored in the local cache, still appears
        //in the recycler view.. it is pending write to db in server, in our recycler view adapter, we check the seen
        //if seen == null, we display a Sending... message, that's why we initially put seen as null
        val messageToSend = Chat(
            senderID = auth.currentUser!!.uid,
            receiverID = receiverUserID,
            textMessage = message,
            imageUrl = "",
            seen = false,
            messageID = messageKey!!,

            )
        //THIS IS FOR THE CHAT LIST OF THE PEOPLE WE HAVE MESSAGED
        val chatListSenderReference =
            db.getReference(CHAT_LIST).child(auth.currentUser!!.uid)
                .child(receiverUserID)
        chatListSenderReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //if snapshot doesnt exist, this is the first time we messaged them, so we add them to out chatlist of people
                //we have talked to
                if (!snapshot.exists()) {
                    chatListSenderReference.setValue(ChatList(receiverUserID))

                    //same for the receiver
                    val chatListReceiverReference =
                        db.getReference(CHAT_LIST).child(receiverUserID)
                            .child(auth.currentUser!!.uid)
                    chatListReceiverReference.setValue(ChatList(auth.currentUser!!.uid))
                }

            }

            override fun onCancelled(error: DatabaseError) {

            }

        })

        reference.child(messageKey).setValue(messageToSend).addOnCompleteListener {

            //the code inside this addOnCompleteListener is only triggered when the message is sent to database online// not when
            //it is written in local db cache

            if (it.isSuccessful) {
                Log.d(
                    TAG,
                    "sendMessageToUser: Sending message : ${messageToSend.textMessage} was SUCCESSFUL."
                )


                //SENDING THE REMOTE MESSAGE (FOR NOTIFICATION)
                sendRemoteMessage(receiverUserID, message, "")

            } else {
                Log.d(
                    TAG,
                    "sendMessageToUser: Sending message : ${messageToSend.textMessage} FAILED"
                )
            }
        }


    }


    fun sendImageMessage(imageUri: Uri?, receiverUserID: String, chatUID: String) {
        val reference = db.getReference(CHATS).child(chatUID)
        val messageKey = reference.push().key

        //this is the place where we want to store all chat images
        //also the name of the image will be that UNIQUE message key
        val storageReference = storage.getReference(CHAT_IMAGES_STORAGE).child("$messageKey.jpg")

        if (imageUri != null) {
            //this is how to upload a file to the Firebase Storage
            val uploadImage = storageReference.putFile(imageUri)

            //continuewithTask: Returns a new Task that will be completed with the result of applying the specified Continuation
            // to this Task. The Continuation will be called on the main application thread.
            uploadImage.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>?> {
                //as we can see, the previous task is the input and the Task<Uri> will be the output for this continuation

                if (!it.isSuccessful) {
                    Log.d(
                        TAG,
                        "uploadImageToFirebaseStorage: ${it.exception!!.printStackTrace()}"
                    )
                    //return@Continuation null//returns Task<Uri> =null
                }
                return@Continuation storageReference.downloadUrl //returns Task<Uri> type

            }).addOnCompleteListener {
                //get the Uri from the task result//result is uri
                val downloadUrl = it.result?.toString()

                //sending the IMAGE MESSAGE
                //when sending image message, we set the textMessage to "Sent you a message" as denoted by the constant
                val messageToSend = Chat(
                    senderID = auth.currentUser!!.uid,
                    receiverID = receiverUserID,
                    textMessage = IMAGE_MESSAGE,
                    seen = false,
                    imageUrl = downloadUrl!!,
                    messageID = messageKey!!,


                    )
                reference.child(messageKey).setValue(messageToSend).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        //SENDING THE REMOTE MESSAGE (FOR NOTIFICATION)
                        sendRemoteMessage(
                            receiverUserID, "Sent you a photo", downloadUrl
                        )
                    } else
                        Log.d(
                            TAG,
                            "sendMessageToUser: Sending message : ${messageToSend.textMessage} FAILED"
                        )

                }


            }

        }


    }


    //SENDING NOTIFICATION TO THE RECEIVER WHEN WE SEND THEM A MESSAGE
    private fun sendRemoteMessage(receiverUserID: String, message: String, imageUrl: String) {

        //getting the token of the receiver// we need it for sending a remote message(notification) to them
        db.getReference(TOKEN).child(receiverUserID)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val receiversToken = snapshot.getValue(Token::class.java)


                        //this is just so to get our username and user profile pic
                        db.getReference(USERS).child(auth.currentUser!!.uid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val currentUser = snapshot.getValue(User::class.java)
                                        val username = currentUser!!.username
                                        val userprofilepic = currentUser.profilePic

                                        //filling up the data we need to send for the notification
                                        val notificationData = NotificationData(
                                            sender = auth.currentUser!!.uid,
                                            senderPic = userprofilepic,
                                            title = "$username sent you a message",
                                            body = "$username : $message",
                                            imageUrl = imageUrl,
                                            receiver = receiversToken?.tokenID.toString()
                                        )

                                        //this is the object that we actually send to the API
                                        val body = SendNotification(
                                            data = notificationData,
                                            to = receiversToken?.tokenID.toString()
                                        )
                                        Log.d(
                                            TAG,
                                            "onDataChange: Preparing to send notification to:${receiversToken?.tokenID.toString()}"
                                        )

                                        fcmApiService.sendNotifications(body).enqueue(object :
                                            Callback<MyResponse> {
                                            override fun onResponse(
                                                call: Call<MyResponse>,
                                                response: Response<MyResponse>
                                            ) {
                                                Log.d(
                                                    TAG,
                                                    "onResponse: RemoteMessage: Sending notifications " +
                                                            "Response code = ${response.code()}"

                                                )
                                                Log.d(
                                                    TAG,
                                                    "onResponse: RemoteMessage: Sending notifications " +
                                                            "Error message = ${
                                                                response.errorBody()?.string()
                                                            }"
                                                )




                                                if (response.code() == 200) {
                                                    if (response.body()?.success != 1) {
                                                        Log.d(
                                                            TAG,
                                                            "onResponse: RemoteMessage: Sending notifications FAILED" +
                                                                    "success code: ${response.body()?.success}"

                                                        )

                                                    } else {
                                                        Log.d(
                                                            TAG,
                                                            "onResponse: RemoteMessage: Sending notifications SUCCESS" +
                                                                    "success code: ${response.body()?.success}"
                                                        )
                                                    }
                                                }
                                            }

                                            override fun onFailure(
                                                call: Call<MyResponse>,
                                                t: Throwable
                                            ) {
                                                Log.d(
                                                    TAG,
                                                    "onResponse: Error in sending RemoteNotification" +
                                                            t.printStackTrace()

                                                )
                                            }


                                        })


                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.d(TAG, "onCancelled: Reading user from db error: ${error.toException().printStackTrace()}")

                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "onCancelled:Getting user token from db error: ${error.toException().printStackTrace()}")

                }
            })
    }


    //for deleting messages
    fun deleteMessage(messageToDelete: Chat, chatUID: String) {
        db.getReference(CHATS).child(chatUID).child(messageToDelete.messageID).removeValue()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "deleteMessage: Message deleted ${messageToDelete.messageID}")
                } else
                    Log.d(TAG, "deleteMessage: Message delete failed. ${messageToDelete.messageID}")
                it.exception?.printStackTrace()
            }
    }


}






