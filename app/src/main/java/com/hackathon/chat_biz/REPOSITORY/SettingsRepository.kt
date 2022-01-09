package com.hackathon.chat_biz.REPOSITORY

import android.net.Uri
import android.util.Log
import com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER.SettingsFragment
import com.hackathon.chat_biz.VIEW.USERS
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import javax.inject.Inject

private const val TAG = "Settings Repository"

class SettingsRepository @Inject constructor(
    val auth: FirebaseAuth,
    val db: FirebaseDatabase,
    val storage: FirebaseStorage
) {


    fun updateUserInfo(nodeToUpdate: String, newInfo: String) {

        db.getReference(USERS).child(auth.currentUser!!.uid).child(nodeToUpdate).setValue(newInfo)

    }

    //Uploading the image in our Firebase Storage
    fun uploadImageToFirebaseStorage(imageUri: Uri?, pictureToPick: Int) {


        //this will be the image file name
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        //this is the place where we want to store, not that the child is the filename
        val storageReference = storage.getReference(auth.currentUser!!.uid).child(fileName)

        if (imageUri != null) {
            //this is how to upload a file to the Firebase Storage
            val uploadImage = storageReference.putFile(imageUri)

            //continuewithTask: Returns a new Task that will be completed with the result of applying the specified Continuation
            // to this Task. The Continuation will be called on the main application thread.
            uploadImage.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> {
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

                if (pictureToPick == SettingsFragment.PROFILE_PIC_PICK) {
                    //upload the image to the database
                    db.getReference(USERS).child(auth.currentUser!!.uid).child("profilePic")
                        .setValue(downloadUrl)

                }
                if (pictureToPick == SettingsFragment.COVER_PIC_PICK) {
                    //upload the image to the database
                    db.getReference(USERS).child(auth.currentUser!!.uid).child("coverPic")
                        .setValue(downloadUrl)
                    //NO NEED TO UPDATE THE VIEWS AS WE ADDED valueEventListener() ABOVE TO ALL VIEWS// SO UPDATED AUTOMATICALLY WHEN
                    //DB IS CHANGED

                }
            }

        }

    }
}