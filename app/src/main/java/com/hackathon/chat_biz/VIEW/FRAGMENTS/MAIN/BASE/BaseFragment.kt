package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE


import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER.SettingsFragment
import com.hackathon.chat_biz.VIEW.USERS
import com.hackathon.chat_biz.VIEWMODEL.BaseViewModel
import com.hackathon.chat_biz.databinding.FragmentBaseBinding
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject


const val TAG = "Base Fragment"

@AndroidEntryPoint
class BaseFragment : Fragment(R.layout.fragment_base) {

    private var _binding: FragmentBaseBinding? = null
    private val binding get() = _binding!!



    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var db: FirebaseDatabase


    val baseViewModel: BaseViewModel by viewModels()

    private lateinit var viewPagerAdapter: ViewPagerAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        _binding = FragmentBaseBinding.bind(view)

        //hiding the action bar
        (activity as AppCompatActivity).supportActionBar?.hide()

        //setting up menu in toolbar
        binding.toolbarMain.inflateMenu(R.menu.menu_logout)
        binding.toolbarMain.setOnMenuItemClickListener {
            //log out the user
            if (it.itemId == R.id.logout_button_menu) {
                baseViewModel.logOutUser()
                Log.d(TAG, "onViewCreated: User has been logged out")
                findNavController().navigate(
                    R.id.welcomeFragment, null,
                    NavOptions.Builder().setPopUpTo(R.id.base_fragment, true).build()
                )

            }
            true
        }


        //configuring the ViewPager
        viewPagerAdapter =
            ViewPagerAdapter(childFragmentManager, lifecycle)//instantiating our view pager adapter
        //always use childFragmentManager here

        binding.viewPager.adapter = viewPagerAdapter //setting up the adapter for our ViewPager2

        //setting up out Tab Layout with ViewPager
        //We use TabLayoutMediator to set up our tabs title, icon,etc here
        //We passed our tabLayout, viewPager2 and a TabConfigurationStrategy here
        TabLayoutMediator(
            binding.tabLayout, binding.viewPager
        ) { tab, position ->
            when (position) {
                0 -> tab.text = "CHAT"
                1 -> tab.setText("SEARCH")
                2 -> tab.setText("SETTINGS")

            }
        }.attach()


        //ADD USER TO DATABASE IF NOT ALREADY//IF ALREADY EXISTS, IT WONT ADD
        baseViewModel.addUserToDatabase()


        //LOADING USER INFO IN THE TOOLBAR
        baseViewModel.loadUserProfile().observe(viewLifecycleOwner) {
            if (it != null) {
                binding.userName.text = it.username
                Glide.with(binding.root).load(it.profilePic).timeout(60000).error(R.drawable.ic_profile)
                    .placeholder(R.drawable.ic_profile).into(binding.profileImage)

                //on click listener for profile pic
                binding.profileImage.setOnClickListener { view ->
                    //navigating to full image view fragment
                    val action = BaseFragmentDirections
                        .actionBaseFragmentToProfileFragment(it.userID)
                    view.findNavController().navigate(action)

                }
            }
        }

        //every device has a unique token sent by the firebase. we need this token to send cloud messages(notification)
        //from firebase
        baseViewModel.saveFirebaseToken()


        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                //FAB button is only visible on the first page of the view pager
                binding.fab.isVisible = position == 0
            }

        })


        //on click listener on FAB button//go to next fragment in the viewpager
        binding.fab.setOnClickListener {
            binding.viewPager.setCurrentItem(binding.viewPager.currentItem + 1, true)
        }


    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            Log.d("LOCATION", "LOCATION CALL EXECUTED")
            try{
                val mLocationRequest = LocationRequest.create()
                mLocationRequest.interval = 10000
                mLocationRequest.fastestInterval = 3000
                mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                val mLocationCallback: LocationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        if (locationResult.equals(null)) {
                            Log.d("LOCATION", "LOCATION NO LOCATON")
                            return
                        }
                        for (location in locationResult.locations) {
                            if (location != null) {
                                //update the user info in database
                                Log.d("LOCATION", "SD"+location.latitude)
                                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                                val result = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                if (result.isNotEmpty()) {
                                    val address = result[0]
                                    var place = ""
                                    for (i in 0..address.maxAddressLineIndex) {
                                        place += address.getAddressLine(i) + ""
                                    }
                                    place += "latlng ${location.latitude}:${location.longitude}"
                                    if (auth.currentUser != null) {
                                        db.getReference(USERS).child(auth.currentUser!!.uid)
                                            .child("location").setValue(place)
                                    }


                                }
                            }
                        }
                    }
                }

                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    askForLocationPermission()
                    return
                }
                LocationServices.getFusedLocationProviderClient(requireContext())
                    .requestLocationUpdates(mLocationRequest, mLocationCallback,  Looper.getMainLooper())
            }
            catch (e: Exception){
                Log.e(TAG, e.stackTrace.toString())

            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun askForLocationPermission() {
        if (shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            AlertDialog.Builder(ContextThemeWrapper(requireContext(), R.style.MyAlertDialogStyle))
                .setTitle("Permission needed")
                .setMessage("Location permission is needed to access your current location")
                .setPositiveButton("GRANT PERMISSION") { _, _ ->
                    //if clicked ok, it requests the permission
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        1
                    )
                } //if clicked no, it dismisses the dialog
                .setNegativeButton(
                    "DECLINE"
                ) { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
                }.create().show()
        } else
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
    }


    override fun onResume() {
        super.onResume()
        //make user online if logged in
        baseViewModel.makeUserOnline()


    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (auth.currentUser != null) {
                    Log.d("LOCATION", "LOCATION CALL EXECUTED")
                    try {
                        val mLocationRequest = LocationRequest.create()
                        mLocationRequest.interval = 60000
                        mLocationRequest.fastestInterval = 5000
                        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                        val mLocationCallback: LocationCallback = object : LocationCallback() {
                            override fun onLocationResult(locationResult: LocationResult) {
                                if (locationResult.equals(null)) {
                                    Log.d("LOCATION", "LOCATION NO LOCATON")
                                    return
                                }
                                for (location in locationResult.locations) {
                                    if (location != null) {
                                        //update the user info in database
                                        Log.d("LOCATION", "SD" + location.latitude)
                                        val geocoder =
                                            Geocoder(requireContext(), Locale.getDefault())
                                        val result = geocoder.getFromLocation(
                                            location.latitude,
                                            location.longitude,
                                            1
                                        )
                                        if (result.isNotEmpty()) {
                                            val address = result[0]
                                            var place = ""
                                            for (i in 0..address.maxAddressLineIndex) {
                                                place += address.getAddressLine(i) + ""
                                            }
                                            place += "latlng ${location.latitude}:${location.longitude}"
                                            if (auth.currentUser != null) {
                                                db.getReference(USERS).child(auth.currentUser!!.uid)
                                                    .child("location").setValue(place)
                                            }


                                        }
                                    }
                                }
                            }
                        }
                        LocationServices.getFusedLocationProviderClient(requireContext())
                            .requestLocationUpdates(
                                mLocationRequest,
                                mLocationCallback,
                                Looper.getMainLooper()
                            )
                    } catch (e: Exception) {
                        Log.e(TAG, e.stackTrace.toString())

                    }


                }
            }


        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }




}


