package com.example.sleepshakker

import androidx.room.TypeConverter
import java.util.Calendar

class DaysSetConverter {
    @TypeConverter
    fun fromDaysSet(days: Set<Int>?): String? {
        return days?.joinToString(",")
    }

    @TypeConverter
    fun toDaysSet(daysString: String?): Set<Int>? {
        return daysString?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet()
    }
}