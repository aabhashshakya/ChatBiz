package com.hackathon.chat_biz.REPOSITORY

import android.util.Log
import com.hackathon.chat_biz.MODEL.User
import com.hackathon.chat_biz.VIEW.USERS
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import javax.inject.Inject


private const val TAG = "BaseRepository"

class RegisterRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseDatabase

) {


    fun registerUser(email: String, password: String): Task<AuthResult> {

        return auth.createUserWithEmailAndPassword(email, password)


    }

    //if user doesn't exist, the user is added to the database
    fun addUserToDatabase(username: String, email: String) {
        db.getReference(USERS).child(auth.currentUser!!.uid).setValue(
            User(
                userID = auth.currentUser!!.uid,
                username = username,
                email = email,
                profilePic = "https://firebasestorage.googleapis.com/v0/b/fir-chatapp-93cbf." +
                        "appspot.com/o/ic_profile.png?alt=media&token=7a3c8ace-c5e4-4dca-821d-37892aa774bd",
                coverPic = "https://firebasestorage.googleapis.com/v0/b/fir-chatapp-93cbf.appspot.com/o/" +
                        "coverimage.jpg?alt=media&token=da59999d-1d4a-419a-afcb-e0b897e680bf",
                online = true,
                bio = "Hello, its me",
                location = "No location specified",
                gpson = false
            )
        )
        Log.d(TAG, "addUserToDatabase: New User added to database : $username")

    }


}





