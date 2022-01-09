package com.hackathon.chat_biz.REPOSITORY

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener


//ALWAYS DO THIS WHEN YOU NEED TO QUERY DATABASE AND UPDATE THE VIEWS ACCORDINGLY


//WE HAVE EXTEND LIVE DATA AS WE ONLY WANT TO QUERY THE DATABASE FOR INFO AND UPDATE THE VIEWS WHEN THE FRAGMENT IS VISIBLE
//THIS HELPS PREVENT NULL POINTER EXCEPTIONS IN THE FRAGMENT, IF WE HAD PUT DB QUERY IN FRAGMENT,
//WHEN DB QUERY IS TRIGGERED BUT THE FRAGMENT'S VIEW IS NOT ACTIVE/CREATED
private const val TAG = " FirebaseQueryLiveData"

//Query and DatabaseReference are the same, Query being better as it supports orderedby, etc..
//as we can see it takes a Query and stores a DataSnapshot as its live data value
//this DataSnapshot will be analysed accordingly by the respective view models
class FirebaseQueryLiveData(private val reference: Query) : LiveData<DataSnapshot>() {

    private val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            //updating the value of the live data
            value = snapshot
            Log.d(TAG, "${reference} : Snapshot exists: ${snapshot.exists()}")
        }

        override fun onCancelled(error: DatabaseError) {
            Log.d(TAG, "onCancelled: DB query cancelled : ${error.message}")
        }

    }


    //if the fragment/activity is active, attach the ValueEventListener to the db reference
    override fun onActive() {
        super.onActive()
        Log.d(TAG, "onActive: Value Event Listener for $reference attached")
        reference.addValueEventListener(listener)
    }


    //if the fragment/activity is not active, remove the ValueEventListener to the db reference
    override fun onInactive() {
        super.onInactive()
        Log.d(TAG, "onInactive: Value Event Listener for $reference detached")
        reference.removeEventListener(listener)


    }


}
