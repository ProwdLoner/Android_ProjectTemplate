package com.example.prowd_android_template.util_object

import android.Manifest
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.*
import java.util.*

object GpsUtil {
    // (GPS 위치정보를 갱신하고 정보 가져오기)
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getNowLocation(
        context: Context,
        onComplete: (lat: Double, lng: Double) -> Unit
    ) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    // 위치 정보 해제
                    fusedLocationClient.removeLocationUpdates(this)

                    for (location in locationResult.locations) {
                        val latitude = location.latitude
                        val longitude = location.longitude

                        onComplete(latitude, longitude)
                        break
                    }
                }
            }
        }

        // 위치 정보 갱신
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    // (가장 최근 업데이트된 위치 정보 가져오기)
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun getCurrentKnownLocation(
        context: Context,
        onComplete: (lat: Double, lng: Double) -> Unit
    ) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location ->
                val latitude = location.latitude
                val longitude = location.longitude
                onComplete(latitude, longitude)
            }
    }

    // (좌표를 주소로 변환)
    fun getAddressFromCoordination(
        context: Context,
        latitude: Double,
        longitude: Double,
        onComplete: (address: String?) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Geocoder(context, Locale.KOREAN).getFromLocation(
                latitude, longitude, 1
            ) { mResultList -> onComplete(mResultList[0].getAddressLine(0)) }
        } else {
            val mResultList: List<Address>? =
                Geocoder(context, Locale.KOREAN).getFromLocation(
                    latitude, longitude, 1
                )
            if (mResultList != null) {
                onComplete(mResultList[0].getAddressLine(0))
            } else {
                onComplete(null)
            }
        }
    }
}