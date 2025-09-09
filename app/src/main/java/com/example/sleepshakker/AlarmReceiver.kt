package com.example.sleepshakker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        // Static variable to hold the ringtone so it can be stopped from other components (e.g., DismissActivity)
        // A more robust solution would involve a Service to manage the ringtone lifecycle.
        var ringtone: Ringtone? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmItemId = intent.getLongExtra("ALARM_ITEM_ID", -1L) // Retrieve alarmItemId here
        // Retrieve CHALLENGE_TYPE from the incoming intent
        val challengeType = intent.getStringExtra("CHALLENGE_TYPE")

        if (Intent.ACTION_BOOT_COMPLETED == action) {
            Log.d("AlarmReceiver", "Device booted. Implement alarm rescheduling logic here.")
            // TODO: Retrieve all active alarms from storage and reschedule them
            // using AlarmScheduler. For example:
            // val scheduler = AlarmScheduler(context)
            // val alarms = loadAlarmsFromStorage() // Implement this
            // alarms.filter { it.isEnabled }.forEach { scheduler.schedule(it) }
        } else {
            // This is an alarm trigger
            Log.d("AlarmReceiver", "Alarm intent received. Item ID: $alarmItemId, Challenge: $challengeType")

            if (alarmItemId != -1L) {
                // Play the default alarm sound
                try {
                    val alarmSoundUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

                    if (alarmSoundUri == null) {
                        Log.w("AlarmReceiver", "Default alarm sound (TYPE_ALARM) not found. Trying TYPE_NOTIFICATION.")
                        // Fallback to notification sound if alarm sound is not found
                        // alarmSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }

                    if (alarmSoundUri != null) {
                        ringtone?.stop() // Stop any previous ringtone instance from this static variable
                        ringtone = RingtoneManager.getRingtone(context, alarmSoundUri)
                        ringtone?.play()
                        Log.d("AlarmReceiver", "Ringtone started for alarm ID: $alarmItemId")
                    } else {
                        Log.e("AlarmReceiver", "Could not find any default sound URI to play.")
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error playing alarm sound for alarm ID: $alarmItemId", e)
                }

                // Start DismissActivity
                val dismissIntent = Intent(context, DismissActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("ALARM_ITEM_ID", alarmItemId)
                    putExtra("CHALLENGE_TYPE", challengeType) // Pass the challengeType to DismissActivity
                }
                context.startActivity(dismissIntent)
                Log.d("AlarmReceiver", "DismissActivity started for alarm ID: $alarmItemId with challenge: $challengeType")

            } else {
                Log.w("AlarmReceiver", "Alarm received with invalid or missing ALARM_ITEM_ID.")
            }
        }
    }
}
