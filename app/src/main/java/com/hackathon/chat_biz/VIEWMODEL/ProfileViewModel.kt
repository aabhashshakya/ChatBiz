package com.hackathon.chat_biz.VIEWMODEL

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.hackathon.chat_biz.MODEL.User
import com.hackathon.chat_biz.REPOSITORY.FirebaseQueryLiveData
import com.hackathon.chat_biz.VIEW.USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

private const val TAG = "ProfileViewModel"

class ProfileViewModel @ViewModelInject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseDatabase
) : ViewModel() {

    fun loadUserProfile(userID: String): LiveData<User> {
        val userLiveDataSnapshot = FirebaseQueryLiveData(db.getReference(USERS).child(userID))
        val userProfileLiveData = Transformations.switchMap(userLiveDataSnapshot) {

            var user = User()
            if (it.exists()) {
                Log.d(TAG, "loadUserProfile: Getting the user profile")
                user = it.getValue(User::class.java)!!
            }
            MutableLiveData(user)


        }
        return userProfileLiveData


    }

    fun getCurrentUserID(): String {
        return auth.currentUser!!.uid
    }

}