package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER.ChatListFragment
import com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER.SearchFragment
import com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER.SettingsFragment

//This adapter is for the ViewPager
class ViewPagerAdapter constructor(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    //Here we assign the order of the fragments in out ViewPager
    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> ChatListFragment()
            1 -> SearchFragment()
            else -> SettingsFragment()
        }


    override fun getItemCount(): Int {
        return 3 //as we have 3 fragments
    }
}