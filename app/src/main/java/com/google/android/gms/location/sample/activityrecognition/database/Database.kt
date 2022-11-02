package com.google.android.gms.location.sample.activityrecognition.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@androidx.room.Database(
    entities = [UserActivity::class],
//    autoMigrations = [
//        AutoMigration (from = 1, to = 2)
//    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(activityConverters::class)
abstract class Database : RoomDatabase() {

    abstract fun activityDao(): ActivityDao

    companion object {
        @Volatile
        private var INSTANCE: Database? = null

        fun getDatabase(context: Context): Database {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            if (INSTANCE == null) {
                synchronized(this) {
                    // Pass the database to the INSTANCE
                    INSTANCE = buildDatabase(context)
                }
            }
            // Return database.
            return INSTANCE!!
        }

        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // The following query will add a new column called lastUpdate to the notes database
                database.execSQL("ALTER TABLE notes ADD COLUMN lastUpdate INTEGER NOT NULL DEFAULT 0")
            }
        }

        private fun buildDatabase(context: Context): Database {
            return Room.databaseBuilder(
                context.applicationContext,
                Database::class.java,
                "activity_database"
            )
                //.addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}