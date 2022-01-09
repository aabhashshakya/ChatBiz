package com.hackathon.chat_biz.REPOSITORY


import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

import javax.inject.Inject


private const val TAG = "BaseRepository"

class LoginRepository @Inject constructor(
    val auth: FirebaseAuth

) {


    fun loginUser(email: String, password: String): Task<AuthResult> {

        return auth.signInWithEmailAndPassword(email, password)


    }


}





