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

private const val TAG = "SearchViewModel"

class SearchViewModel @ViewModelInject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseDatabase
) : ViewModel() {

    fun getAllUsers(currentUserID: String): LiveData<ArrayList<User>> {
        val allUsers = ArrayList<User>()
        val allUsersSnapshot =
            FirebaseQueryLiveData(db.getReference(USERS).orderByChild("username"))
        val allUsersLiveData = Transformations.switchMap(allUsersSnapshot) {
            if (it.exists()) {
                allUsers.clear()
                for (singleUser in it.children) {
                    val user = singleUser.getValue(User::class.java)
                    if (user!!.userID != currentUserID) {
                        allUsers.add(user)
                    }

                }

            }
            Log.d(TAG, "getAllUsers: All Users list size: ${allUsers.size}")
            MutableLiveData(allUsers)


        }
        return allUsersLiveData


    }

    fun getCurrentUserID(): String {
        return auth.currentUser!!.uid
    }


}



