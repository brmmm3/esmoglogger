package com.wakeup.esmoglogger.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LocationHandler(private val context: AppCompatActivity, private val locationCallback: (Location) -> Unit) : LocationListener {
    private var locationManager: LocationManager? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000

    fun initialize() {
        // Initialize LocationManager
        if (locationManager == null) {
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }
    }

    private fun checkLocationPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    fun handlePermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            AlertDialog.Builder(context)
                .setMessage("Location permissions are required to draw the path.")
                .setPositiveButton("OK") { _, _ -> context.finish() }
                .show()
        }
    }

    private fun startLocationUpdates() {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // Update every 1 second
                    5f, // Update every 5 meters
                    this
                )
                // Provide initial location if available
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let { location ->
                    locationCallback(location)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onLocationChanged(location: Location) {
        locationCallback(location)
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    fun resume() {
        if (checkLocationPermissions()) {
            startLocationUpdates()
        }
    }

    fun pause() {
        locationManager?.removeUpdates(this)
    }
}