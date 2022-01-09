package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER

import android.util.Log
import android.view.LayoutInflater
import android.view.View.*
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.hackathon.chat_biz.MODEL.Chat
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEW.IMAGE_MESSAGE
import com.hackathon.chat_biz.databinding.AdapterLeftMessageHolderBinding
import com.hackathon.chat_biz.databinding.AdapterRightMessageHolderBinding
import com.bumptech.glide.Glide

private const val TAG = "Message Adapter"

//WE USE RECYCLER VIEW THAT INFLATES TWO DIFFERENT VIEWS BASED ON CERTAIN CONDITIONS defined in getItemViewType()
//since we have used two view holder, we have set the RecyclerView.ViewHolder (parent class) as the generic, as we cannot set
//two view holder in the generic, this would mean we have to CAST the view holder to the right one when performing operations below


class MessageAdapter(
    private val receiverUserID: String,
    private val receiverProfilePic: String

) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var messagesList = ArrayList<Chat>()

    companion object {
        const val RIGHT_SIDE_VIEW_HOLDER = 0
        const val LEFT_SIDE_VIEW_HOLDER = 1

    }


    //this determines, under which condition which view to inflate
    //as we do here, we check if we are the send in each of the chat object, if so we inflate the right_message_view (we sent a message)
    //else we inflate the left_message_view (we received a message)
    override fun getItemViewType(position: Int): Int {
        //SO THIS MEANS IF THE MESSAGE HAS THE SAME SENDER ID AS US, WE SENT THAT MESSAGE
        //SO THAT MESSAGE SHOULD BE DISPLAYED IN THE RIGHT HAND SIDE VIEW
        return if (messagesList[position].senderID == receiverUserID) {
            LEFT_SIDE_VIEW_HOLDER
        } else {
            //THIS MEANS WE RECEIVED THE MESSAGE SO WE DISPLAY IT IN LEFT HAND SIDE VIEW
            RIGHT_SIDE_VIEW_HOLDER
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        //BASICALLY WE LOAD DIFFERENT VIEW HOLDER TO THE RECYCLERVIEW BASED ON THE PARAMETERS DEFINED IN getItemViewType ABOVE
        return if (viewType == RIGHT_SIDE_VIEW_HOLDER) {
            //for loading the sent message(right hand side view)
            val binding = AdapterRightMessageHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            RightMessageViewHolder(binding)
        } else {   //for loading the received message(left hand side view)
            val binding = AdapterLeftMessageHolderBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            LeftMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        //MAKE SURE YOU USE IF/ELSE CONDITION AND SET ALL THE POSSIBILITIES FOR A VIEW TO BE GONE/VISIBLE. BECAUSE THE RECYCLERVIEW
        //RECYCLES THE VIEWS AND IF YOU, FOR EXAMPLE, DON'T MAKE A VIEW VISIBLE AFTER SETTING IT TO INVISIBLE, UPON SCROLLING EVERYTHING
        //GETS FUCKED UP
        //IF ALL THE VIEWS ARE SET TO VISIBLE(DEFAULT) AND YOU DON'T NEED TO TOGGLE THEIR VISIBILITY(not our case), EVERYTHING IS FINE,
        // BUT ONCE YOU TOGGLE THEIR VISIBILTY UPON CERTAIN CONDITION (our case) ALWAYS TOGGLE THEM BACK TO DEFAULT UPON THAT CONDITION NOT
        //MET//SEE BELOW


        when (holder.itemViewType) {

            //for the messages that we send
            RIGHT_SIDE_VIEW_HOLDER -> {
                (holder as RightMessageViewHolder).apply {

                    //for image messages
                    if (messagesList[position].textMessage == IMAGE_MESSAGE && messagesList[position].imageUrl.isNotEmpty()) {
                        Glide.with(this.binding.root).load(messagesList[position].imageUrl)
                            .timeout(60000)
                            .error(R.drawable.coverimage)
                            .placeholder(R.drawable.coverimage)
                            .into(this.binding.rightImageView)
                        Log.d(TAG, "onBindViewHolder: Image message is loaded")
                        this.binding.rightMessageCardView.visibility = GONE
                        //CONTINUING ON ABOVE RANT, YOU MIGHT THINK WHY TO SET THE IMAGECARDVIEW TO VISIBLE HERE AS IT ALREADY VISIBLE
                        //BY DEFAULT AND THIS IS THE FIRST IF CONDITION RIGHT, WRONG!!!! // IF THIS CONDITION IS NOT MET, THE ELSE
                        //STATEMENT BELOW ACTUALLY SET THE VIEW TO GONE AND WHEN RECYCLER VIEW RECYCLER THE VIEWS WHEN SCROLLING, UR
                        //IMAGECARDVIEW IS NOT VISIBLE and EVERYTHING IS FUCKED WHEN SCROLLING. SO ALWAYS SET VIEW TO VISIBLE/INVISIBLE
                        //ACCOUNTING FOR ALL CASES
                        this.binding.rightImageCardView.visibility = VISIBLE

                    } else {
                        //for text message
                        if (messagesList[position].textMessage.isNotEmpty() && messagesList[position].textMessage != "") {
                            this.binding.rightMessageCardView.visibility = VISIBLE
                            this.binding.rightMessageTextView.text =
                                messagesList[position].textMessage
                            this.binding.rightImageCardView.visibility = GONE
                        }
                    }

                    //for SEEN message//we show if our message was the last message

                    if (position == messagesList.lastIndex) {
                        this.binding.rightSeenTextView.visibility = VISIBLE


                        Log.d(TAG, "onBindViewHolder: Seen status : ${messagesList[position].seen}")
                        if (messagesList[position].seen) {
                            this.binding.rightSeenTextView.text = "Seen"
                        } else
                            this.binding.rightSeenTextView.text = "Sent"


                    } else {
                        this.binding.rightSeenTextView.visibility = GONE
                    }
                }


            }

            //for the messages that we receive
            LEFT_SIDE_VIEW_HOLDER -> {
                (holder as LeftMessageViewHolder).apply {

                    //for loading the profile pic

                    Glide.with(binding.root).load(receiverProfilePic)
                        .error(R.drawable.ic_profile)
                        .placeholder(R.drawable.ic_profile)
                        .into(binding.leftReceiverProfilePic)


                    //we don't load the profile pic of the person on the left every if they send a CONSECUTIVE MESSAGE
                    //AGAIN SEE, ACCOUNTING FOR ALL THE CASES WHERE THE VIEW NEEDS TO BE VISIBLE/INVISIBLE
                    if (holder.adapterPosition == 0) {
                        this.binding.leftReceiverProfilePic.visibility = VISIBLE
                    } else {
                        if (messagesList[position].senderID == messagesList[position - 1].senderID) {
                            this.binding.leftReceiverProfilePic.visibility = INVISIBLE
                        } else {
                            this.binding.leftReceiverProfilePic.visibility = VISIBLE
                        }
                    }

                    //for image messages
                    if (messagesList[position].textMessage == IMAGE_MESSAGE && messagesList[position].imageUrl.isNotEmpty()) {

                        Glide.with(this.binding.root).load(messagesList[position].imageUrl)
                            .timeout(60000)
                            .error(R.drawable.coverimage)
                            .placeholder(R.drawable.coverimage)
                            .into(this.binding.leftImageView)
                        this.binding.leftMessageCardView.visibility = GONE
                        this.binding.leftImageCardView.visibility = VISIBLE


                    } else {
                        //for text message
                        if (messagesList[position].textMessage.isNotEmpty() && messagesList[position].textMessage != "") {
                            this.binding.leftMessageCardView.visibility = VISIBLE
                            this.binding.leftMessageTextView.text =
                                messagesList[position].textMessage
                            this.binding.leftImageCardView.visibility = GONE
                        }

                    }


                }


            }
        }
    }


    //view holder for inflating the view of the messages that we RECEIVE
    inner class LeftMessageViewHolder(val binding: AdapterLeftMessageHolderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        //adding on click listener for imageviews
        init {
            binding.leftImageCardView.setOnClickListener {
                //navigating to full image view fragment
                val action = MessageFragmentDirections
                    .actionMessageFragmentToFullImageViewFragment(messagesList[adapterPosition].imageUrl)
                it.findNavController().navigate(action)

            }


            //handling click events for profile pic click/each click takes us to that users profile
            binding.leftReceiverProfilePic.setOnClickListener {

                Log.d(TAG, "We are visiting id : $receiverUserID")

                val action =
                    MessageFragmentDirections.actionMessageFragmentToProfileFragment(receiverUserID)
                it.findNavController().navigate(action)
            }
        }


    }

    //view holder for inflating the view of the messages that we SEND
    inner class RightMessageViewHolder(val binding: AdapterRightMessageHolderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        //adding on click listener for imageview
        init {
            binding.rightImageCardView.setOnClickListener {
                //navigating to full image view fragment
                val action = MessageFragmentDirections
                    .actionMessageFragmentToFullImageViewFragment(messagesList[adapterPosition].imageUrl)
                it.findNavController().navigate(action)

            }
        }


    }

    override fun getItemCount(): Int {
        return messagesList.size
    }


}