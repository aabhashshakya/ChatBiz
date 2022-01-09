package com.hackathon.chat_biz.VIEWMODEL

import android.net.Uri
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.hackathon.chat_biz.MODEL.Chat
import com.hackathon.chat_biz.MODEL.User
import com.hackathon.chat_biz.REPOSITORY.FirebaseQueryLiveData
import com.hackathon.chat_biz.REPOSITORY.MessageRepository
import com.hackathon.chat_biz.VIEW.CHATS
import com.hackathon.chat_biz.VIEW.USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase


private const val TAG = "MessageViewModel"

class MessageViewModel @ViewModelInject constructor(
    private val repository: MessageRepository,
    private val db: FirebaseDatabase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private fun getChatUID(receiverID: String): String {
        return if (auth.currentUser!!.uid < receiverID) {
            auth.currentUser!!.uid + " + " + receiverID
        } else
            receiverID + " + " + auth.currentUser!!.uid

    }

    //send text message to user
    fun sendMessageToUser(message: String, receiverUserID: String) {
        //a unique chat ID for this conversation
        repository.sendMessageToUser(message, receiverUserID, getChatUID(receiverUserID))

    }

    //upload image to firebase storage and then db (send image message to user)
    fun sendImageMessage(imageUri: Uri?, receiverUserID: String) {
        repository.sendImageMessage(imageUri, receiverUserID, getChatUID(receiverUserID))
    }

    //to get all the messages
    fun retrieveMessages(
        receiverID: String

    ): LiveData<ArrayList<Chat>> {

        val firebaseQueryLiveData =
            FirebaseQueryLiveData(db.getReference(CHATS).child(getChatUID(receiverID)))
        //transformations are used in ViewModel if we want to deal with LiveData in ViewModel
        //since we cannot observe LiveData in ViewModel as there is no observer lifecycle, we used Transformation
        //as we can see here we user transformations to transform one livedata(DataSnapshot) to another type of livedata(ArrayList<Chat>)
        //now the fragment can observe this new livedata easily
        //the fragment could also have observed the original(DataSnapshot) livedata but we would have to do the work of getting the
        //value from snapshot(we do that here) in fragment(not ideal) so we just simply transformed that livedata into a livedata
        //the fragment can use easily
        val liveChatList: LiveData<ArrayList<Chat>> =
            Transformations.switchMap(firebaseQueryLiveData) {
                Log.d(
                    TAG,
                    "retrieveMessages: Message history for ${getChatUID(receiverID)} is empty: ${!it.exists()}"
                )
                val chatList = ArrayList<Chat>()
                if (it.exists()) {

                    for (data in it.children) {
                        val chat = data.getValue(Chat::class.java)
                        if (chat != null) {
                            chatList.add(chat)
                        }

                    }
                }
                //this new livedata that is coupled with the original livedata//so if that changes, this changes
                MutableLiveData<ArrayList<Chat>>(chatList)
            }

        return liveChatList


    }

    fun retrieveReceiverInfo(
        receiverUserID: String
    ): LiveData<User> {
        val firebaseQueryLiveData =
            FirebaseQueryLiveData(db.getReference(USERS).child(receiverUserID))
        //transformations are used in ViewModel if we want to deal with LiveData in ViewModel
        //since we cannot observe LiveData in ViewModel as there is no observer lifecycle, we used Transformation
        //as we can see here we use transformations to transform one livedata(DataSnapshot) to another type of livedata(<User>)
        //now the fragment can observe this new livedata easily
        //the fragment could also have observed the original(DataSnapshot) livedata but we would have to do the work of getting the
        //value from snapshot(we do that here) in fragment(not ideal) so we just simply transformed that livedata into a livedata the
        // fragment can use easily
        val liveUserInfo: LiveData<User> = Transformations.switchMap(firebaseQueryLiveData) {

            val user = it.getValue(User::class.java)
            //this new livedata that is coupled with the original livedata//so if that changes, this changes
            MutableLiveData<User>(user)
        }
        return liveUserInfo

    }


    fun deleteMessage(messageToDelete: Chat, receiverID: String) {
        repository.deleteMessage(messageToDelete, getChatUID(receiverID))

    }


    fun markMessagesAsSeen(receiverUserID: String): LiveData<DataSnapshot> {
        return FirebaseQueryLiveData(db.getReference(CHATS).child(getChatUID(receiverUserID)))


    }


}