package com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.FRAGMENT_IN_VIEWPAGER

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.hackathon.chat_biz.R
import com.hackathon.chat_biz.VIEW.FRAGMENTS.MAIN.BASE.BaseFragmentDirections
import com.hackathon.chat_biz.VIEWMODEL.SettingsViewModel
import com.hackathon.chat_biz.databinding.FragmentSettingsBinding
import com.rengwuxian.materialedittext.MaterialEditText
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject


private const val TAG = "Settings Fragment"

@AndroidEntryPoint
class SettingsFragment @Inject constructor() : Fragment(R.layout.fragment_settings) {

    companion object {
        private const val STORAGE_PERMISSION = 12
        private const val LOCATION_PERMISSION = 89
        const val PROFILE_PIC_PICK = 1
        const val COVER_PIC_PICK = 2
        private const val INTENT_REQUEST_CODE = 42069
        private const val USERNAME_UPDATE = "username"
        private const val BIO_UPDATE = "bio"
        private const val ROLE_UPDATE = "role"
        private const val LOCATION_UPDATE = "location"


    }


    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!


    private val settingsViewModel: SettingsViewModel by viewModels()


    private var pictureToPick: Int? = null
    private var infoToUpdate: String? = null


    var googleMap: GoogleMap? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)


        //for updating the ui with appropriate information
        settingsViewModel.loadUserProfile().observe(viewLifecycleOwner) {
            binding.settingsUsername.text = it?.username
            binding.settingsBio.text =
                it?.bio ?: ""//means if bio is null, put nothing there
            binding.settingsRole.text = it?.role ?: ""
            Glide.with(binding.settingsCardView).load(it?.profilePic)
                .timeout(60000)
                .error(R.drawable.ic_profile)
                .placeholder(R.drawable.ic_profile)
                //A class for monitoring the status of a request while images load.
                //the generic is drawable as we loaded a drawable into the imageview
                .listener(object : RequestListener<Drawable> {
                    //when the loading of image fails
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "onLoadFailed: ERROR in loading pic. Progress bar cancelled")
                        binding.settingsProgressBarProfilePic.isVisible = false
                        return false
                        //ALWAYS return false otherwise glide will not load the image
                    }

                    //when images are loaded
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {

                        Log.d(TAG, "onResourceReady SUCCESS in loading pic. Progress bar cancelled")
                        binding.settingsProgressBarProfilePic.isVisible = false

                        return false
                        //ALWAYS return false otherwise glide will not load the image
                    }

                })
                .into(binding.settingsProfilePic)

            Glide.with(binding.settingsCardView).load(it?.coverPic)
                .timeout(60000)
                .error(R.drawable.coverimage)
                .placeholder(R.drawable.coverimage)
                //A class for monitoring the status of a request while images load.
                //the generic is drawable as we loaded a drawable into the imageview
                .listener(object : RequestListener<Drawable> {
                    //when the loading of image fails
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "onLoadFailed: ERROR in loading pic. Progress bar cancelled")
                        binding.settingsProgressBarCoverpic.isVisible = false
                        return false
                        //ALWAYS return false otherwise glide will not load the image
                    }

                    //when images are loaded
                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {

                        Log.d(TAG, "onResourceReady SUCCESS in loading pic. Progress bar cancelled")
                        binding.settingsProgressBarCoverpic.isVisible = false

                        return false
                        //ALWAYS return false otherwise glide will not load the image
                    }

                }).into(binding.settingsCoverPic)

            binding.settingsAddress.text = it?.location?.substringBefore("latlng")

            //on click listener for profile pic
            binding.settingsProfilePic.setOnClickListener { view ->
                //navigating to full image view fragment
                val action = BaseFragmentDirections
                    .actionBaseFragmentToFullImageViewFragment(it?.profilePic)
                view.findNavController().navigate(action)

            }

            //on click listener for cover pic
            binding.settingsCoverPic.setOnClickListener { view ->
                //navigating to full image view fragment
                val action = BaseFragmentDirections
                    .actionBaseFragmentToFullImageViewFragment(it?.coverPic)
                view.findNavController().navigate(action)


            }

            binding.settingsProgressBar.isVisible = false
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


        //updating the info of the user
        binding.settingsUsername.setOnClickListener {
            infoToUpdate = USERNAME_UPDATE
            enterInfo(infoToUpdate!!)
        }
        binding.settingsBio.setOnClickListener {
            infoToUpdate = BIO_UPDATE
            enterInfo(infoToUpdate!!)
        }

        binding.settingsRole.setOnClickListener {
            infoToUpdate = ROLE_UPDATE
            enterInfo(infoToUpdate!!)
        }

        binding.settingsAddress.setOnClickListener {


        }


        //for modifying user profile and cover pic
        binding.settingsProfilePicEdit.setOnClickListener {
            pictureToPick = PROFILE_PIC_PICK
            pickImage()
        }
        binding.settingsCoverPicEdit.setOnClickListener {
            pictureToPick = COVER_PIC_PICK
            pickImage()
        }



        binding.settingsMapView.onCreate(savedInstanceState)
        binding.settingsMapView.getMapAsync {
            Log.d(TAG, "onMapReady: Map is not ready")
            if (it != null) {
                googleMap = it

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                   // askForLocationPermission()
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




//                LocationServices.getFusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {location ->
//                    it.addMarker(MarkerOptions().position(LatLng(location.latitude, location.longitude)))
//                }

                Log.d(TAG, "onMapReady: Map is ready")


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
        binding.settingsMapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.settingsMapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.settingsMapView.onStop()
    }


    override fun onLowMemory() {
        super.onLowMemory()
        binding.settingsMapView.onLowMemory()
    }

    override fun onPause() {
        super.onPause()
        binding.settingsMapView.onPause()

    }


    //when user click textview, it pops up an alert dialog to update your info
    private fun enterInfo(infoToUpdate: String) {
        //editing the behavior of the edittext based on which info user wants to update
        //We use ContextThemeWrapper as we want to use a theme in our EditText
        val editText =
            MaterialEditText(ContextThemeWrapper(context, R.style.MyEditTextStyleForAlertDialog))
        editText.apply {
            when (infoToUpdate) {
                USERNAME_UPDATE -> {
                    this.maxLines = 1
                    this.inputType = InputType.TYPE_CLASS_TEXT
                    this.addTextChangedListener {
                        if (it.toString().length < 2 || it.toString().length > 25) {
                            this.error = "Invalid Username"

                        }
                    }

                }
                ROLE_UPDATE -> {
                    this.maxLines = 1
                    this.inputType = InputType.TYPE_CLASS_TEXT
                    this.addTextChangedListener {
                        if (it.toString().length < 5) {
                            this.error = "Please enter a valid role"

                        }
                    }
                }
                BIO_UPDATE -> {
                    this.setLines(5)
                    this.maxLines = 5
                    this.isSingleLine = false
                    this.gravity = Gravity.START
                    this.addTextChangedListener {
                        if (it.toString().length < 15) {
                            this.error = "The bio should be at least 15 characters"

                        }
                    }
                }


            }
        }

        editText.hint = "Please enter your $infoToUpdate"
        //we set the editText to alertdialog so that user can enter information
        //We use ContextThemeWrapper as we want to use a theme in our Alert Dialog
        val alertDialog =
            AlertDialog.Builder(ContextThemeWrapper(context, R.style.MyAlertDialogStyle))
                .setView(editText)
                .setPositiveButton(
                    "SUBMIT"
                )
                { _, _ ->
                    //if no errors in the edittext, we can update that data in the db
                    if (editText.error == null && editText.text?.isNotEmpty() == true) {
                        val updatedInfo = editText.text.toString().trim()

                        //UPDATE THE USER INFO
                        settingsViewModel.updateUserInfo(infoToUpdate, updatedInfo)
                    }
                }
                .setNegativeButton("CANCEL") { dialog, _ ->
                    dialog.cancel()
                }.create()


        // this is how you do specific behaviors such as enable/disable button in AlertDialog//using a listener
        alertDialog.setOnShowListener {
            val button = (it as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            button.isEnabled = false
            editText.addTextChangedListener {
                Log.d(TAG, "enterInfo: Alert dialog button changed")
                button.isEnabled = editText.error == null && editText.text?.isNotEmpty() == true
            }
        }
        alertDialog.setCanceledOnTouchOutside(false)

        alertDialog.show()


    }

    private fun pickImage() {
        //first checking for storage permissions
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            askForStoragePermission()
        } else {

            //we send an intent ACTION_GET_CONTENT which will open activity for us to pick an image
            val intent = Intent().apply {
                action = Intent.ACTION_GET_CONTENT
                type = "image/*"

            }
            startActivityForResult(intent, INTENT_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == INTENT_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            //on result, we get the data and we can get the image uri by data.getData() as below

            val imageUri = data.data

            //checking if internet is connected first
            if (!checkIfNetworkIsConnected()) {
                AlertDialog.Builder(ContextThemeWrapper(context, R.style.MyAlertDialogStyle))
                    .setMessage("Please make sure you are connected to a network")
                    .setPositiveButton(
                        "OK"
                    )
                    { dialog, _ ->
                        dialog.cancel()

                    }.show()

            } else {
                if (pictureToPick == COVER_PIC_PICK) {
                    binding.settingsProgressBarCoverpic.isVisible = true
                } else {
                    binding.settingsProgressBarProfilePic.isVisible = true
                }

                //UPLOADING THE IMAGE TO FIREBASE STORAGE AND DB
                settingsViewModel.uploadImageToFirebaseStorage(
                    imageUri,
                    pictureToPick!!
                )
                Toast.makeText(context, "Uploading your image...", Toast.LENGTH_LONG).show()
            }


        }
    }

    //permission stuffs
    private fun askForLocationPermission() {
        if (shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            AlertDialog.Builder(ContextThemeWrapper(context, R.style.MyAlertDialogStyle))
                .setTitle("Permission needed")
                .setMessage("Location permission is needed to access your current location")
                .setPositiveButton("GRANT PERMISSION") { _, _ ->
                    //if clicked ok, it requests the permission
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        SettingsFragment.LOCATION_PERMISSION
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
                SettingsFragment.LOCATION_PERMISSION
            )
    }

    private fun askForStoragePermission() {

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
                    Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
                }.create().show()
        } else
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION
            )
    }


    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        when (requestCode) {
            STORAGE_PERMISSION -> {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Storage permissions granted.", Toast.LENGTH_SHORT)
                        .show()
                } else
                    Toast.makeText(context, "Storage permission not granted.", Toast.LENGTH_SHORT)
                        .show()


            }

//            LOCATION_PERMISSION -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Toast.makeText(context, "Location permissions granted.", Toast.LENGTH_SHORT)
//                        .show()
//
//                    //enabling the my location button
//                    googleMap?.isMyLocationEnabled = true
//                    googleMap?.uiSettings?.isMyLocationButtonEnabled = true
//
//                } else
//                    Toast.makeText(context, "Locattion permission not granted.", Toast.LENGTH_SHORT)
//                        .show()
//
//            }


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
            val activeNetwork = cm.getActiveNetworkInfo()
            activeNetwork != null
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }


}