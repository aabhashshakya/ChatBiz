package com.hackathon.chat_biz.REPOSITORY

import android.util.Log
import com.hackathon.chat_biz.MODEL.User
import com.hackathon.chat_biz.NOTIFICATIONS.Token
import com.hackathon.chat_biz.VIEW.TOKEN
import com.hackathon.chat_biz.VIEW.USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject


private const val TAG = "BaseRepository"

class BaseRepository @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseDatabase
) {


    //make user online // will be automatically offline if the internet connection goes
    fun makeUserOnline() {
        //updating our status to online//only if the user is logged in
        if (auth.currentUser != null) {
            db.getReference(USERS).child(auth.currentUser!!.uid).child("online").setValue(true)
            //if internet disconnect, the user goes offline//it may take some time for firebase to update the data
            db.getReference(USERS).child(auth.currentUser!!.uid).child("online").onDisconnect()
                .setValue(false)
        }
    }


    //if user doesnt exist, the user is added to the database
    fun addUserToDatabase() {
        db.getReference(USERS).child(auth.currentUser!!.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || snapshot.childrenCount < 5) {
                        snapshot.ref.setValue(
                            User(
                                userID = auth.currentUser!!.uid,
                                username = auth.currentUser!!.displayName ?: "User",
                                email = auth.currentUser!!.email ?: "Null",
                                profilePic = "https://firebasestorage.googleapis.com/v0/b/fir-chatapp-93cbf." +
                                        "appspot.com/o/ic_profile.png?alt=media&token=7a3c8ace-c5e4-4dca-821d-37892aa774bd",
                                coverPic = "https://firebasestorage.googleapis.com/v0/b/fir-chatapp-93cbf.appspot.com/o/" +
                                        "coverimage.jpg?alt=media&token=da59999d-1d4a-419a-afcb-e0b897e680bf",
                                online = true,
                                bio = "Hello, its me",
                                location = "No location specified",
                                role = "No role specified",
                                gpson = false
                            )
                        )

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(TAG, "onCancelled: Adding user to database failed ${error.toException().printStackTrace()}")
                }

            }
            )


    }


    fun saveFirebaseToken() {
        //every device has a unique token sent by the firebase. we need this token to send cloud messages(notification)
        //from firebase
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isSuccessful) {
                db.getReference(TOKEN).child(auth.currentUser!!.uid).setValue(Token(it.result!!))
            }
        }
    }

    fun logOutUser() {
        //logs user out after making him offline
        db.getReference(USERS).child(auth.currentUser!!.uid).child("online").setValue(false)
        db.getReference(USERS).child(auth.currentUser!!.uid).child("gpson").setValue(false)
        auth.signOut()
    }

}





