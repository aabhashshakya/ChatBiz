package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER


import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEWMODEL.ProfileViewModel
import com.hackathon.chat_biz.databinding.FragmentProfileBinding
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.hackathon.chat_biz.MODEL.User
import com.hackathon.chat_biz.NOTIFICATIONS.*
import com.hackathon.chat_biz.VIEW.TOKEN
import com.hackathon.chat_biz.VIEW.USERS
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject

private const val TAG = "Profile Fragment"

@AndroidEntryPoint
class ProfileFragment @Inject constructor(
) : Fragment(R.layout.fragment_profile) {

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var db: FirebaseDatabase

    @Inject
    lateinit var retrofit: Retrofit




    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!


    var googleMap: GoogleMap? = null

    val profileViewModel: ProfileViewModel by viewModels()


    val navArgs: ProfileFragmentArgs by navArgs()

    companion object {
        private const val LOCATION_PERMISSION = 89
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        binding.toolbar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        } //for showing the back button


        //if we are visiting our own profile, the send message button is gone
        binding.settingsSendMessage.isVisible =
            navArgs.userID != profileViewModel.getCurrentUserID()


        //for updating the ui with appropriate information
        profileViewModel.loadUserProfile(navArgs.userID).observe(viewLifecycleOwner) {
            binding.profileUsername.text = it?.username
            binding.profileBio.text =
                it?.bio ?: ""//means if bio is null, put nothing there
            binding.profileRole.text = it?.role ?: ""
            binding.profileAddress.text = it?.location?.substringBefore("latlng")
            Glide.with(binding.profileCardView).load(it?.profilePic)
                .timeout(60000)
                .error(R.drawable.ic_profile)
                .placeholder(R.drawable.ic_profile)
                .into(binding.profileProfilePic)
            Glide.with(binding.profileCardView).load(it?.coverPic)
                .timeout(60000)
                .error(R.drawable.coverimage)
                .placeholder(R.drawable.coverimage)
                .into(binding.profileCoverPic)
            if(it.gpson){
                binding.profileGpsIndicator.isEnabled = false
                binding.profileGpsIndicator.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_location_on_24))
            }else{
                binding.profileGpsIndicator.isEnabled = true
                binding.profileGpsIndicator.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_location_off_24))
                binding.profileGpsIndicator.setOnClickListener {
                    sendGpsRequestNotification(navArgs.userID) //send turn gps on notification

                }
                }
            if(it.online){
                binding.onlinestatus.setColorFilter(Color.parseColor("#05df29"))
            }else
            {
                binding.onlinestatus.setColorFilter(Color.parseColor("#636161"))

            }

            //to message that user
            binding.settingsSendMessage.setOnClickListener { _ ->
                val action =
                    ProfileFragmentDirections.actionProfileFragmentToMessageFragment(
                        navArgs.userID,
                        it.profilePic,
                        it.username
                    )
                findNavController().navigate(action)
            }

            //on click listener for profile pic
            binding.profileProfilePic.setOnClickListener { view ->

                //navigating to full image view fragment
                val action = ProfileFragmentDirections
                    .actionProfileFragmentToFullImageViewFragment(it?.profilePic)
                view.findNavController().navigate(action)

            }

            //on click listener for cover pic
            binding.profileCoverPic.setOnClickListener { view ->
                //navigating to full image view fragment
                val action = ProfileFragmentDirections
                    .actionProfileFragmentToFullImageViewFragment(it?.coverPic)
                view.findNavController().navigate(action)

            }


            binding.profileRole.setOnClickListener {
                //doing nothing lol


            }

            //when user clicks the address, the REAL Google Map opens
            binding.profileAddress.setOnClickListener { _ ->
                try {
                    val addressOfUser: String = it?.location.toString()
                    val latitude = addressOfUser.substringAfter("latlng ").run {
                        this.substringBefore(":").toDouble()
                    }
                    val longitude = addressOfUser.substringAfter(":").toDouble()
                    //opening google maps
                    //geo: means google map's camera will be fixed to that position,
                    //?q (query)means a marker will be placed at that postiion and google maps display info about that
                    //we put the query as the address name rather than coordinates, as what is query will be displayed by the map
                    //and we want to display name of the place, not the coordinates
                    val uri = String.format(
                        Locale.ENGLISH,
                        "geo:$latitude,$longitude?q=$latitude,$longitude"
                    )
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    requireContext().startActivity(intent)
                } catch (e: Exception) {
                    Log.d(TAG, "onViewCreated: Address to map: ${e.message} ${e.printStackTrace()}")
                    Toast.makeText(
                        context,
                        "This location cannot be viewed in Maps",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            }

            //for google maps
            try {
                val addressOfUser: String = it?.location.toString()
                val latitude = addressOfUser.substringAfter("latlng ").run {
                    this.substringBefore(":").toDouble()
                }
                val longitude = addressOfUser.substringAfter(":").toDouble()

                googleMap?.clear()
                googleMap?.addMarker(
                    MarkerOptions().position(
                        LatLng(
                            latitude,
                            longitude
                        )
                    )
                )
                googleMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            latitude,
                            longitude
                        ), 15.3F
                    )
                )
            } catch (e: Exception) {
                Log.d(TAG, "onViewCreated: Address to map: ${e.message} ${e.printStackTrace()}")
            }

        }

        binding.profileMapView.onCreate(savedInstanceState)
        binding.profileMapView.getMapAsync {

            Log.d(TAG, "onMapReady: Map is not ready")
            if (it != null) {
                googleMap = it

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    askForLocationPermission()
                } else {
                    it.isMyLocationEnabled = true
                    it.uiSettings.isMyLocationButtonEnabled = true
                }

                it.setOnMyLocationButtonClickListener {
                    if (!isLocationEnabled()) {
                        AlertDialog.Builder(
                            ContextThemeWrapper(
                                context,
                                R.style.MyAlertDialogStyle
                            )
                        )
                            .setMessage("Please make sure your Location Services are enabled")
                            .setPositiveButton(
                                "OK"
                            )
                            { dialog, _ ->
                                dialog.cancel()

                            }.show()
                    }
                    //returning false means the default behavior occurs, the camera moves to the user's current position
                    false

                }


            }


        }


    }

    //if the devices location services are enabled nibbers. VERY IMPORTANT
    private fun isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // This is new method provided in API 28
            val locationManager: LocationManager =
                requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isLocationEnabled
        } else {
            //for older devices
            val mode = Settings.Secure.getInt(
                requireContext().contentResolver, Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            )
            (mode != Settings.Secure.LOCATION_MODE_OFF)
        }
    }


    //managing lifecycle of google mapview
    override fun onStart() {
        super.onStart()
        binding.profileMapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.profileMapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.profileMapView.onStop()
    }


    override fun onLowMemory() {
        super.onLowMemory()
        binding.profileMapView.onLowMemory()
    }

    override fun onPause() {
        super.onPause()
        binding.profileMapView.onPause()

    }


    //permission stuffs
    private fun askForLocationPermission() {
        if (shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            AlertDialog.Builder(requireContext())
                .setTitle("Permission needed")
                .setMessage("Location permission is needed to access your current location")
                .setPositiveButton("GRANT PERMISSION") { _, _ ->
                    //if clicked ok, it requests the permission
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION
                    )
                } //if clicked no, it dismisses the dialog
                .setNegativeButton(
                    "DECLINE"
                ) { dialog, _ ->
                    dialog.dismiss()
                    Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
                }.create().show()
        } else
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION
            )
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {

            LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Location permissions granted.", Toast.LENGTH_SHORT)
                        .show()

                    //enabling the my location button
                    googleMap?.isMyLocationEnabled = true
                    googleMap?.uiSettings?.isMyLocationButtonEnabled = true

                } else
                    Toast.makeText(context, "Locattion permission not granted.", Toast.LENGTH_SHORT)
                        .show()

            }


        }


    }

    //SENDING NOTIFICATION TO THE RECEIVER WHEN WE SEND THEM A MESSAGE
    private fun sendGpsRequestNotification(receiverUserID: String) {
       var fcmApiService: FCMAPIService = retrofit.create(FCMAPIService::class.java)

        //getting the token of the receiver// we need it for sending a remote message(notification) to them
        db.getReference(TOKEN).child(receiverUserID)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val receiversToken = snapshot.getValue(Token::class.java)


                        //this is just so to get our username and user profile pic
                        db.getReference(USERS).child(auth.currentUser!!.uid)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val currentUser = snapshot.getValue(User::class.java)
                                        val username = currentUser!!.username
                                        val userprofilepic = currentUser.profilePic

                                        //filling up the data we need to send for the notification
                                        val notificationData = NotificationData(
                                            sender = auth.currentUser!!.uid,
                                            senderPic = userprofilepic,
                                            title = "$username has asked you to turn on GPS location",
                                            body = "",
                                            imageUrl = "",
                                            receiver = receiversToken?.tokenID.toString()
                                        )

                                        //this is the object that we actually send to the API
                                        val body = SendNotification(
                                            data = notificationData,
                                            to = receiversToken?.tokenID.toString()
                                        )
                                        Log.d(
                                            TAG,
                                            "onDataChange: Preparing to send notification to:${receiversToken?.tokenID.toString()}"
                                        )

                                        fcmApiService.sendNotifications(body).enqueue(object :
                                            Callback<MyResponse> {
                                            override fun onResponse(
                                                call: Call<MyResponse>,
                                                response: Response<MyResponse>
                                            ) {
                                                Log.d(
                                                    TAG,
                                                    "onResponse: RemoteMessage: Sending notifications " +
                                                            "Response code = ${response.code()}"

                                                )
                                                Log.d(
                                                  TAG,
                                                    "onResponse: RemoteMessage: Sending notifications " +
                                                            "Error message = ${
                                                                response.errorBody()?.string()
                                                            }"
                                                )




                                                if (response.code() == 200) {
                                                    if (response.body()?.success != 1) {
                                                        Log.d(
                                                           TAG,
                                                            "onResponse: RemoteMessage: Sending notifications FAILED" +
                                                                    "success code: ${response.body()?.success}"

                                                        )
                                                        Toast.makeText(requireContext(), "You requested the user to turn on their GPS", Toast.LENGTH_SHORT)
                                                            .show()

                                                    } else {
                                                        Log.d(
                                                           TAG,
                                                            "onResponse: RemoteMessage: Sending notifications SUCCESS" +
                                                                    "success code: ${response.body()?.success}"
                                                        )
                                                    }
                                                }
                                            }

                                            override fun onFailure(
                                                call: Call<MyResponse>,
                                                t: Throwable
                                            ) {
                                                Log.d(
                                                  TAG,
                                                    "onResponse: Error in sending RemoteNotification" +
                                                            t.printStackTrace()

                                                )
                                                Toast.makeText(requireContext(), "Your requested to the user to turn on their GPS was not sent" , Toast.LENGTH_SHORT)
                                                    .show()
                                            }


                                        })


                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    Log.d(
                                     TAG,
                                        "onCancelled: Reading user from db error: ${
                                            error.toException().printStackTrace()
                                        }"
                                    )

                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d(
                    TAG,
                        "onCancelled:Getting user token from db error: ${
                            error.toException().printStackTrace()
                        }"
                    )

                }
            })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null


    }




}