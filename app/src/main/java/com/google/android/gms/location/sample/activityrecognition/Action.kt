package com.google.android.gms.location.sample.activityrecognition

enum class Action(val actionId:Int, val actonName:String) {
    UnknownAction(0, "unknown"),
    UserActivityChanged(1, "onUserActivityChanged"),
    LocationChanged(2, "onLocationChanged");


    companion object{
        fun getAction(actionId: Int): String {
            return when (actionId) {
                UserActivityChanged.actionId -> {
                    UserActivityChanged.actonName
                }
                LocationChanged.actionId -> {
                    LocationChanged.actonName
                }
                else -> {
                    UnknownAction.actonName
                }
            }
        }
    }
}