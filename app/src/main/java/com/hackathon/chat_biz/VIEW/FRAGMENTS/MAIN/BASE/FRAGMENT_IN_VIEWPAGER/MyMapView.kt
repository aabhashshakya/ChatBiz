package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.google.android.gms.maps.MapView

//WE HAVE IMPLEMENTED A CUSTOM MAP VIEW AS WE HAVE PUT THIS MAPVIEW INSIDE A FRAGMENT THAT IS PART OF A VIEWPAGER2
//SO SWIPING ON THE MAP< THE VIEWPAGER2 GETS THE TOUCH EVENT FIRST AND INSTEAD SWIPES BETWEEN THE FRAGMENTS, INSTEEAD OF SWIPING IN THE MAP
//WE WANT TO MAKE IT SO THAT THE MAPVIEW GETS SWIPED WHEN TOUCHE, NOT THE VIEWPAGER2
//WE NEED TO ONLY DO THIS AS OUR MAPVIEW IS INSIDE OF A VIEWPAGER2

private const val TAG = "MyMapView"

class MyMapView(context: Context, attr: AttributeSet) : MapView(context, attr) {
    //MAKE SURE TO USE BOTH CONSTRUCTORS context and attributeset, IT IS NECESSARY
    //NO NEED TO INSTANTIATE THIS CLASS IN THE FRAGMENT, IT IS DONE AUTOMATICALLY

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.d(TAG, "onInterceptTouchEvent: MapView overrode the viewpager touch")
        //this means when mapview is touched, the parent(viewpager) doesn't get the touch signal
        //this way the map can be manipulated
        parent.requestDisallowInterceptTouchEvent(true)
        return super.onInterceptTouchEvent(ev)
    }


}