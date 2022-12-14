/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.location.sample.activityrecognition

import android.app.IntentService
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import kotlin.collections.ArrayList

/**
 * IntentService for handling incoming intents that are generated as a result of requesting
 * activity updates using
 * [com.google.android.gms.location.ActivityRecognitionApi.requestActivityUpdates].
 */
class DetectedActivitiesIntentService
/**
 * This constructor is required, and calls the super IntentService(String)
 * constructor with the name for a worker thread.
 */
    : IntentService(TAG) {
    override fun onCreate() {
        super.onCreate()
    }

    /**
     * Handles incoming intents.
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     * is called.
     */
    override fun onHandleIntent(intent: Intent?) {
        val result = ActivityRecognitionResult.extractResult(
            intent!!
        )

        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        try {

        }catch (e:Exception){

        }
        val probableActivity = result?.probableActivities
        var detectedActivities: ArrayList<DetectedActivity> = ArrayList()
        if (!probableActivity.isNullOrEmpty()){
            detectedActivities =
                probableActivity as ArrayList<DetectedActivity>
        }

        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putString(
                Constants.KEY_DETECTED_ACTIVITIES,
                Utils.detectedActivitiesToJson(detectedActivities)
            )
            .apply()

        // Log each activity.
        Log.d(TAG, "activities detected")
        for (da in detectedActivities) {
            Log.d(
                TAG, Utils.getActivityString(
                    applicationContext,
                    da.type
                ) + " " + da.confidence + "%"
            )

            /*Toast.makeText(applicationContext, Utils.getActivityString(
                applicationContext,
                da.type
            ) + " " + da.confidence + "%", Toast.LENGTH_LONG).show()*/
        }
    }

    companion object {
        const val TAG = "DetectedActivitiesIS"
    }
}