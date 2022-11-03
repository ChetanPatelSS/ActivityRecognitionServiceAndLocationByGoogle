package com.google.android.gms.location.sample.activityrecognition.services

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.location.sample.activityrecognition.Constants
import com.google.android.gms.location.sample.activityrecognition.Utils.getPendingIntentFlags
import com.google.android.gms.location.sample.activityrecognition.services.ActivityDetectionService
import com.google.android.gms.location.sample.activityrecognition.services.DetectedActivityIntentService

class ActivityDetectionService : Service() {
    private var mPendingIntent: PendingIntent? = null
    private var mActivityRecognitionClient: ActivityRecognitionClient? = null
    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind()")
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand()")
        mActivityRecognitionClient = ActivityRecognitionClient(this)
        val mIntentService = Intent(this, DetectedActivityIntentService::class.java)
        // FLAG_UPDATE_CURRENT indicates that if the described PendingIntent already exists,
        // then keep it but replace its extra data with what is in this new Intent.
        mPendingIntent = PendingIntent.getService(
            this,
            1, mIntentService, getPendingIntentFlags(true)
        )
        requestActivityUpdatesHandler()
        return START_STICKY
    }

    // request updates and set up callbacks for success or failure
    private fun requestActivityUpdatesHandler() {
        Log.d(TAG, "requestActivityUpdatesHandler()")
        if (mActivityRecognitionClient != null) {
            val task = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            } else {
                mActivityRecognitionClient!!.requestActivityUpdates(
                    Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                    mPendingIntent!!
                )
            }
            // Adds a listener that is called if the Task completes successfully.
            task.addOnSuccessListener { Log.d(TAG, "Successfully requested activity updates") }
            // Adds a listener that is called if the Task fails.
            task.addOnFailureListener { Log.e(TAG, "Requesting activity updates failed to start") }

        }
    }

    // remove the activity requested updates from Google play.
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service-onDestroy")
        // need to remove the request to Google play services. Brings down the connection.
        removeActivityUpdatesHandler()
        stopSelf()
    }

    // remove updates and set up callbacks for success or failure
    fun removeActivityUpdatesHandler() {
        if (mActivityRecognitionClient != null) {
            val task = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }else
                mActivityRecognitionClient!!.removeActivityUpdates(
                mPendingIntent!!)

            // Adds a listener that is called if the Task completes successfully.
            task.addOnSuccessListener { Log.d(TAG, "Removed activity updates successfully!") }
            // Adds a listener that is called if the Task fails.
            task.addOnFailureListener { Log.e(TAG, "Failed to remove activity updates!") }
        }
    }

    companion object {
        val TAG = ActivityDetectionService::class.java.simpleName
    }
}