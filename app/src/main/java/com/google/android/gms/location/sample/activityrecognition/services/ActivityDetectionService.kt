package com.google.android.gms.location.sample.activityrecognition.services

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.gms.location.sample.activityrecognition.Constants
import com.google.android.gms.location.sample.activityrecognition.Constants.BROADCAST_DETECTED_LOCATION
import com.google.android.gms.location.sample.activityrecognition.Constants.CHANNEL_ID
import com.google.android.gms.location.sample.activityrecognition.Constants.LOCATION_DETECTION_INTERVAL_IN_MILLISECONDS
import com.google.android.gms.location.sample.activityrecognition.MainActivity
import com.google.android.gms.location.sample.activityrecognition.Utils.getPendingIntentFlags
import com.google.android.gms.location.sample.activityrecognition.R


class ActivityDetectionService : Service() {
    private var mPendingIntent: PendingIntent? = null
    private var mActivityRecognitionClient: ActivityRecognitionClient? = null


    //region data
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null

    //endregion
    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind()")
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate()")
        initData()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand()")
        prepareForegroundNotification()
        startLocationUpdates()
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
        stopLocationUpdates()
        stopSelf()
    }

    // remove updates and set up callbacks for success or failure
    private fun removeActivityUpdatesHandler() {
        if (mActivityRecognitionClient != null) {
            val task = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
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
        val TAG: String = ActivityDetectionService::class.java.simpleName
    }


    //Location Callback
    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val currentLocation = locationResult.lastLocation
            Log.d(
                "Locations",
                currentLocation!!.latitude.toString() + "," + currentLocation.longitude
            )
            //Share/Publish Location
            broadcastActivity(currentLocation)
        }
    }

    private fun broadcastActivity(currentLocation: Location?) {
        val intent = Intent(BROADCAST_DETECTED_LOCATION)
        intent.putExtra("latitude", currentLocation!!.latitude)
        intent.putExtra("longitude", currentLocation.longitude)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mFusedLocationClient!!.requestLocationUpdates(
            locationRequest!!,
            locationCallback, Looper.myLooper()
        )
    }

    private fun stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mFusedLocationClient!!.removeLocationUpdates(locationCallback)
    }

    private fun prepareForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            123,
            notificationIntent, getPendingIntentFlags(true)
        )
        val notification: Notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentTitle("Location tracking")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(1234, notification)
    }

    private fun initData() {
        locationRequest = LocationRequest.create().apply {
            interval = LOCATION_DETECTION_INTERVAL_IN_MILLISECONDS
            fastestInterval = LOCATION_DETECTION_INTERVAL_IN_MILLISECONDS
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }
}