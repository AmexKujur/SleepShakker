package com.example.sleepshakker

import androidx.lifecycle.ViewModel
import java.util.Calendar

class SetAlarmViewModel(private val alarmScheduler: AlarmScheduler) : ViewModel() {

    fun schedule(alarmItem: AlarmItem) { // Changed signature to accept AlarmItem
        // The AlarmScheduler will now handle the logic of when to schedule
        // (e.g., if time is in the past, schedule for the next valid occurrence based on repeatDays)
        alarmScheduler.schedule(alarmItem) // Pass the whole AlarmItem

        // TODO: Here you would also save the alarmItem to persistent storage
        // (e.g., SharedPreferences, Room database, or a file)
        // so that alarms are remembered if the app is closed or the device restarts.
        // For example:
        // viewModelScope.launch {
        //     alarmRepository.saveAlarm(alarmItem)
        // }
    }
}
