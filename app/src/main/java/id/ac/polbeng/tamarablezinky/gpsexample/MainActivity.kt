package id.ac.polbeng.tamarablezinky.gpsexample

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import id.ac.polbeng.tamarablezinky.gpsexample.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var trackingLocation: Boolean = false
    private val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvLocation.text = ""
        binding.tvLocation.movementMethod = ScrollingMovementMethod()
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {

                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION,
                    false) -> {
                    // Precise location access granted.
                }

                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION,
                    false) -> {
                    // Only approximate location access granted.
                } else -> {
                // No location access granted.
                binding.btnUpdate.isEnabled = false
            }
            }
        }
        locationPermissionRequest.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        val builder = LocationSettingsRequest.Builder()
        val client: SettingsClient =
            LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> =
            client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            locationRequest = LocationRequest.create().apply {
                interval = TimeUnit.SECONDS.toMillis(20)
                // Sets the fastest rate for active location updates. This interval is exact, and your
                // application will never receive updates more frequently than this value. fastestInterval = TimeUnit.SECONDS.toMillis(10)
                // Sets the maximum time when batched location updates are delivered. Updates may be
                        // delivered sooner than this interval. maxWaitTime = TimeUnit.SECONDS.toMillis(40)
                //smallestDisplacement = 107f //170m = 0.1 mile
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult:
                                              LocationResult) {
                    /*for (location in locationResult.locations){
                    val latitude = location.latitude.toString()
                   val longitude = location.longitude.toString()
                   logResultsToScreen("$latitude, $longitude")
                    }*/
                    if (locationResult.locations.isNotEmpty()) {
                        // get latest location
                        val location = locationResult.lastLocation
                        // use your location object
                        // get latitude , longitude and other info from this
                        val latitude = location.latitude.toString()
                        val longitude = location.longitude.toString()
                        logResultsToScreen("$latitude, $longitude")
                    }
                }
            }
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            exception.startResolutionForResult(this@MainActivity, 100)
                        } catch (sendEx: IntentSender.SendIntentException) {
                            // Ignore the error.
                        }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location : Location? ->
                // Got last known location. In some rare situations this can be null.
            logResultsToScreen("${location?.latitude},${location?.longitude}")
            }
        binding.btnUpdate.setOnClickListener {
            if(!trackingLocation){
                startLocationUpdates()
                trackingLocation = true
            }else{
                stopLocationUpdates()
                trackingLocation = false
            }
            updateButtonState(trackingLocation)
        }
    }
    private fun logResultsToScreen(output: String) {
        val outputWithPreviousLogs =
            "$output\n${binding.tvLocation.text}"
        binding.tvLocation.text = outputWithPreviousLogs
    }
    private fun updateButtonState(trackingLocation: Boolean) {
        if (trackingLocation) {
            binding.btnUpdate.text = getString(R.string.stop_update)
        } else {
            binding.btnUpdate.text = getString(R.string.start_update)
        }
    }
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        Log.d(TAG, "Request Location Updates")
    }
    private fun stopLocationUpdates() {
        //fusedLocationClient.removeLocationUpdates(locationCallback)
        val removeTask =
            fusedLocationClient.removeLocationUpdates(locationCallback)
        removeTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(TAG, "Location Callback removed.")
            } else {
                Log.d(TAG, "Failed to remove Location Callback.")
            }
        }
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
}