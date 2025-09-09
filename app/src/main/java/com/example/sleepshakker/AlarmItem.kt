package com.example.sleepshakker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Calendar // Ensure Calendar is imported if you use it for repeatDays constants

@Parcelize
data class AlarmItem(
    val id: Long,
    var timeInMillis: Long,
    var message: String = "Alarm",
    var isEnabled: Boolean = true,
    var repeatDays: Set<Int>? = null, // Set of Calendar.DAY_OF_WEEK (e.g., Calendar.MONDAY)
    var dismissOption: String? = null // e.g., "SHAKE", "MATH_PROBLEM", "VIBRATE_ONLY"
    // var vibrationPattern: LongArray? = null, // Placeholder for custom vibration
    // var ringtoneUri: String? = null // Placeholder for Uri for custom ringtone
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AlarmItem

        if (id != other.id) return false
        if (timeInMillis != other.timeInMillis) return false
        if (message != other.message) return false
        if (isEnabled != other.isEnabled) return false
        if (repeatDays != other.repeatDays) return false
        if (dismissOption != other.dismissOption) return false
        // if (vibrationPattern != null) {
        //     if (other.vibrationPattern == null) return false
        //     if (!vibrationPattern.contentEquals(other.vibrationPattern)) return false
        // } else if (other.vibrationPattern != null) return false
        // if (ringtoneUri != other.ringtoneUri) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timeInMillis.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + isEnabled.hashCode()
        result = 31 * result + (repeatDays?.hashCode() ?: 0)
        result = 31 * result + (dismissOption?.hashCode() ?: 0)
        // result = 31 * result + (vibrationPattern?.contentHashCode() ?: 0)
        // result = 31 * result + (ringtoneUri?.hashCode() ?: 0)
        return result
    }
}
