package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.hackathon.chat_biz.MODEL.Chat
import com.hackathon.chat_biz.MODEL.User
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEW.CHATS
import com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.BaseFragmentDirections
import com.hackathon.chat_biz.databinding.AdapterChatListBinding
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private const val TAG = "ChatList Adapter"


class ChatListAdapter(
    private val db: FirebaseDatabase,
    private val auth: FirebaseAuth

) :
    androidx.recyclerview.widget.ListAdapter<User, ChatListAdapter.MyViewHolder>(
        DIFF_UTIL
    ) {

    private var listener: ValueEventListener? = null
    private var chatUID: String? = null

    companion object {

        val DIFF_UTIL = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.userID == newItem.userID
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }

        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatListAdapter.MyViewHolder {
        val binding =
            AdapterChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)


    }

    override fun onBindViewHolder(holder: ChatListAdapter.MyViewHolder, position: Int) {
        val user = getItem(position)
        holder.binding.apply {
            this.username.text = user.username
            Glide.with(this.root).load(user.profilePic).timeout(60000).error(R.drawable.ic_profile)
                .placeholder(R.drawable.ic_profile).into(this.profileImage)
        }

        Log.d(TAG, "onBindViewHolder: " + user.online)
        if (user.online) {

            holder.binding.onlinestatus.isVisible = true
            holder.binding.offlinestatus.isVisible = false
        } else {
            holder.binding.onlinestatus.isVisible = false
            holder.binding.offlinestatus.isVisible = true
        }

        //FOR RETRIEVING THE UNREAD MESSAGES & LAST MESSAGE
        //this is how we stored the chat for each conversation in the database
        chatUID = if (auth.currentUser!!.uid < user.userID) {
            auth.currentUser!!.uid + " + " + user.userID
        } else
            user.userID + " + " + auth.currentUser!!.uid



        listener = db.getReference(CHATS).child(chatUID!!).addValueEventListener(object :
            ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    var lastMessage = Chat()
                    var unreadCount = 0
                    for (data in snapshot.children) {
                        val message = data.getValue(Chat::class.java)
                        if (message!!.receiverID == auth.currentUser!!.uid && !message.seen) {
                            unreadCount += 1
                            //the last message is last message sent by either one of us

                        }
                        lastMessage = message

                    }
                    //FOR DISPLAYING UNREAD MESSAGES COUNT
                    if (unreadCount < 1) {
                        holder.binding.unreadMessages.text = ""
                        holder.binding.unreadMessages.isVisible = false
                    } else {
                        holder.binding.unreadMessages.text = unreadCount.toString()
                        holder.binding.unreadMessages.isVisible = true

                    }
                    //FOR DISPLAYING LAST MESSAGES
                    if (lastMessage.senderID == auth.currentUser!!.uid) {
                        holder.binding.lastMessage.text = "You: ${lastMessage.textMessage}"

                    } else {

                        holder.binding.lastMessage.text = lastMessage.textMessage
                    }

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d(TAG, "onCancelled: ${error.toException().printStackTrace()}")
            }
        })

    }


    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        db.getReference(CHATS).child(chatUID!!).removeEventListener(listener!!)

    }

    //view holder for inflating the view of the messages that we SEND
    inner class MyViewHolder(val binding: AdapterChatListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {

            //handling click events for each user in the recycler view
            binding.root.setOnClickListener {
                val id = getItem(adapterPosition).userID
                val profilePicUrl = getItem(adapterPosition).profilePic
                val receiverName = getItem(adapterPosition).username
                Log.d(TAG, "We are going to message id : $id")

                //we are using BaseFragmentDirections even though we know this adapter's recycler view is in ChatFragment, this is
                //because ChatFragment is inside of a TabLayout and the Navigation component doesn't count fragments inside a TabLayout
                //as destination(or source) as they don't have a back stack
                //so we CANNOT use fragments inside of a tab layout in nav_graph
                //SO WE USE THE FRAGMENT THAT CONTAINS THE TAB LAYOUT i.e BaseFragment for navigation
                val action = BaseFragmentDirections.actionBaseFragmentToMessageFragment(
                    id,
                    profilePicUrl,
                    receiverName
                )
                it.findNavController().navigate(action)
            }

            //on click listener for profile pic
            binding.profileImage.setOnClickListener {
                //navigating to full image view fragment
                val action = BaseFragmentDirections
                    .actionBaseFragmentToProfileFragment(getItem(adapterPosition).userID)
                it.findNavController().navigate(action)

            }


        }


    }


}