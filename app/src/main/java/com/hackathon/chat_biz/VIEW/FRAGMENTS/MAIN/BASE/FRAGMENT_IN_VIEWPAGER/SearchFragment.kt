package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEWMODEL.SearchViewModel
import com.hackathon.chat_biz.databinding.FragmentSearchBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "SearchFragment"

@AndroidEntryPoint
class SearchFragment @Inject constructor() : Fragment(R.layout.fragment_search) {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val adapter = SearchAdapter()

    private val searchViewModel: SearchViewModel by viewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSearchBinding.bind(view)

        binding.searchRecyclerView.adapter = adapter
        searchViewModel.getAllUsers(searchViewModel.getCurrentUserID())
            .observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    Log.d(TAG, "onViewCreated: All users adapter updated : List size: ${it.size}")

                    adapter.submitAllUserList(it.toList(), binding.searchView.hasFocus())
                    Log.d(
                        TAG,
                        "onViewCreated: SearchView has focus : ${binding.searchView.hasFocus()}"
                    )
                    //this filter is need as after process death/orientation change we want our list to be filterd if there was any text in
                    //the searchview, as the searchview's state is preserved
                    adapter.filter.filter(binding.searchView.query)
                    binding.searchProgressBar.isVisible = false


                }
                binding.searchProgressBar.isVisible = false
            }


        //we implement the searchview to search the items that we already queried from firebase db ABOVE
        binding.searchView.imeOptions =
            EditorInfo.IME_ACTION_DONE//changes the keyboards search button to tick
        //DO THIS IF YOU ARE USINg ONQUERYTEXTCHANGE INSTEAD OF SUBMIT


        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                //WE NEED TO IMPLEMENT FILTERABLE ON OUR ADAPTER FOR THIS TO WORK
                Log.d(TAG, "onQueryTextChange: User searched for a specific user: $newText")
                adapter.filter.filter(newText)

                //for some reason the recyclerview doesn't scroll to top when switching from filtered list to all users list(due to
                //DIFFUTIL)
                //so we give a 300ms delay and scroll manually

                Handler().postDelayed({
                    if (getView() != null) {

                        binding.searchRecyclerView.scrollToPosition(0)
                    }
                }, 300)
                return false
            }

        })


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}