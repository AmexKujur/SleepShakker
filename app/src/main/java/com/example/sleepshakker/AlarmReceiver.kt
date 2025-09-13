package com.example.sleepshakker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        var ringtone: Ringtone? = null
        const val ALARM_NOTIFICATION_CHANNEL_ID = "alarm_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "onReceive ENTERED. Action: ${intent.action}, Alarm ID from intent: ${intent.getLongExtra("ALARM_ITEM_ID", -99L)}")
        try {
            val action = intent.action
            val alarmItemId = intent.getLongExtra("ALARM_ITEM_ID", -1L)
            val challengeType = intent.getStringExtra("CHALLENGE_TYPE") ?: "SHAKE" // Ensure default

            if (Intent.ACTION_BOOT_COMPLETED == action) {
                Log.d("AlarmReceiver", "Device booted. Implement alarm rescheduling logic here.")
                // Your rescheduling logic
            } else {
                Log.d("AlarmReceiver", "Alarm intent received. Item ID: $alarmItemId, Challenge: $challengeType")

                if (alarmItemId != -1L) {
                    // Start Ringtone
                    try {
                        val alarmSoundUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        if (alarmSoundUri != null) {
                            ringtone?.stop()
                            ringtone = RingtoneManager.getRingtone(context, alarmSoundUri)
                            ringtone?.play()
                            Log.d("AlarmReceiver", "Ringtone started for alarm ID: $alarmItemId")
                        } else {
                            Log.e("AlarmReceiver", "Could not find any default sound URI.")
                        }
                    } catch (e: Exception) {
                        Log.e("AlarmReceiver_Ringtone", "Error playing alarm sound for alarm ID: $alarmItemId", e)
                    }

                    // --- Create and Show Notification with fullScreenIntent ---
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    createNotificationChannel(notificationManager)

                    // Intent to launch DismissActivity
                    val dismissActivityIntent = Intent(context, DismissActivity::class.java).apply {
                        // FLAG_ACTIVITY_NEW_TASK is good here for a fullScreenIntent
                        // FLAG_ACTIVITY_CLEAR_TASK might also be useful depending on desired behavior
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("ALARM_ITEM_ID", alarmItemId)
                        putExtra("CHALLENGE_TYPE", challengeType)
                    }

                    // Request code for PendingIntent should be unique per alarm, e.g., alarmItemId.toInt()
                    // Add PendingIntent.FLAG_IMMUTABLE or FLAG_MUTABLE as required by your target SDK
                    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }

                    val fullScreenPendingIntent = PendingIntent.getActivity(
                        context,
                        alarmItemId.toInt(), // Unique request code
                        dismissActivityIntent,
                        pendingIntentFlags
                    )

                    val notificationBuilder = NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_alarm) // Replace with your alarm icon
                        .setContentTitle("Alarm Ringing!")
                        .setContentText("Time to wake up! Challenge: $challengeType")
                        .setPriority(NotificationCompat.PRIORITY_HIGH) // Essential for heads-up display
                        .setCategory(NotificationCompat.CATEGORY_ALARM) // Essential
                        .setFullScreenIntent(fullScreenPendingIntent, true) // This is the key part!
                        .setAutoCancel(true) // Dismiss notification when DismissActivity is launched
                    // You might also want to add actions like "Dismiss" if the fullScreenIntent fails
                    // or if the user pulls down the notification shade.

                    // Notification ID should also be unique if you might have multiple alarm notifications
                    notificationManager.notify(alarmItemId.toInt(), notificationBuilder.build())
                    Log.d("AlarmReceiver", "Notification with fullScreenIntent posted for alarm ID: $alarmItemId")

                    // DO NOT call context.startActivity(dismissIntent) directly anymore.
                    // The notification system will handle launching DismissActivity via fullScreenIntent.

                } else {
                    Log.w("AlarmReceiver", "Alarm received with invalid or missing ALARM_ITEM_ID.")
                }
            }
        } catch (e: Throwable) {
            Log.e("AlarmReceiver_CRASH", "CRASH in onReceive for alarm ID: ${intent.getLongExtra("ALARM_ITEM_ID", -99L)}", e)
        }
        Log.d("AlarmReceiver", "onReceive EXITED for alarm ID: ${intent.getLongExtra("ALARM_ITEM_ID", -99L)}")
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_NOTIFICATION_CHANNEL_ID,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH // Must be HIGH for fullScreenIntent
            ).apply {
                description = "Channel for alarm notifications"
                // Configure other channel properties like sound (though ringtone is played separately), vibration, etc.
                // For alarms, you might want to disable notification sound on the channel if you manage it manually.
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("AlarmReceiver", "Notification channel created/ensured.")
        }
    }
}
