package com.example.sleepshakker

import android.app.NotificationManager // Added for cancelling notification
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt

class DismissActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lightSensor: Sensor? = null

    // For Shake Challenge
    private var shakeProgress = 0
    private lateinit var shakeProgressBar: ProgressBar
    private var isShakeChallengeActive = false

    // For LUX Challenge
    private lateinit var luxChallengeLayout: LinearLayout
    private var isLuxChallengeActive = false
    private val LUX_THRESHOLD_BRIGHT = 150f

    private var alarmItemId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        // VERY FIRST LINE: Log entry
        Log.d("DismissActivity_Lifecycle", "onCreate ENTERED for alarm ID from intent: " + intent.getLongExtra("ALARM_ITEM_ID", -99L))

        try {
            super.onCreate(savedInstanceState)
            Log.d("DismissActivity_Lifecycle", "super.onCreate() COMPLETED")

            setContentView(R.layout.activity_dismiss)
            Log.d("DismissActivity_Lifecycle", "setContentView() COMPLETED")

            alarmItemId = intent.getLongExtra("ALARM_ITEM_ID", -1L)
            Log.d("DismissActivity_Lifecycle", "Instance alarmItemId SET to: $alarmItemId")

            if (alarmItemId == -1L || alarmItemId == -99L) {
                Log.e("DismissActivity_Lifecycle", "CRITICAL: Invalid alarmItemId received. Cannot proceed. Finishing.")
                Toast.makeText(this, "Error: Invalid alarm ID.", Toast.LENGTH_LONG).show()
                // Stop any potentially orphaned ringtone if possible
                AlarmReceiver.ringtone?.stop()
                AlarmReceiver.ringtone = null
                finish()
                return // Exit onCreate early
            }

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            Log.d("DismissActivity_Lifecycle", "SensorManager OBTAINED for ID: $alarmItemId")

            shakeProgressBar = findViewById(R.id.shakeProgressBar)
            luxChallengeLayout = findViewById(R.id.luxChallengeLayout)
            Log.d("DismissActivity_Lifecycle", "UI elements FOUND for ID: $alarmItemId")

            Log.d("DismissActivity_Lifecycle", "Calling showOverLockscreen() for ID: $alarmItemId...")
            showOverLockscreen()
            Log.d("DismissActivity_Lifecycle", "showOverLockscreen() COMPLETED for ID: $alarmItemId")

            val challengeType = intent.getStringExtra("CHALLENGE_TYPE") ?: "SHAKE"
            Log.d("DismissActivity_Lifecycle", "Challenge type from intent: $challengeType for ID: $alarmItemId")

            findViewById<LinearLayout>(R.id.shakeChallengeLayout).visibility = View.GONE
            findViewById<LinearLayout>(R.id.mathChallengeLayout).visibility = View.GONE
            luxChallengeLayout.visibility = View.GONE
            Log.d("DismissActivity_Lifecycle", "All challenge layouts HIDDEN initially for ID: $alarmItemId")

            when (challengeType) {
                "SHAKE" -> {
                    isShakeChallengeActive = true
                    Log.d("DismissActivity_Lifecycle", "Setting up SHAKE challenge for ID: $alarmItemId...")
                    setupShakeChallenge()
                }
                "MATH" -> {
                    Log.d("DismissActivity_Lifecycle", "Setting up MATH challenge for ID: $alarmItemId...")
                    setupMathChallenge()
                }
                "LUX_CHALLENGE" -> {
                    isLuxChallengeActive = true
                    Log.d("DismissActivity_Lifecycle", "Setting up LUX challenge for ID: $alarmItemId...")
                    setupLuxChallenge()
                }
                else -> {
                    Log.w("DismissActivity_Lifecycle", "Unknown challenge type '$challengeType', defaulting to SHAKE for ID: $alarmItemId.")
                    isShakeChallengeActive = true
                    setupShakeChallenge()
                }
            }
            Log.d("DismissActivity_Lifecycle", "Challenge setup COMPLETED for ID: $alarmItemId, type: $challengeType")

        } catch (e: Throwable) {
            Log.e("DismissActivity_CRASH", "CRASH IN ONCREATE for ID: $alarmItemId: ${e.message}", e)
            AlarmReceiver.ringtone?.stop()
            AlarmReceiver.ringtone = null
            // Cancel notification if it was used to launch, assuming alarmItemId is valid
            if (alarmItemId != -1L && alarmItemId != -99L) {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(alarmItemId.toInt())
            }
            Toast.makeText(applicationContext, "DismissActivity crashed. Alarm stopped.", Toast.LENGTH_LONG).show()
            finish() // Finish the activity if it crashed during setup
            // Not re-throwing e here to prevent system "app has crashed" dialog if we handled it.
            return
        }
        Log.d("DismissActivity_Lifecycle", "onCreate EXITED NORMALLY for alarm ID: $alarmItemId")
    }

    private fun dismissAlarm() {
        Log.d("DismissActivity_Action", "Dismissing alarm ID: $alarmItemId")
        AlarmReceiver.ringtone?.stop()
        AlarmReceiver.ringtone = null
        Log.d("DismissActivity_Action", "Ringtone stopped and nulled for alarm ID: $alarmItemId")

        sensorManager.unregisterListener(this)
        Log.d("DismissActivity_Action", "All sensor listeners unregistered in dismissAlarm for ID: $alarmItemId")

        // Cancel the notification that launched this activity
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmItemId.toInt()) // Assumes alarmItemId is a valid int for notification ID
        Log.d("DismissActivity_Action", "Notification CANCELED for alarm ID: $alarmItemId")

        isShakeChallengeActive = false
        isLuxChallengeActive = false

        Toast.makeText(this, "Alarm Dismissed!", Toast.LENGTH_SHORT).show()
        finish()
        Log.d("DismissActivity_Action", "finish() called for alarm ID: $alarmItemId")
    }

    private fun setupShakeChallenge() {
        Log.d("DismissActivity_Setup", "setupShakeChallenge BEGIN for alarm ID: $alarmItemId")
        try {
            findViewById<LinearLayout>(R.id.shakeChallengeLayout).visibility = View.VISIBLE
            shakeProgressBar.progress = 0
            shakeProgress = 0
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accelerometer != null) {
                val registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
                Log.d("DismissActivity_Setup", "Accelerometer listener registered for SHAKE: $registered for ID: $alarmItemId")
            } else {
                Log.e("DismissActivity_Setup", "Accelerometer not available for ID: $alarmItemId. Cannot setup shake challenge.")
                Toast.makeText(this, "Shake sensor not available!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("DismissActivity_CRASH", "CRASH in setupShakeChallenge for ID: $alarmItemId", e)
            Toast.makeText(this, "Error setting up shake challenge.", Toast.LENGTH_SHORT).show()
        }
        Log.d("DismissActivity_Setup", "setupShakeChallenge END for alarm ID: $alarmItemId")
    }

    private fun setupMathChallenge() {
        Log.d("DismissActivity_Setup", "setupMathChallenge BEGIN for alarm ID: $alarmItemId")
        try {
            findViewById<LinearLayout>(R.id.mathChallengeLayout).visibility = View.VISIBLE
            val num1 = (10..25).random()
            val num2 = (5..15).random()
            val answer = num1 + num2

            findViewById<TextView>(R.id.mathQuestionText).text = "$num1 + $num2 = ?"
            val mathAnswerInput = findViewById<EditText>(R.id.mathAnswerInput)
            mathAnswerInput.text.clear()

            findViewById<Button>(R.id.mathSubmitButton).setOnClickListener {
                val userInput = mathAnswerInput.text.toString()
                if (userInput.isNotEmpty() && userInput.toIntOrNull() == answer) {
                    Log.d("DismissActivity_Math", "Math answer correct for alarm ID: $alarmItemId. Dismissing.")
                    dismissAlarm()
                } else {
                    Log.d("DismissActivity_Math", "Math answer incorrect for alarm ID: $alarmItemId.")
                    Toast.makeText(this, "Wrong answer, try again!", Toast.LENGTH_SHORT).show()
                    mathAnswerInput.text.clear()
                }
            }
        } catch (e: Exception) {
            Log.e("DismissActivity_CRASH", "CRASH in setupMathChallenge for ID: $alarmItemId", e)
            Toast.makeText(this, "Error setting up math challenge.", Toast.LENGTH_SHORT).show()
        }
        Log.d("DismissActivity_Setup", "setupMathChallenge END for alarm ID: $alarmItemId")
    }

    private fun setupLuxChallenge() {
        Log.d("DismissActivity_Setup", "setupLuxChallenge BEGIN for alarm ID: $alarmItemId")
        try {
            luxChallengeLayout.visibility = View.VISIBLE
            lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            if (lightSensor != null) {
                val registered = sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
                Log.d("DismissActivity_Setup", "Light sensor listener registered for LUX: $registered for ID: $alarmItemId")
            } else {
                Log.e("DismissActivity_Setup", "Light sensor not available for ID: $alarmItemId. Cannot setup LUX challenge.")
                Toast.makeText(this, "Light sensor not available!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("DismissActivity_CRASH", "CRASH in setupLuxChallenge for ID: $alarmItemId", e)
            Toast.makeText(this, "Error setting up lux challenge.", Toast.LENGTH_SHORT).show()
        }
        Log.d("DismissActivity_Setup", "setupLuxChallenge END for alarm ID: $alarmItemId")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (alarmItemId == -1L) return // Don't process if alarmId is not properly set

        if (isShakeChallengeActive && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = sqrt(x * x + y * y + z * z)

            if (acceleration > 20) {
                shakeProgress += 10
                shakeProgressBar.progress = shakeProgress
                if (shakeProgress >= 100) {
                    Log.d("DismissActivity_Shake", "Shake challenge COMPLETED for alarm ID: $alarmItemId. Dismissing.")
                    dismissAlarm()
                }
            }
        } else if (isLuxChallengeActive && event.sensor.type == Sensor.TYPE_LIGHT) {
            val luxValue = event.values[0]
            if (luxValue > LUX_THRESHOLD_BRIGHT) {
                Log.d("DismissActivity_Lux", "LUX threshold MET for alarm ID: $alarmItemId. Dismissing.")
                dismissAlarm()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.d("DismissActivity_Sensor", "onAccuracyChanged: sensor=${sensor?.name}, accuracy=$accuracy, alarmId=$alarmItemId")
    }

    override fun onStart() {
        super.onStart()
        Log.d("DismissActivity_Lifecycle", "onStart for alarm ID: $alarmItemId")
    }

    override fun onResume() {
        super.onResume()
        Log.d("DismissActivity_Lifecycle", "onResume BEGIN for alarm ID: $alarmItemId. Re-registering listeners if active...")
        if (alarmItemId == -1L) {
            Log.e("DismissActivity_Lifecycle", "onResume called with invalid alarmItemId. Cannot re-register listeners.")
            return
        }
        try {
            if (isShakeChallengeActive && accelerometer != null) {
                val registered = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
                Log.d("DismissActivity_Lifecycle", "Accelerometer listener RE-REGISTERED in onResume: $registered for ID: $alarmItemId")
            }
            if (isLuxChallengeActive && lightSensor != null) {
                val registered = sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
                Log.d("DismissActivity_Lifecycle", "Light sensor listener RE-REGISTERED in onResume: $registered for ID: $alarmItemId")
            }
        } catch (e: Exception) {
            Log.e("DismissActivity_CRASH", "CRASH in onResume while re-registering listeners for ID: $alarmItemId", e)
        }
        Log.d("DismissActivity_Lifecycle", "onResume END for alarm ID: $alarmItemId")
    }

    override fun onPause() {
        super.onPause()
        Log.d("DismissActivity_Lifecycle", "onPause BEGIN for alarm ID: $alarmItemId. Unregistering ALL listeners.")
        if (alarmItemId == -1L) {
            Log.w("DismissActivity_Lifecycle", "onPause called with invalid alarmItemId. SensorManager might not be initialized.")
        } else {
            try {
                sensorManager.unregisterListener(this)
            } catch (e: Exception) {
                Log.e("DismissActivity_CRASH", "CRASH in onPause while unregistering listeners for ID: $alarmItemId", e)
            }
        }
        Log.d("DismissActivity_Lifecycle", "All sensor listeners UNREGISTERED (or attempt) in onPause for ID: $alarmItemId")
        Log.d("DismissActivity_Lifecycle", "onPause END for alarm ID: $alarmItemId")
    }

    override fun onStop() {
        super.onStop()
        Log.d("DismissActivity_Lifecycle", "onStop for alarm ID: $alarmItemId")
    }

    private fun showOverLockscreen() {
        Log.d("DismissActivity_LockScreen", "Attempting to show over lock screen for alarm ID: $alarmItemId")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                Log.d("DismissActivity_LockScreen", "Using setShowWhenLocked(true) and setTurnScreenOn(true) for ID: $alarmItemId")
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                Log.d("DismissActivity_LockScreen", "Using legacy WindowManager flags for lock screen for ID: $alarmItemId")
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
            Log.d("DismissActivity_LockScreen", "Lock screen flags applied for alarm ID: $alarmItemId")
        } catch (e: Exception) {
            Log.e("DismissActivity_CRASH", "CRASH in showOverLockscreen for ID: $alarmItemId", e)
            Toast.makeText(this, "Error showing over lock screen.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("DismissActivity_Lifecycle", "onDestroy BEGIN for alarm ID: $alarmItemId. Final unregistration.")
        if (alarmItemId == -1L) {
            Log.w("DismissActivity_Lifecycle", "onDestroy called with invalid alarmItemId. SensorManager might not be initialized.")
        } else {
            try {
                sensorManager.unregisterListener(this) // Final attempt
            } catch (e: Exception) {
                Log.e("DismissActivity_CRASH", "CRASH in onDestroy while unregistering listeners for ID: $alarmItemId", e)
            }
        }
        Log.d("DismissActivity_Lifecycle", "All sensor listeners UNREGISTERED (or attempt) in onDestroy for ID: $alarmItemId")
        Log.d("DismissActivity_Lifecycle", "onDestroy END for alarm ID: $alarmItemId")
    }
}
