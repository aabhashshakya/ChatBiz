package com.hackathon.chat_biz.HILT

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp


//This is required for working HILT. You know it dawg

//We implemented the LfeCycleObserver to know if our app is in the foreground or background
//we need to check this to display notification in MyFirebaseMessaging service

private const val TAG = "Application"

@HiltAndroidApp
class MyApplication : Application(), LifecycleObserver {


    companion object {
        var isInBackground: Boolean? = null
    }

    override fun onCreate() {
        super.onCreate()

        //adding as a lifecycle observer to observe the below events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)




    }




    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onMoveToForeground() {
        // app moved to background
        Log.d(TAG, "onMoveToForeground: true ")
        isInBackground = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onMoveToBackground() {
        // app moved to background
        Log.d(TAG, "onMoveToBackground: true ")
        isInBackground = true
    }
}