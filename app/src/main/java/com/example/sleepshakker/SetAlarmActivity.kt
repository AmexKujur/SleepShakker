package com.example.sleepshakker

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope // ADDED
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch // ADDED
import java.util.Calendar
import java.util.Locale

// Data class for dismiss options
data class DismissOption(
    val label: String,
    val iconResId: Int,
    val typeIdentifier: String // e.g., "SHAKE", "MATH"
)

class SetAlarmActivity : AppCompatActivity() {

    private lateinit var viewModel: SetAlarmViewModel
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var amPmGroup: RadioGroup
    private lateinit var dailySwitch: SwitchMaterial
    // The following lines related to individual day chips and ChipGroup are being removed
    // as per the user's request to simplify to just a "daily" switch.
    // private lateinit var dayChipGroup: ChipGroup
    // private lateinit var chipSunday: Chip
    // private lateinit var chipMonday: Chip
    // private lateinit var chipTuesday: Chip
    // private lateinit var chipWednesday: Chip
    // private lateinit var chipThursday: Chip
    // private lateinit var chipFriday: Chip
    // private lateinit var chipSaturday: Chip
    // private lateinit var dayChips: List<Chip>

    // Flags to prevent listener loops for day selection - No longer needed
    // private var isDailySwitchUpdatingChips = false
    // private var isChipUpdatingDailySwitch = false

    // Dismiss options
    private lateinit var dismissOptionsRadioGroup: RadioGroup
    private var selectedDismissOptionType: String = "SHAKE" // Default
    private val dismissOptionsList = listOf(
        DismissOption("Shake Phone", R.drawable.ic_vibration, "SHAKE"),
        DismissOption("Math Quiz", android.R.drawable.ic_dialog_info, "MATH"),
        DismissOption("Turn On Lights", R.drawable.ic_lightbulb, "LUX_CHALLENGE")
    )

