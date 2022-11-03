/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.location.sample.activityrecognition

import android.Manifest
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.android.gms.location.DetectedActivity.UNKNOWN
import com.google.android.gms.location.sample.activityrecognition.Constants.REQUEST_PERMISSIONS_REQUEST_CODE
import com.google.android.gms.location.sample.activityrecognition.Utils.checkBackgroundLocationPermissionAPI30
import com.google.android.gms.location.sample.activityrecognition.Utils.checkLocationPermissionAPI28
import com.google.android.gms.location.sample.activityrecognition.Utils.checkLocationPermissionAPI29
import com.google.android.gms.location.sample.activityrecognition.Utils.checkPermissions
import com.google.android.gms.location.sample.activityrecognition.Utils.checkSinglePermission
import com.google.android.gms.location.sample.activityrecognition.Utils.distanceInKm
import com.google.android.gms.location.sample.activityrecognition.Utils.getActivityString
import com.google.android.gms.location.sample.activityrecognition.database.Database
import com.google.android.gms.location.sample.activityrecognition.database.UserActivity
import com.google.android.gms.location.sample.activityrecognition.location.LocationUpdateService
import com.google.android.gms.location.sample.activityrecognition.services.ActivityDetectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), GPSCallback{
    private var mContext: Context? = null

    /**
     * The entry point for interacting with activity recognition.
     */


    // UI elements.
    private var mRequestActivityUpdatesButton: Button? = null
    private var mRemoveActivityUpdatesButton: Button? = null
    private var txtSpeed:TextView? = null

    var latitude = 0.0
    var longitude = 0.0
    private var fusedLocationClient: FusedLocationProviderClient? = null // for lat and long
    private val intervalValue:Long = 10000 //1000 * 60 * 1
    private val fastestIntervalValue:Long = 10000 //1000 * 60 * 1
    // for getting the current location update after every $intervalValue seconds with high accuracy
    private val locationRequest = LocationRequest.create().apply {
        interval = intervalValue
        fastestInterval = fastestIntervalValue
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    var sessionDateTime = Date()

    /**
     * Adapter backed by a list of DetectedActivity objects.
     */
    private var mAdapter: DetectedActivitiesAdapter? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        mContext = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm: PowerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
        // Get the UI widgets.
        mRequestActivityUpdatesButton =
            findViewById<View>(R.id.request_activity_updates_button) as Button
        mRemoveActivityUpdatesButton =
            findViewById<View>(R.id.remove_activity_updates_button) as Button
        txtSpeed = findViewById<View>(R.id.txtSpeed) as TextView
        val detectedActivitiesListView = findViewById<View>(
            R.id.detected_activities_listview
        ) as ListView

        // Enable either the Request Updates button or the Remove Updates button depending on
        // whether activity updates have been requested.
        setButtonsEnabledState()
        val detectedActivities = Utils.detectedActivitiesFromJson(
            PreferenceManager.getDefaultSharedPreferences(this).getString(
                Constants.KEY_DETECTED_ACTIVITIES, ""
            )
        )

        // Bind the adapter to the ListView responsible for display data for detected activities.
        mAdapter = DetectedActivitiesAdapter(this, detectedActivities)
        detectedActivitiesListView.adapter = mAdapter
        setRecyclerView()
        observeActivity()
        if (updatesRequestedState){
            startActivityDetection()
        }

        //distance = distanceInKm(23.2371135, 72.6328296, 23.2157529, 72.6413329)
        //distance = Utils.roundOffDecimal(distance)
        //Log.d("HAM", "distanceInKm-${distance}")
    }

    public override fun onStart() {
        super.onStart()
        Log.d("GPSSpeed", "speed-0-onStart-1")
        if (!checkPermissions(this)) {
            Log.d("GPSSpeed", "speed-0-onStart-2")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d("GPSSpeed", "speed-0-onStart-3")
                isAllPermissionGrantedOrRequest()
            }
        }
        else {
            Log.d("GPSSpeed", "speed-0-onStart-4")
            getCurrentSpeed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(ActivityDetectionService.TAG, "MainActivity-onDestroy")
        stopActivityDetection()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun startActivityDetection(){
        stopActivityDetection()

        if (!isAllPermissionGrantedOrRequest()){
            return
        }

        sessionDateTime = Date()
        if (checkPermissions(this)) {
            Log.d("GPSSpeed", "speed-0-onStart-2")
            getCurrentSpeed()
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mActivityBroadcastReceiver,
            IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY)
        )
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mLocationBroadcastReceiver,
            IntentFilter(Constants.BROADCAST_DETECTED_LOCATION)
        )

        startService(Intent(this, ActivityDetectionService::class.java))
        startService(Intent(this, LocationUpdateService::class.java))
        updatesRequestedState = true
        //getLocation()
    }

    private fun stopActivityDetection(){
        if (mActivityBroadcastReceiver != null) {
            stopService(Intent(this, ActivityDetectionService::class.java))
            stopService(Intent(this, LocationUpdateService::class.java))
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mActivityBroadcastReceiver)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mLocationBroadcastReceiver)
            updatesRequestedState = false
            gpsManager?.stopListening()
            gpsManager?.gpsCallback = null
            gpsManager = null
            fusedLocationClient?.removeLocationUpdates(locationCallback)
        }
    }

    /**
     * Registers for activity recognition updates using
     * [ActivityRecognitionClient.requestActivityUpdates].
     * Registers success and failure callbacks.
     */
    fun requestActivityUpdatesButtonHandler(view: View?) {
        startActivityDetection()


        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(
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
        }
        val task = mActivityRecognitionClient!!.requestActivityUpdates(
            Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
            activityDetectionPendingIntent
        )
        task.addOnSuccessListener {
            Toast.makeText(
                mContext,
                getString(R.string.activity_updates_enabled),
                Toast.LENGTH_SHORT
            )
                .show()
            updatesRequestedState = true
            updateDetectedActivitiesList()
        }
        task.addOnFailureListener {
            Log.w(TAG, getString(R.string.activity_updates_not_enabled))
            Toast.makeText(
                mContext,
                getString(R.string.activity_updates_not_enabled),
                Toast.LENGTH_SHORT
            )
                .show()
            updatesRequestedState = false
        }*/
    }

    /**
     * Removes activity recognition updates using
     * [ActivityRecognitionClient.removeActivityUpdates]. Registers success and
     * failure callbacks.
     */
    fun removeActivityUpdatesButtonHandler(view: View?) {
        stopActivityDetection()
        /*val task = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(
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
        }else{
            mActivityRecognitionClient!!.removeActivityUpdates(
                activityDetectionPendingIntent)
        }


        task.addOnSuccessListener {
            Toast.makeText(
                mContext,
                getString(R.string.activity_updates_removed),
                Toast.LENGTH_SHORT
            )
                .show()
            updatesRequestedState = false
            // Reset the display.
            mAdapter!!.updateActivities(ArrayList())
        }
        task.addOnFailureListener {
            Log.w(TAG, "Failed to enable activity recognition.")
            Toast.makeText(
                mContext, getString(R.string.activity_updates_not_removed),
                Toast.LENGTH_SHORT
            ).show()
            updatesRequestedState = true
        }*/
    }// We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
    // requestActivityUpdates() and removeActivityUpdates().

    /**
     * Ensures that only one button is enabled at any time. The Request Activity Updates button is
     * enabled if the user hasn't yet requested activity updates. The Remove Activity Updates button
     * is enabled if the user has requested activity updates.
     */
    private fun setButtonsEnabledState() {
        if (updatesRequestedState) {
            mRequestActivityUpdatesButton!!.isEnabled = false
            mRemoveActivityUpdatesButton!!.isEnabled = true
        } else {
            mRequestActivityUpdatesButton!!.isEnabled = true
            mRemoveActivityUpdatesButton!!.isEnabled = false
        }
    }
    /**
     * Retrieves the boolean from SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    /**
     * Sets the boolean in SharedPreferences that tracks whether we are requesting activity
     * updates.
     */
    private var updatesRequestedState: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(Constants.KEY_ACTIVITY_UPDATES_REQUESTED, false)
        private set(requesting) {
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Constants.KEY_ACTIVITY_UPDATES_REQUESTED, requesting)
                .apply()
            setButtonsEnabledState()
        }

    private var lastActivity: String
        get() = PreferenceManager.getDefaultSharedPreferences(this)
            .getString(Constants.LAST_ACTIVITY, "").toString()
        private set(activity) {
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString(Constants.LAST_ACTIVITY, activity)
                .apply()
        }

    private var lastActivityType: Int
        get() = PreferenceManager.getDefaultSharedPreferences(this)
            .getInt(Constants.LAST_ACTIVITY_TYPE, UNKNOWN)
        private set(activityType) {
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(Constants.LAST_ACTIVITY_TYPE, activityType)
                .apply()
        }

    private var lastActivityConfidence: Int
        get() = PreferenceManager.getDefaultSharedPreferences(this)
            .getInt(Constants.LAST_ACTIVITY_CONFIDENCE, 0)
        private set(activityType) {
            PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(Constants.LAST_ACTIVITY_CONFIDENCE, activityType)
                .apply()
        }

    companion object {
        const val TAG = "MainActivity"
    }

    private var mActivityBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Log.d(TAG, "onReceive()");
            if (intent.action == Constants.BROADCAST_DETECTED_ACTIVITY) {
                //val type = intent.getIntExtra("type", -1)
                //val confidence = intent.getIntExtra("confidence", 0)
                val detectedActivities = Utils.detectedActivitiesFromJson(intent.getStringExtra("activity"))
                mAdapter?.updateActivities(detectedActivities)
                for (activity in detectedActivities){
                    if(activity.confidence > 70){
                        lastActivity = getActivityString(this@MainActivity, activity.type)
                        lastActivityType = activity.type
                        lastActivityConfidence = activity.confidence
                        saveLastActivityIntoDB(Action.UserActivityChanged)
                    }
                }
            }
        }
    }

    private var mLocationBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Log.d(TAG, "onReceive()");
            if (intent.action == Constants.BROADCAST_DETECTED_LOCATION) {
                latitude = intent.getDoubleExtra("latitude", 0.0)
                longitude = intent.getDoubleExtra("longitude", 0.0)
                saveLastActivityIntoDB(Action.LocationChanged)
            }
        }
    }

    private fun isAllPermissionGrantedOrRequest(): Boolean {
        var isGranted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !checkSinglePermission(Manifest.permission.ACTIVITY_RECOGNITION)){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACTIVITY_RECOGNITION).not()) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), REQUEST_PERMISSIONS_REQUEST_CODE)
            }
            isGranted = false
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION)){
            this.checkLocationPermissionAPI28(Constants.BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE)
            isGranted = false
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !checkSinglePermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            !checkSinglePermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
            !checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
            this.checkLocationPermissionAPI29(Constants.BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE)
            isGranted = false
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !this.checkSinglePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)){
            this.checkBackgroundLocationPermissionAPI30(Constants.BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE)
            isGranted = false
        }
        return isGranted
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i("HAM", "User interaction was cancelled.")
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    if (isAllPermissionGrantedOrRequest()){
                        // Permission granted.
                        getCurrentSpeed()
                    }
                }
                else -> {

                }
            }
        }else if (requestCode == Constants.BACKGROUND_LOCATION_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i("HAM", "User interaction was cancelled.")
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    if (isAllPermissionGrantedOrRequest()){
                        // Permission granted.
                        getCurrentSpeed()
                    }
                }
                else -> {

                }
            }
        }
    }

    private var gpsManager: GPSManager? = null
    private var speed = 0.0
    var isGPSEnabled = false
    var currentSpeed = 0.0
    var kmphSpeed = 0.0

    private fun getCurrentSpeed() {
        Log.d("GPSSpeed", "speed-1-getCurrentSpeed-1")
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        gpsManager = GPSManager(this@MainActivity)
        isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (isGPSEnabled) {
            Log.d("GPSSpeed", "speed-3-getCurrentSpeed-2")
            gpsManager?.startListening(applicationContext)
            gpsManager?.gpsCallback = this
        } else {
            Log.d("GPSSpeed", "speed-4-getCurrentSpeed-2")
            Toast.makeText(this, "GPS not enabled", Toast.LENGTH_LONG).show()
            gpsManager?.showSettingsAlert()
        }
    }

    override fun onGPSUpdate(location: Location) {
        Log.d("GPSSpeed", "speed-7-onGPSUpdate")
        speed = location.speed.toDouble()
        currentSpeed = round(speed, 3, BigDecimal.ROUND_HALF_UP)
        kmphSpeed = round(currentSpeed * 3.6, 3, BigDecimal.ROUND_HALF_UP)
        txtSpeed?.text = "$kmphSpeed km/h"
    }

    private fun round(unrounded: Double, precision: Int, roundingMode: Int): Double {
        val bd = BigDecimal(unrounded)
        val rounded: BigDecimal = bd.setScale(precision, roundingMode)
        return rounded.toDouble()
    }

    //implementing the Interface LocationListener for receiving notifications when the location has changed
    var locationManager //initialize variable, type LocationManager which is a class that provides access to system location services
            : LocationManager? = null



    private fun getLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            isAllPermissionGrantedOrRequest()
            return
        }else{
            if (fusedLocationClient == null){
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                Log.d("HAM", "requestLocationUpdates Start -- lastActivity-$lastActivity")
                fusedLocationClient?.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            for (location in locationResult.locations){
                latitude = location.latitude
                longitude = location.longitude
                //Log.d("HAM", "onLocationResult - - lastActivity-$lastActivity -- onLocationResult:-latitude-$latitude -- longitude-$longitude")
                //Toast.makeText(this@MainActivity,"lastActivity-$lastActivity  lat-$latitude AND long-$longitude", Toast.LENGTH_LONG).show()
                saveLastActivityIntoDB(Action.LocationChanged)
            }
        }
    }

    private fun saveLastActivityIntoDB(action: Action){
        val activityDateTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
        val id = Date()
        distance = getTotalDistance()
        val newActivity = UserActivity(id, activityDateTime, lastActivity, "", 1, latitude, longitude, kmphSpeed, lastActivityConfidence, lastActivityType, distance, sessionDateTime, action.actionId)
        val info = "Activity:- $lastActivity  Lat:- $latitude  Long:- $longitude  Speed:-$kmphSpeed"

        CoroutineScope(Dispatchers.Main).launch {
            activityDatabase.addUserActivity(newActivity)
            distance = 0.0
            observeActivity()
            val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            // Vibrate for 500 milliseconds
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                //deprecated in API 26
                v.vibrate(500)
            }
        }

        //Toast.makeText(context, info, Toast.LENGTH_LONG).show()
        Log.d("HAM", "${action.actonName}-info-${info}")
        Toast.makeText(this,info, Toast.LENGTH_LONG).show()
    }

    var isDistanceActivity = false
    var lastDistanceLatitude = 0.0
    var lastDistanceLongitude = 0.0
    var distance = 0.0
    var lastDistance = 0.0
    private fun getTotalDistance(): Double {
        var tempDistance = 0.0
        if (lastActivityType == DetectedActivity.IN_VEHICLE){
            if (!isDistanceActivity){
                lastDistanceLatitude = latitude
                lastDistanceLongitude = longitude
                isDistanceActivity = true
                lastDistance = 0.0
            }else{
                tempDistance = distanceInKm(lastDistanceLatitude, lastDistanceLongitude, latitude, longitude) + lastDistance
                Log.d("HAM", "$lastActivity - distanceInKm-${tempDistance}")
                lastDistanceLatitude = latitude
                lastDistanceLongitude = longitude
                isDistanceActivity = true
                lastDistance = tempDistance
            }
        }else{
            if (isDistanceActivity){
                tempDistance = distanceInKm(lastDistanceLatitude, lastDistanceLongitude, latitude, longitude) + lastDistance
                Log.d("HAM", "$lastActivity - distanceInKm-${tempDistance}")
            }
            lastDistanceLatitude = 0.0
            lastDistanceLongitude = 0.0
            isDistanceActivity = false
            lastDistance = 0.0
        }

        return tempDistance
    }

    private lateinit var adapter: UserActivityAdapter
    private val activityDatabase by lazy { Database.getDatabase(this).activityDao() }
    private fun setRecyclerView() {
        val activityRecyclerview = findViewById<RecyclerView>(R.id.activity_recyclerview)
        activityRecyclerview.layoutManager = LinearLayoutManager(this)
        activityRecyclerview.setHasFixedSize(true)
        adapter = UserActivityAdapter()
        activityRecyclerview.adapter = adapter
    }

    private fun observeActivity() {
        lifecycleScope.launch {
            activityDatabase.getUserActivity().collect { activityList ->
                Log.d("Your getUserActivity:", activityList.size.toString())
                if (activityList.isNotEmpty()) {
                    adapter.updateData(activityList)
                }
            }
        }
    }
}