package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import java.util.Locale

object LocationHelper {

    fun hasLocationPermissions(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun fetchLiveLocation(context: Context, onLocationResult: (Double, Double, String) -> Unit) {
        if (!hasLocationPermissions(context)) {
            onLocationResult(0.0, 0.0, "Permission Denied")
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager == null) {
            onLocationResult(0.0, 0.0, "GPS Unavailable")
            return
        }

        var location: Location? = null
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
        } catch (e: Exception) {
            // ignore
        }

        if (location != null) {
            val lat = location.latitude
            val lng = location.longitude
            val addr = getAddressFromCoords(context, lat, lng)
            onLocationResult(lat, lng, addr)
            return
        }

        val provider = when {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        if (provider != null) {
            try {
                locationManager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        onLocationResult(loc.latitude, loc.longitude, getAddressFromCoords(context, loc.latitude, loc.longitude))
                    }
                    override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                    override fun onProviderEnabled(p0: String) {}
                    override fun onProviderDisabled(p0: String) {}
                }, null)
            } catch (e: Exception) {
                onLocationResult(0.0, 0.0, "Location Error: " + e.localizedMessage)
            }
        } else {
            onLocationResult(0.0, 0.0, "No Location Providers")
        }
    }

    fun getAddressFromCoords(context: Context, lat: Double, lng: Double): String {
        if (lat == 0.0 && lng == 0.0) return "Unknown Spot"
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                val line = addresses[0].getAddressLine(0)
                if (!line.isNullOrBlank()) {
                    return line
                }
            }
            "UK Security Patrol Spot ($lat, $lng)"
        } catch (e: Exception) {
            "UK Security Patrol Spot ($lat, $lng)"
        }
    }
}
