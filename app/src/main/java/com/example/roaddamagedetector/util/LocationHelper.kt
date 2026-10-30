package com.example.roaddamagedetector.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    fun getCurrentLocation(onLocationReceived: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onLocationReceived(null)
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null && isFreshLocation(location)) {
                        onLocationReceived(location)
                    } else {
                        requestFreshLocation(onLocationReceived)
                    }
                }
                .addOnFailureListener {
                    requestFreshLocation(onLocationReceived)
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing", e)
            onLocationReceived(null)
        }
    }

    private fun requestFreshLocation(onLocationReceived: (Location?) -> Unit) {
        if (!hasLocationPermission()) {
            onLocationReceived(null)
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 3000
        ).setMaxUpdates(1).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                fusedLocationClient.removeLocationUpdates(this)
                onLocationReceived(location)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location updates", e)
            onLocationReceived(null)
        }
    }

    private fun isFreshLocation(location: Location): Boolean {
        val age = System.currentTimeMillis() - location.time
        return age < 60_000 // Fresh if under 60 seconds old
    }

    companion object {
        private const val TAG = "LocationHelper"
    }
}
