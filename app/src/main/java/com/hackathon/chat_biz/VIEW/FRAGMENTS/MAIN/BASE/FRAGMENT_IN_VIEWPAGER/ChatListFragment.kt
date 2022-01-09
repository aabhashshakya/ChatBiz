package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEWMODEL.ChatListViewModel
import com.hackathon.chat_biz.databinding.FragmentChatListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "ChatListFragment"

@AndroidEntryPoint
class ChatListFragment @Inject constructor() : Fragment(R.layout.fragment_chat_list) {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var auth: FirebaseAuth
    @Inject
    lateinit var db: FirebaseDatabase

    val chatListViewModel: ChatListViewModel by viewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        _binding = FragmentChatListBinding.bind(view)


        val chatListAdapter = ChatListAdapter(db, auth)

        //recycler view stuff
        binding.chatListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatListAdapter


        }

        //getting the chat list of people that we have messaged
        chatListViewModel.retrieveChatListOfUsers().observe(viewLifecycleOwner) {
            Log.d(TAG, "onViewCreated: Chat List Of Users updated")
            binding.chatlistTextview.isVisible = it.size < 1
            chatListAdapter.submitList(it.toList())

        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}