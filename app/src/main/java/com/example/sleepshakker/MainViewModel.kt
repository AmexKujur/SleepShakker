package com.example.sleepshakker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val alarmDao: AlarmDao
    val allAlarms: LiveData<List<AlarmItem>>

    init {
        val database = AppDatabase.getDatabase(application)
        alarmDao = database.alarmDao()
        allAlarms = alarmDao.getAllAlarms()
    }

    // Optional: If you want to add a function to delete an alarm from the ViewModel
    fun deleteAlarm(alarmItem: AlarmItem) {
        viewModelScope.launch {
            alarmDao.deleteById(alarmItem.id)
            // If you need to re-schedule/cancel system alarms when deleting from DB
            // you might need to call AlarmScheduler here too.
            val scheduler = AlarmScheduler(getApplication())
            scheduler.cancel(alarmItem)
        }
    }

    // Optional: If you want to update an alarm (e.g., toggle isEnabled)
    fun updateAlarm(alarmItem: AlarmItem) {
        viewModelScope.launch {
            alarmDao.update(alarmItem)
            // If isEnabled changed, re-schedule or cancel accordingly
            val scheduler = AlarmScheduler(getApplication())
            if (alarmItem.isEnabled) {
                // Before re-scheduling, ensure the time is still valid,
                // or retrieve the full item from DB if needed for original hour/minute.
                // For simplicity, this assumes alarmItem has correct future time if re-enabled.
                // A more robust solution might re-calculate next occurrence if it's a repeating alarm.
                scheduler.schedule(alarmItem)
            } else {
                scheduler.cancel(alarmItem)
            }
        }
    }
}
