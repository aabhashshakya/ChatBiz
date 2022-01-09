package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hackathon.chat_biz.MODEL.User
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.BaseFragmentDirections
import com.hackathon.chat_biz.databinding.AdapterSearchBinding

import com.bumptech.glide.Glide

private const val TAG = "UserAdapter"

class SearchAdapter() : ListAdapter<User, SearchAdapter.MyViewHolder>(
    DIFF_UTIL
), Filterable {
    //we implement filterable as we also want to filter the results in the recycler view according to the search text
    companion object {
        val DIFF_UTIL = object : DiffUtil.ItemCallback<User>() {

            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                Log.d(TAG, "areItemsTheSame: ${oldItem.userID == newItem.userID}")
                return oldItem.userID == newItem.userID
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                Log.d(TAG, "areItemsTheSame: ${oldItem == newItem}")
                return oldItem == newItem
            }

        }


    }


    //we call submitList() from this user-generated method as we need to save the original all users list
    //this is because we are using Filterable and we need to load the original list when there is no search query in the searchview
    private var allUsersList: ArrayList<User>? = null

    //filtered list will be populated in getFilter() below
    private var filteredList: ArrayList<User>? = null
    fun submitAllUserList(allUsers: List<User>, searchViewActive: Boolean) {
        allUsersList = ArrayList(allUsers)
        if (!searchViewActive) {
            super.submitList(allUsersList)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val binding =
            AdapterSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)


    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val user = getItem(position)
        holder.binding.username.text = user.username
        Glide.with(holder.binding.root).load(user.profilePic).error(R.drawable.ic_profile)
            .placeholder(R.drawable.ic_profile)
            .into(holder.binding.profileImage)



        if (user.online) {
            holder.binding.onlinestatus.isVisible = true
            holder.binding.offlinestatus.isVisible = false

        } else {

            holder.binding.onlinestatus.isVisible = false
            holder.binding.offlinestatus.isVisible = true
        }

        holder.binding.bio.text = user.bio


    }

    inner class MyViewHolder(val binding: AdapterSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {

            //handling click events for each user in the recycler view//each click takes us to that users profile
            binding.root.setOnClickListener {
                val id = getItem(adapterPosition).userID
                Log.d(TAG, "We are visiting id : $id")

                //we are using BaseFragmentDirections even though we know this adapter's recycler view is in ChatFragment, this is
                //because ChatFragment is inside of a TabLayout and the Navigation component doesn't count fragments inside a TabLayout
                //as destination(or source) as they don't have a back stack
                //so we CANNOT use fragments inside of a tab layout in nav_graph
                //SO WE USE THE FRAGMENT THAT CONTAINS THE TAB LAYOUT i.e BaseFragment for navigation
                val action = BaseFragmentDirections.actionBaseFragmentToProfileFragment(id)
                it.findNavController().navigate(action)
            }


        }


    }

    //required for filtering data in the recycler view according to the search text
    override fun getFilter(): Filter {
        val filter = object : Filter() {
            //where we specify our filter criteria
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                filteredList = ArrayList<User>()

                //if the search bar is empty, we show the all users list
                if (constraint == null || constraint.isEmpty()) {
                    if (allUsersList != null) {
                        filteredList = ArrayList(allUsersList!!)
                    }

                } else {
                    //we compare the query with what we want, in our case the username
                    val searchQuery = constraint.toString().trim()
                    for (user in allUsersList!!) {
                        if (user.username.contains(searchQuery, true)) {
                            filteredList!!.add(user)
                        }
                    }

                }
                //return this so publishResults() can have the new list
                val filterResults = FilterResults()
                filterResults.values = filteredList!!.toList()
                return filterResults

            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {

                //null check
                if (results?.values != null) {
                    //submit the new list
                    submitList(results.values as List<User>)
                }


            }

        }
        //return filter so that we can access the filter on other classes(SearchFragment)
        return filter


    }
}