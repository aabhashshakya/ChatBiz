package com.hackathon.chat_biz.VIEWMODEL

import android.net.Uri
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.hackathon.chat_biz.MODEL.User
import com.hackathon.chat_biz.REPOSITORY.FirebaseQueryLiveData
import com.hackathon.chat_biz.REPOSITORY.SettingsRepository
import com.hackathon.chat_biz.VIEW.USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


private const val TAG = "SettingsViewModel"

class SettingsViewModel @ViewModelInject constructor(
    private val repository: SettingsRepository,
    private val db: FirebaseDatabase,
    private val auth: FirebaseAuth
) : ViewModel() {

    //Uploading the image in our Firebase Storage and DB
    fun uploadImageToFirebaseStorage(imageUri: Uri?, pictureToPick: Int) {

        repository.uploadImageToFirebaseStorage(imageUri, pictureToPick)


    }

    //updating the user info
    fun updateUserInfo(nodeToUpdate: String, newInfo: String) {
        repository.updateUserInfo(nodeToUpdate, newInfo)
    }

    fun loadUserProfile(): LiveData<User> {
        val userLiveDataSnapshot =
            FirebaseQueryLiveData(db.getReference(USERS).child(auth.currentUser!!.uid))
        val userProfileLiveData = Transformations.switchMap(userLiveDataSnapshot) {

            var user = User()
            if (it.exists()) {
                Log.d(TAG, "loadUserProfile: Getting the user profile for settings")
                user = it.getValue(User::class.java)!!
            }
            MutableLiveData(user)


        }
        return userProfileLiveData


    }


}