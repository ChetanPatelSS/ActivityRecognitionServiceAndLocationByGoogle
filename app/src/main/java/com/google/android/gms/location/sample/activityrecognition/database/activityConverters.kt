package com.google.android.gms.location.sample.activityrecognition.database

import androidx.room.TypeConverter
import java.util.*

class activityConverters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}