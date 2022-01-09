package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hackathon.chat_biz.MODEL.Chat
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEWMODEL.MessageViewModel
import com.hackathon.chat_biz.databinding.FragmentMessageBinding
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint


private const val TAG = "MessageFragment"

@AndroidEntryPoint
class MessageFragment : Fragment(R.layout.fragment_message) {


    companion object {
        const val REQUEST_CODE = 23
        private const val STORAGE_PERMISSION = 12
    }


    private val navArgs by navArgs<MessageFragmentArgs>()
    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!


    val messageViewModel: MessageViewModel by viewModels()


    private var receiverUserID: String? = null


    private lateinit var messageAdapter: MessageAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMessageBinding.bind(view)

        binding.messageToolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)


        binding.messageToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }


        //saving the receiver's user ID from the NavArgs
        receiverUserID = navArgs.receiverId

        messageAdapter = MessageAdapter(navArgs.receiverId!!, navArgs.receiverProfilePic!!)
        binding.messageRecyclerView.apply {
            //setting the adapter for the recycler view
            val linearLayoutManager = LinearLayoutManager(context)
            //this means the recycle view is filled from the bottom
            linearLayoutManager.stackFromEnd = true


            this.layoutManager = linearLayoutManager

            this.adapter = messageAdapter

            //we implement this so that a swipe of a message deletes the message
            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    Log.d(TAG, "Recycler view item moved.")
                    return true

                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    //we need to do this check as only the sender can delete his/her messages
                    if (viewHolder is MessageAdapter.RightMessageViewHolder) {

                        val adapterPosition = viewHolder.adapterPosition
                        val messageToBeDeleted =
                            messageAdapter.messagesList[adapterPosition]
                        //warning to user
                        //We use ContextThemeWrapper as we want to use a theme in our Alert Dialog
                        AlertDialog.Builder(
                            ContextThemeWrapper(
                                context,
                                R.style.MyAlertDialogStyle
                            )
                        )
                            .setTitle("Are you sure you want to delete this message?")
                            .setMessage("This deletes the message for EVERYONE and is IRREVERSIBLE")
                            .setPositiveButton(
                                "CONFIRM"
                            ) { _, _ ->
                                //if user choose to delete the message
                                messageViewModel.deleteMessage(
                                    messageToBeDeleted,
                                    receiverUserID!!

                                )
                            }
                            //if user cancels the deletion
                            .setNegativeButton(
                                "CANCEL"
                            ) { dialog, _ ->
                                dialog.dismiss()
                                //we need to provide the position where the item was swiped
                                //as swiping removes the item from the recyclerview
                                //adding this means that we notify the recycler view that item was changed so it can load it again
                                messageAdapter.notifyItemChanged(viewHolder.adapterPosition)
                            }.show()


                    }
                }


            }).attachToRecyclerView(this)


            //displaying the receiver's username and profile pic

            binding.messageReceiverName.text = navArgs.receiverName
            Glide.with(binding.messageToolbar).load(navArgs.receiverProfilePic)
                .error(R.drawable.ic_profile)
                .placeholder(R.drawable.ic_profile)
                .into(binding.messageReceiverProfilePic)


            //getting the messages
            messageViewModel.retrieveMessages(receiverUserID!!).observe(viewLifecycleOwner) {
                Log.d(TAG, "onViewCreated: New Messages list submitted")
                messageAdapter.messagesList = it
                //bad idea to use notifyDataSetChanged() but DiffUtil doesn't refresh the recycler view until the contents within the
                //list change.. and sending a new message doesn't change the content of the previous message so the SEEN/SENT view is
                //displayed for both the previous and new message until recycler view is scrolled. that's why we used notifyDataSetChanged()
                messageAdapter.notifyDataSetChanged()
                binding.messageRecyclerView.scrollToPosition(it.lastIndex)
            }


            //on click listener for the profile pic//click takes us to that users profile
            binding.messageReceiverProfilePic.setOnClickListener {

                Log.d(TAG, "We are visiting id : $receiverUserID")

                val action =
                    MessageFragmentDirections.actionMessageFragmentToProfileFragment(
                        receiverUserID!!
                    )
                it.findNavController().navigate(action)


            }
        }


        //when send button clicked, we need to send the message
        binding.messageSend.setOnClickListener {

            val message = binding.messageEditText.text.toString()
            if (message.isNotEmpty()) {
                //if no internet, we show an alert dialog //we used the extension function we created observeOnce()
                if (!checkIfNetworkIsConnected()) {

                    AlertDialog.Builder(
                        ContextThemeWrapper(
                            context,
                            R.style.MyAlertDialogStyle
                        )
                    )
                        .setMessage("Your message will be delivered when you are connected to a network")
                        .setPositiveButton(
                            "OK"
                        )
                        { dialog, _ ->
                            dialog.cancel()

                        }.show()
                }


                //WE SEND THE MESSAGE && NOTIFICATION
                messageViewModel.sendMessageToUser(message.trim(), receiverUserID!!)
                binding.messageEditText.setText("")
                binding.messageEditText.clearFocus()


            }

        }

        //when user click attach file, we do this
        binding.messageAttachFile.setOnClickListener {

            //first checking for strorage permissions
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                askForPermission()
            } else {
                val intent = Intent().apply {
                    this.type = "image/*"
                    this.action = Intent.ACTION_GET_CONTENT
                }
                startActivityForResult(
                    Intent.createChooser(intent, "Pick an image"),
                    REQUEST_CODE
                )

            }


        }


    }

    override fun onResume() {
        super.onResume()
        //when we open this fragment, the messages that we get need to be marked as seen
        messageViewModel.markMessagesAsSeen(receiverUserID!!).observe(viewLifecycleOwner) {
            if (it.exists()) {
                for (messageSnapshot in it.children) {
                    val message = messageSnapshot.getValue(Chat::class.java)

                    if (message!!.senderID == receiverUserID) {
                        //ONLY THE MESSAGES THAT WE GET, ARE MARKED AS SEEN
                        val hashMap = HashMap<String, Boolean>()
                        hashMap["seen"] = true
                        //updating the value of this snapshot//message is seen
                        messageSnapshot.ref.updateChildren(hashMap.toMap())

                    }


                }
            }
        }
    }


    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            //WE UPLOAD THE IMAGE TO STORAGE AND DB && SEND NOTIFICATION

            //if no internet, we show an alert dialog
            if (!checkIfNetworkIsConnected()) {
                AlertDialog.Builder(ContextThemeWrapper(context, R.style.MyAlertDialogStyle))
                    .setMessage("Please connect to a network before sending a message")
                    .setPositiveButton(
                        "OK"
                    )
                    { dialog, _ ->
                        dialog.cancel()

                    }.show()

            } else {
                messageViewModel.sendImageMessage(data.data, receiverUserID!!)
                Toast.makeText(context, "Uploading your image...", Toast.LENGTH_LONG).show()
            }


        }


    }


    //permission stuffs
    private fun askForPermission() {

        if (shouldShowRequestPermissionRationale(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            AlertDialog.Builder(ContextThemeWrapper(context, R.style.MyAlertDialogStyle))
                .setTitle("Permission needed")
                .setMessage("Storage permission is needed to upload the pictures.")
                .setPositiveButton("GRANT PERMISSION") { _, _ ->
                    //if clicked ok, it requests the permission
                    requestPermissions(
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        STORAGE_PERMISSION
                    )
                } //if clicked no, it dismisses the dialog
                .setNegativeButton(
                    "DECLINE"
                ) { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT)
                        .show()
                }.create().show()
        } else
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION
            )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if (requestCode == STORAGE_PERMISSION) {

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    context,
                    "Storage permissions granted.",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else
                Toast.makeText(
                    context,
                    "Storage permission not granted.",
                    Toast.LENGTH_SHORT
                )
                    .show()

        }


    }

    //chcek if the internet is connected
    private fun checkIfNetworkIsConnected(): Boolean {
        val cm =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //if android>M, this is how we check active internet connection
            val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            networkCapabilities != null
        } else {
            //if android<M, this is how we check active internet connection
            val activeNetwork = cm.activeNetworkInfo
            activeNetwork != null
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null


    }

}