package com.google.android.gms.location.sample.activityrecognition.services

import android.app.IntentService
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.sample.activityrecognition.Constants
import com.google.android.gms.location.sample.activityrecognition.Constants.BROADCAST_DETECTED_ACTIVITY
import com.google.android.gms.location.sample.activityrecognition.Utils
import com.google.android.gms.location.sample.activityrecognition.Utils.detectedActivitiesToJson
import java.lang.Exception
import java.util.ArrayList

class DetectedActivityIntentService : IntentService(TAG) {
    override fun onCreate() {
        super.onCreate()
        // Log.d(TAG,TAG + "onCreate()");
    }

    override fun onHandleIntent(intent: Intent?) {
        try {
            Log.d(TAG, TAG + "onHandleIntent()")
            val result = ActivityRecognitionResult.extractResult(
                intent!!
            )

            // Get the list of the probable activities associated with the current state of the
            // device. Each activity is associated with a confidence level, which is an int between
            // 0 and 100.
            val detectedActivities = result!!.probableActivities as ArrayList<DetectedActivity>
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(
                    Constants.KEY_DETECTED_ACTIVITIES,
                    Utils.detectedActivitiesToJson(detectedActivities)
                )
                .apply()
            broadcastActivity(detectedActivities)
            /*for (activity in detectedActivities) {
                Log.d(TAG, "Detected activity: " + activity.type + ", " + activity.confidence)
                broadcastActivity(activity)
            }*/
        } catch (e: Exception) {
        }
    }

    private fun broadcastActivity(activity: ArrayList<DetectedActivity>) {
         Log.d(TAG,TAG+ "broadcastActivity()");
        val intent = Intent(BROADCAST_DETECTED_ACTIVITY)
        intent.putExtra("activity", detectedActivitiesToJson(activity))
        //intent.putExtra("type", activity.type)
        //intent.putExtra("confidence", activity.confidence)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {
        val TAG = DetectedActivityIntentService::class.java.simpleName
    }
}