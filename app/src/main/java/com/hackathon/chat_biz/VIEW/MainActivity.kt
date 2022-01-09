package com.hackathon.chat_biz.VIEW

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER.SettingsFragment
import com.hackathon.chat_biz.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject


private const val TAG = "Main Activity"

const val USERS = "Users"
const val CHATS = "Chats"
const val CHAT_LIST = "ChatList"
const val IMAGE_MESSAGE = "Sent a photo"
const val CHAT_IMAGES_STORAGE = "Chat Images"
const val TOKEN = "Token"

@AndroidEntryPoint

class MainActivity : AppCompatActivity() {


    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var db: FirebaseDatabase
    private lateinit var binding: ActivityMainBinding

    private var user: FirebaseUser? = null

    private val locationSwitchStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            print("Sdd : ${intent.action.toString()}")
            if (LocationManager.PROVIDERS_CHANGED_ACTION == intent.action) {
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

                if (isGpsEnabled) {
                    if (auth.currentUser != null) {
                        changeGpsStatus(true)
                    }

                } else {
                    if (auth.currentUser != null) {
                        changeGpsStatus(false)
                    }


                }
            }
        }
    }

    fun changeGpsStatus(on: Boolean) {
        //updating our status to online//only if the user is logged in
        if (auth.currentUser != null) {
            db.getReference(USERS).child(auth.currentUser!!.uid).child("gpson").setValue(on)
            //if internet disconnect, the user goes offline//it may take some time for firebase to update the data
            db.getReference(USERS).child(auth.currentUser!!.uid).child("gpson").onDisconnect()
                .setValue(false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //for preventing screenshots
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container) as NavHostFragment
        val navController = navHostFragment.navController
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)



        user = auth.currentUser
        if (user == null) {
            //if user not logged in goto welcome fragment

            navGraph.startDestination = R.id.welcomeFragment
            Log.d(TAG, "User: User is not logged in. Navigation to welcome fragment")

        } else {
            navGraph.startDestination = R.id.base_fragment
        }
        navController.graph = navGraph


        //just deleting the cache
        externalCacheDir?.deleteRecursively()



    }


    //THE STATUS ONLINE IS IN BASE FRAGMENTS's onResume() method
    override fun onPause() {
        super.onPause()
        //updating our status to online
        try{
        if (auth.currentUser != null) {
            db.getReference(USERS).child(auth.currentUser!!.uid).child("online").setValue(false)
            db.getReference(USERS).child(auth.currentUser!!.uid).child("gpson").setValue(false)

        }
        unregisterReceiver(locationSwitchStateReceiver)}
        catch (e: Exception){
            Log.e(TAG, "onPause: "+e.stackTrace.toString(), )
        }
    }



    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        //register br for GPS thingy
        try{
        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED)
        if (auth.currentUser != null) {
            val locationManager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            db.getReference(USERS).child(auth.currentUser!!.uid).child("gpson").setValue(isGpsEnabled)

        }
        registerReceiver(locationSwitchStateReceiver, filter)}
        catch(e:Exception){
            Log.e(TAG," Error registering broadcast manager")
        }

    }



}









