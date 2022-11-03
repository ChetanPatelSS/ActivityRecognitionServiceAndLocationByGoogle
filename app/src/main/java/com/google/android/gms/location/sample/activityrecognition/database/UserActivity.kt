package com.google.android.gms.location.sample.activityrecognition.database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.location.DetectedActivity.UNKNOWN
import com.google.android.gms.location.sample.activityrecognition.Action
import java.util.*

@Entity(tableName = "userActivity")
data class UserActivity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Date,
    @ColumnInfo(name = "dateAdded")
    val dateAdded: String,
    @ColumnInfo(name = "activity")
    val activity: String,
    @ColumnInfo(name = "activityTransitionType")
    val activityTransitionType: String,
    @ColumnInfo(name = "elapsedRealTimeNanos")
    val elapsedRealTimeNanos: Long,
    @ColumnInfo(name = "latitude")
    val latitude: Double,
    @ColumnInfo(name = "longitude")
    val longitude: Double,
    @ColumnInfo(name = "speed")
    val speed: Double,
    @ColumnInfo(name = "confidence")
    val confidence: Int,
    @ColumnInfo(name = "activityType")
    val activityType: Int = UNKNOWN,
    @ColumnInfo(name = "distance")
    val distance: Double = 0.0,
    @ColumnInfo(name = "session")
    val session: Date,
    @ColumnInfo(name = "action")
    val action: Int = Action.UnknownAction.actionId
)
