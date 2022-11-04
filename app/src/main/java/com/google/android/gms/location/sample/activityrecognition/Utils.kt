package com.google.android.gms.location.sample.activityrecognition

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import com.google.android.gms.location.DetectedActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Utility methods used in this sample.
 */
object Utils {
    /**
     * Returns a human readable String corresponding to a detected activity type.
     */
    fun getActivityString(context: Context, detectedActivityType: Int): String {
        val resources = context.resources
        return when (detectedActivityType) {
            DetectedActivity.IN_VEHICLE -> resources.getString(R.string.in_vehicle)
            DetectedActivity.ON_BICYCLE -> resources.getString(R.string.on_bicycle)
            DetectedActivity.ON_FOOT -> resources.getString(R.string.on_foot)
            DetectedActivity.RUNNING -> resources.getString(R.string.running)
            DetectedActivity.STILL -> resources.getString(R.string.still)
            DetectedActivity.TILTING -> resources.getString(R.string.tilting)
            DetectedActivity.UNKNOWN -> resources.getString(R.string.unknown)
            DetectedActivity.WALKING -> resources.getString(R.string.walking)
            else -> resources.getString(
                R.string.unidentifiable_activity,
                detectedActivityType
            )
        }
    }

    fun detectedActivitiesToJson(detectedActivitiesList: ArrayList<DetectedActivity>): String {
        val type = object : TypeToken<ArrayList<DetectedActivity?>?>() {}.type
        return Gson().toJson(detectedActivitiesList, type)
    }

    fun detectedActivitiesFromJson(jsonArray: String?): ArrayList<DetectedActivity> {
        val listType = object : TypeToken<ArrayList<DetectedActivity>>() {}.type
        var detectedActivities = Gson().fromJson<ArrayList<DetectedActivity>>(jsonArray, listType)
        if (detectedActivities == null) {
            detectedActivities = ArrayList()
        }
        return detectedActivities
    }

    fun getPendingIntentFlags(isMutable: Boolean = false) =
        when {
            isMutable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE

            !isMutable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            else -> PendingIntent.FLAG_UPDATE_CURRENT
        }

    fun checkPermissions(context: Context): Boolean {
        val coarsePermissionState = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val finePermissionState = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        var activityPermissionState = PackageManager.PERMISSION_GRANTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            activityPermissionState = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            )
        }

        return activityPermissionState == PackageManager.PERMISSION_GRANTED && coarsePermissionState == PackageManager.PERMISSION_GRANTED && finePermissionState == PackageManager.PERMISSION_GRANTED
    }

    fun distanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist *= 60 * 1.1515
        dist *= 1.609344
        return dist
    }

    //unit == M, K, N
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double, unit: String): Double {
        return if (lat1 == lat2 && lon1 == lon2) {
            0.0
        } else {
            val theta = lon1 - lon2
            var dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(
                Math.toRadians(lat1)
            ) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta))
            dist = Math.acos(dist)
            dist = Math.toDegrees(dist)
            dist *= 60 * 1.1515
            if (unit == "K") {
                dist *= 1.609344
            } else if (unit == "N") {
                dist *= 0.8684
            }
            dist
        }
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }

    fun roundOffDecimal(number: Double): Double {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.FLOOR
        return df.format(number).toDouble()
    }

    @TargetApi(30)
    fun Activity.checkBackgroundLocationPermissionAPI30(backgroundLocationRequestCode: Int) {
        if (checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) return
        AlertDialog.Builder(this)
            .setTitle("Permission Needed!")
            .setMessage("Background Location Permission Needed!, tap \"Allow all time in the next screen\"")
            .setPositiveButton(R.string.settings) { _,_ ->
                // this request will take user to Application's Setting page
                requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), backgroundLocationRequestCode)
            }
            .setNegativeButton(R.string.cancel) { dialog,_ ->
                dialog.dismiss()
            }
            .create()
            .show()

    }

    @TargetApi(28)
    fun Activity.checkLocationPermissionAPI28(locationRequestCode : Int) {
        if (!checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            val permList = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            requestPermissions(permList, locationRequestCode)
        }
    }

    @TargetApi(29)
    fun Activity.checkLocationPermissionAPI29(locationRequestCode : Int) {
        if (checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
            checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) return
        val permList = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        requestPermissions(permList, locationRequestCode)

    }

    fun Context.checkSinglePermission(permission: String) : Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }
}