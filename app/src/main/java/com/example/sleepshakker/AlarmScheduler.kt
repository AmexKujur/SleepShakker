package com.example.sleepshakker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log // Added for our debug log

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarmItem: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ITEM_ID", alarmItem.id)
            putExtra("CHALLENGE_TYPE", alarmItem.dismissOption) // Corrected Typo
            // It's often better to pass the whole Parcelable object if it's not too large
            // and if your receiver needs more than just the ID immediately.
            // For now, ID is enough to retrieve from a data store.
            // If you pass the whole item, ensure AlarmReceiver can handle it.
            // putExtra("ALARM_ITEM", alarmItem)
        }
        // Our debug log to confirm
        Log.d("AlarmScheduler", "Scheduling alarm ID ${alarmItem.id} with CHALLENGE_TYPE: ${intent.getStringExtra("CHALLENGE_TYPE")}")


        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmItem.id.toInt(), // requestCode needs to be unique for each alarm
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Ensure alarm time is in the future
        if (alarmItem.timeInMillis < System.currentTimeMillis()) {
            // Optionally log or handle this case, e.g., by rescheduling for the next valid day
            // For now, we assume timeInMillis has been correctly set for the future in SetAlarmActivity
            Log.w("AlarmScheduler", "Attempted to schedule alarm ID ${alarmItem.id} in the past. Skipping.")
            return // Don't schedule alarms in the past
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmItem.timeInMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Fallback for older SDKs or if permission not granted
                 alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmItem.timeInMillis,
                    pendingIntent
                )
            }
            else { // For very old devices, setExact might be okay
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmItem.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("AlarmScheduler", "Alarm ID ${alarmItem.id} scheduled successfully for ${alarmItem.timeInMillis}.")
        } catch (se: SecurityException) {
            // Handle cases where exact alarm permission is denied (e.g., redirect user to settings)
            // This is more relevant for Android 12 (S) and above.
            // For now, log or show a toast.
            android.util.Log.e("AlarmScheduler", "SecurityException: Cannot schedule exact alarms for ID ${alarmItem.id}.", se)
            // Consider showing a Toast or a dialog to the user
        }
    }

    fun cancel(alarmItem: AlarmItem) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmItem.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d("AlarmScheduler", "Cancelled alarm ID ${alarmItem.id}.")
    }
}