    // ADDED: Database Access Object
    private lateinit var alarmDao: AlarmDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_alarm)

        val alarmScheduler = AlarmScheduler(this)
        val viewModelFactory = SetAlarmViewModelFactory(alarmScheduler)
        viewModel = ViewModelProvider(this, viewModelFactory)[SetAlarmViewModel::class.java]

        // ADDED: Initialize alarmDao
        alarmDao = AppDatabase.getDatabase(applicationContext).alarmDao()

        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        amPmGroup = findViewById(R.id.amPmGroup)
        dailySwitch = findViewById(R.id.dailySwitch)
        dismissOptionsRadioGroup = findViewById(R.id.dismissOptionsRadioGroup)

        // Removed findViewById calls for dayChipGroup and individual day chips

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        setupTimePickers()
        // Removed call to setupDaySelectionLogic()
        setupDismissOptions()

        findViewById<Button>(R.id.saveAlarmButton).setOnClickListener {
            saveAlarm()
        }
    }

    private fun setupTimePickers() {
        hourPicker.minValue = 1
        hourPicker.maxValue = 12
        // UPDATED: Added formatter for hours to show double digits (e.g., 01, 02)
        hourPicker.setFormatter { value -> String.format(Locale.getDefault(), "%02d", value) }
        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        minutePicker.setFormatter { value -> String.format(Locale.getDefault(), "%02d", value) }

        val calendar = Calendar.getInstance()
        val currentHour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        hourPicker.value = currentHour24.let { hour24 ->
            if (hour24 == 0 || hour24 == 12) 12 else hour24 % 12
        }
        minutePicker.value = currentMinute
        if (currentHour24 < 12) amPmGroup.check(R.id.amButton) else amPmGroup.check(R.id.pmButton)
    }

    // Removed setupDaySelectionLogic() method as it's no longer needed

    private fun setupDismissOptions() {
        val inflater = LayoutInflater.from(this)
        dismissOptionsRadioGroup.removeAllViews() // Clear any existing views

        var firstRadioButtonId = -1

        dismissOptionsList.forEachIndexed { index, option ->
            val itemView = inflater.inflate(R.layout.dismiss_option_item, dismissOptionsRadioGroup, false) as LinearLayout
            val iconView = itemView.findViewById<ImageView>(R.id.optionIcon)
            val labelView = itemView.findViewById<TextView>(R.id.optionLabel)
            val radioButton = itemView.findViewById<RadioButton>(R.id.optionRadioButton)

            iconView.setImageResource(option.iconResId)
            labelView.text = option.label
            radioButton.id = View.generateViewId() // Essential for RadioGroup
            radioButton.tag = option.typeIdentifier

            if (index == 0) { // Default selection
                firstRadioButtonId = radioButton.id
                selectedDismissOptionType = option.typeIdentifier
            }

            itemView.setOnClickListener {
                if (!radioButton.isChecked) {
                    dismissOptionsRadioGroup.check(radioButton.id)
                }
            }
            dismissOptionsRadioGroup.addView(itemView)
        }

        if (firstRadioButtonId != -1) {
            dismissOptionsRadioGroup.check(firstRadioButtonId)
        }

        dismissOptionsRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val checkedRadioButton = group.findViewById<RadioButton>(checkedId)
            if (checkedRadioButton != null) {
                selectedDismissOptionType = checkedRadioButton.tag as? String ?: "SHAKE" // Fallback
                Log.d("SetAlarmActivity", "Dismiss option selected: $selectedDismissOptionType")
            } else {
                Log.w("SetAlarmActivity", "No RadioButton found for checkedId: $checkedId. This shouldn't happen.")
                selectedDismissOptionType = "SHAKE" // Fallback to a safe default
            }
        }
    }


    private fun saveAlarm() {
        val calendar = Calendar.getInstance()
        val selectedHour = hourPicker.value
        val selectedMinute = minutePicker.value
        val isPm = amPmGroup.checkedRadioButtonId == R.id.pmButton
        var hour24 = selectedHour
        if (isPm && selectedHour < 12) hour24 += 12
        else if (!isPm && selectedHour == 12) hour24 = 0

        calendar.set(Calendar.HOUR_OF_DAY, hour24)
        calendar.set(Calendar.MINUTE, selectedMinute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val repeatDays: MutableSet<Int> = mutableSetOf()
        if (dailySwitch.isChecked) {
            // For daily, set all days of the week (1=Sunday, 7=Saturday)
            (1..7).toSet().forEach { repeatDays.add(it) }
            // Adjust calendar to the first upcoming instance of this time
            while (calendar.timeInMillis < System.currentTimeMillis() || !repeatDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                if (calendar.timeInMillis < System.currentTimeMillis() && repeatDays.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                    calendar.add(Calendar.DAY_OF_YEAR, 7)
                } else {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        } else {
            // One-time alarm, ensure it's in the future
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Create AlarmItem without an ID, let Room generate it
        val alarmItem = AlarmItem(
            timeInMillis = calendar.timeInMillis,
            message = "Alarm for ${String.format("%02d:%02d", selectedHour, selectedMinute)} ${if (isPm) "PM" else "AM"}",
            isEnabled = true,
            repeatDays = if (repeatDays.isEmpty()) null else repeatDays,
            dismissOption = selectedDismissOptionType
        )

        // UPDATED: Use a coroutine to insert into the database
        lifecycleScope.launch {
            // Insert into database and get the new ID
            val newId = alarmDao.insert(alarmItem)
            // Update the alarmItem with the database-generated ID
            val scheduledAlarmItem = alarmItem.copy(id = newId)

            Log.d("SetAlarmActivity", "Saving AlarmItem with dismissOption: ${scheduledAlarmItem.dismissOption}")
            Log.d("SetAlarmActivity", "Saving AlarmItem: $scheduledAlarmItem, calculated time: ${calendar.time}, repeatDays: $repeatDays")

            // Schedule the alarm using the updated AlarmItem with the correct ID
            viewModel.schedule(scheduledAlarmItem)

            finish() // Finish the activity after scheduling and database operation
        }
    }
}