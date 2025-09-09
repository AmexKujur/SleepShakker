package com.example.sleepshakker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SetAlarmViewModelFactory(private val alarmScheduler: AlarmScheduler) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SetAlarmViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SetAlarmViewModel(alarmScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}