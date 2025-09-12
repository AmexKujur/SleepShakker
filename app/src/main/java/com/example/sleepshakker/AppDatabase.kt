package com.example.sleepshakker

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [AlarmItem::class], version = 1, exportSchema = false)
@TypeConverters(DaysSetConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sleep_shakker_database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // .fallbackToDestructiveMigration() // Use with caution
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
