package com.example.sleepshakker

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AlarmDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmItem): Long // Returns the new rowId

    @Update
    suspend fun update(alarm: AlarmItem)

    @Query("SELECT * FROM alarms ORDER BY timeInMillis ASC")
    fun getAllAlarms(): LiveData<List<AlarmItem>>

    @Query("SELECT * FROM alarms WHERE id = :alarmId")
    suspend fun getAlarmById(alarmId: Long): AlarmItem?

    @Query("DELETE FROM alarms WHERE id = :alarmId")
    suspend fun deleteById(alarmId: Long)

    @Query("DELETE FROM alarms")
    suspend fun deleteAllAlarms() // Optional: if you need a clear all function
}