package com.google.android.gms.location.sample.activityrecognition

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.DetectedActivity
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import java.util.ArrayList

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
}