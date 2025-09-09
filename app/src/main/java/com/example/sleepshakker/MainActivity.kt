package com.example.sleepshakker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val setAlarmButton: Button = findViewById(R.id.setAlarmButton)

        // Set a click listener on the button
        setAlarmButton.setOnClickListener {
            // Create an Intent to open the SetAlarmActivity
            val intent = Intent(this, SetAlarmActivity::class.java)
            // Start the new activity
            startActivity(intent)
        }
    }
}