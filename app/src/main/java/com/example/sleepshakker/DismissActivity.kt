package com.example.sleepshakker

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
    private val LUX_THRESHOLD_BRIGHT = 150f // Test and adjust this value

    private var alarmItemId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dismiss)

        alarmItemId = intent.getLongExtra("ALARM_ITEM_ID", -1L)
        Log.d("DismissActivity", "Received alarm item ID: $alarmItemId")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Find UI elements for all challenges
        shakeProgressBar = findViewById(R.id.shakeProgressBar) // Assuming it's always in the layout
        luxChallengeLayout = findViewById(R.id.luxChallengeLayout)


        // Make the activity show over the lock screen
        showOverLockscreen()

        val challengeType = intent.getStringExtra("CHALLENGE_TYPE") ?: "SHAKE" // Default to SHAKE
        Log.d("DismissActivity", "Challenge type: $challengeType")

        // Hide all challenge layouts initially
        findViewById<LinearLayout>(R.id.shakeChallengeLayout).visibility = View.GONE
        findViewById<LinearLayout>(R.id.mathChallengeLayout).visibility = View.GONE
        luxChallengeLayout.visibility = View.GONE


        when (challengeType) {
            "SHAKE" -> {
                isShakeChallengeActive = true
                setupShakeChallenge()
            }
            "MATH" -> setupMathChallenge()
            "LUX_CHALLENGE" -> {
                isLuxChallengeActive = true
                setupLuxChallenge()
            }
            else -> {
                Log.w("DismissActivity", "Unknown challenge type '$challengeType', defaulting to SHAKE.")
                isShakeChallengeActive = true
                setupShakeChallenge()
            }
        }
    }

    private fun dismissAlarm() {
        Log.d("DismissActivity", "Dismissing alarm ID: $alarmItemId")
        AlarmReceiver.ringtone?.stop()
        AlarmReceiver.ringtone = null

        // Unregister all listeners
        sensorManager.unregisterListener(this)
        Log.d("DismissActivity", "All sensor listeners unregistered in dismissAlarm.")


        isShakeChallengeActive = false // Reset flags
        isLuxChallengeActive = false

        Toast.makeText(this, "Alarm Dismissed!", Toast.LENGTH_SHORT).show()
        finish()
    }

    // --- Challenge Setup Methods ---

    private fun setupShakeChallenge() {
        Log.d("DismissActivity", "Setting up SHAKE challenge.")
        findViewById<LinearLayout>(R.id.shakeChallengeLayout).visibility = View.VISIBLE
        // shakeProgressBar is already found in onCreate
        shakeProgressBar.progress = 0
        shakeProgress = 0
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            Log.d("DismissActivity", "Accelerometer listener registered for SHAKE.")
        } else {
            Log.e("DismissActivity", "Accelerometer not available. Cannot setup shake challenge.")
            Toast.makeText(this, "Shake sensor not available!", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupMathChallenge() {
        Log.d("DismissActivity", "Setting up MATH challenge.")
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
                dismissAlarm()
            } else {
                Toast.makeText(this, "Wrong answer, try again!", Toast.LENGTH_SHORT).show()
                mathAnswerInput.text.clear()
            }
        }
    }

    private fun setupLuxChallenge() {
        Log.d("DismissActivity", "Setting up LUX challenge.")
        luxChallengeLayout.visibility = View.VISIBLE
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("DismissActivity", "Light sensor listener registered for LUX.")
        } else {
            Log.e("DismissActivity", "Light sensor not available. Cannot setup LUX challenge.")
            Toast.makeText(this, "Light sensor not available!", Toast.LENGTH_LONG).show()
            // Consider a fallback or auto-dismiss if essential sensor is missing
        }
    }


    // --- SensorEventListener Methods ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (isShakeChallengeActive && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val acceleration = sqrt(x * x + y * y + z * z)

            if (acceleration > 20) { // Adjusted threshold
                shakeProgress += 10
                shakeProgressBar.progress = shakeProgress
                if (shakeProgress >= 100) {
                    dismissAlarm()
                }
            }
        } else if (isLuxChallengeActive && event.sensor.type == Sensor.TYPE_LIGHT) {
            val luxValue = event.values[0]
            Log.d("DismissActivityLux", "Current LUX: $luxValue") // For debugging
            if (luxValue > LUX_THRESHOLD_BRIGHT) {
                dismissAlarm()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }

    // --- Lifecycle Methods ---

    override fun onPause() {
        super.onPause()
        // Unregister all active listeners
        sensorManager.unregisterListener(this)
        Log.d("DismissActivity", "All sensor listeners unregistered in onPause.")
    }

    override fun onResume() {
        super.onResume()
        // Re-register listeners for active challenges
        if (isShakeChallengeActive && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            Log.d("DismissActivity", "Accelerometer listener re-registered in onResume.")
        }
        if (isLuxChallengeActive && lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d("DismissActivity", "Light sensor listener re-registered in onResume.")
        }
    }

    private fun showOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Final unregistration, though onPause should handle most cases
        sensorManager.unregisterListener(this)
        Log.d("DismissActivity", "All sensor listeners unregistered in onDestroy.")
        // AlarmReceiver.ringtone?.stop() // Already handled in dismissAlarm
        // AlarmReceiver.ringtone = null
        Log.d("DismissActivity", "onDestroy called for alarm ID: $alarmItemId")
    }
}
