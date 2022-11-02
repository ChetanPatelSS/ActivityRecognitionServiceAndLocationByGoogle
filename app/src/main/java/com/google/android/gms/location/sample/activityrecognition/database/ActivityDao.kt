package com.google.android.gms.location.sample.activityrecognition.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUserActivity(userActivity: UserActivity)

    @Query("SELECT * FROM userActivity ORDER BY id DESC")
    fun getUserActivity(): Flow<List<UserActivity>>

    @Update
    suspend fun updateUserActivity(note: UserActivity)

    @Delete
    suspend fun deleteUserActivity(note: UserActivity)
}