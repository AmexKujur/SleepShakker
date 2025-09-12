package com.example.sleepshakker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var alarmsRecyclerView: RecyclerView
    private lateinit var setAlarmButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ViewModel
        val factory = MainViewModelFactory(application)
        mainViewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Find views
        alarmsRecyclerView = findViewById(R.id.alarmsRecyclerView)
        setAlarmButton = findViewById(R.id.setAlarmButton)

        // Set up RecyclerView
        setupRecyclerView()

        // Observe alarms from ViewModel
        mainViewModel.allAlarms.observe(this) { alarms ->
            alarms?.let {
                alarmAdapter.submitList(it)
            }
        }

        // Set listener for the "Set Alarm" button
        setAlarmButton.setOnClickListener {
            val intent = Intent(this, SetAlarmActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        alarmAdapter = AlarmAdapter(
            onAlarmToggle = { alarmItem ->
                mainViewModel.updateAlarm(alarmItem)
            },
            onDeleteClick = { alarmItem ->
                mainViewModel.deleteAlarm(alarmItem)
            }
        )
        alarmsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = alarmAdapter
        }
    }
}