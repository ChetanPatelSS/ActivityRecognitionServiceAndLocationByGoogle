package com.google.android.gms.location.sample.activityrecognition.database
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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
    val confidence: Int
)
