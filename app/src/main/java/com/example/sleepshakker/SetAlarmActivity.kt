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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
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
    private lateinit var dayChipGroup: ChipGroup
    private lateinit var chipSunday: Chip
    private lateinit var chipMonday: Chip
    private lateinit var chipTuesday: Chip
    private lateinit var chipWednesday: Chip
    private lateinit var chipThursday: Chip
    private lateinit var chipFriday: Chip
    private lateinit var chipSaturday: Chip

    private lateinit var dayChips: List<Chip>

    // Flags to prevent listener loops for day selection
    private var isDailySwitchUpdatingChips = false
    private var isChipUpdatingDailySwitch = false

    // Dismiss options
    private lateinit var dismissOptionsRadioGroup: RadioGroup
    private var selectedDismissOptionType: String = "SHAKE" // Default
    private val dismissOptionsList = listOf(
        DismissOption("Shake Phone", R.drawable.ic_vibration, "SHAKE"), // Replace R.drawable.ic_vibration with actual shake icon
        DismissOption("Math Quiz", android.R.drawable.ic_dialog_info, "MATH"), // Replace with actual math icon
        DismissOption("Turn On Lights", R.drawable.ic_lightbulb, "LUX_CHALLENGE")
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_alarm)

        val alarmScheduler = AlarmScheduler(this)
        val viewModelFactory = SetAlarmViewModelFactory(alarmScheduler)
        viewModel = ViewModelProvider(this, viewModelFactory)[SetAlarmViewModel::class.java]

        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        amPmGroup = findViewById(R.id.amPmGroup)
        dailySwitch = findViewById(R.id.dailySwitch)
        dayChipGroup = findViewById(R.id.dayChipGroup)
        dismissOptionsRadioGroup = findViewById(R.id.dismissOptionsRadioGroup)

        chipSunday = findViewById(R.id.chipSunday)
        chipMonday = findViewById(R.id.chipMonday)
        chipTuesday = findViewById(R.id.chipTuesday)
        chipWednesday = findViewById(R.id.chipWednesday)
        chipThursday = findViewById(R.id.chipThursday)
        chipFriday = findViewById(R.id.chipFriday)
        chipSaturday = findViewById(R.id.chipSaturday)
        dayChips = listOf(chipSunday, chipMonday, chipTuesday, chipWednesday, chipThursday, chipFriday, chipSaturday)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        setupTimePickers()
        setupDaySelectionLogic()
        setupDismissOptions()

        findViewById<Button>(R.id.saveAlarmButton).setOnClickListener {
            saveAlarm()
        }
    }

    private fun setupTimePickers() {
        hourPicker.minValue = 1
        hourPicker.maxValue = 12
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

    private fun setupDaySelectionLogic() {
        dailySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChipUpdatingDailySwitch) return@setOnCheckedChangeListener
            isDailySwitchUpdatingChips = true
            dayChips.forEach { it.isChecked = isChecked }
            isDailySwitchUpdatingChips = false
        }
        dayChips.forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ ->
                if (isDailySwitchUpdatingChips) return@setOnCheckedChangeListener
                isChipUpdatingDailySwitch = true
                dailySwitch.isChecked = dayChips.all { it.isChecked }
                isChipUpdatingDailySwitch = false
            }
        }
    }

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
                    dismissOptionsRadioGroup.check(radioButton.id) // Corrected line
                }
            }
            dismissOptionsRadioGroup.addView(itemView)
        }

        if (firstRadioButtonId != -1) {
            dismissOptionsRadioGroup.check(firstRadioButtonId)
        }

        dismissOptionsRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val checkedRadioButton = group.findViewById<RadioButton>(checkedId)
            if (checkedRadioButton != null) { // Check if a radio button was actually found
                selectedDismissOptionType = checkedRadioButton.tag as? String ?: "SHAKE" // Fallback
                Log.d("SetAlarmActivity", "Dismiss option selected: $selectedDismissOptionType")
            } else {
                Log.w("SetAlarmActivity", "No RadioButton found for checkedId: $checkedId. This shouldn't happen.")
                // Handle case where no radio button is found, perhaps default
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

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val repeatDays = mutableSetOf<Int>()
        if (dailySwitch.isChecked) {
            // For daily, can be an empty set or all days, depends on scheduler logic
        } else {
            if (chipSunday.isChecked) repeatDays.add(Calendar.SUNDAY)
            if (chipMonday.isChecked) repeatDays.add(Calendar.MONDAY)
            if (chipTuesday.isChecked) repeatDays.add(Calendar.TUESDAY)
            if (chipWednesday.isChecked) repeatDays.add(Calendar.WEDNESDAY)
            if (chipThursday.isChecked) repeatDays.add(Calendar.THURSDAY)
            if (chipFriday.isChecked) repeatDays.add(Calendar.FRIDAY)
            if (chipSaturday.isChecked) repeatDays.add(Calendar.SATURDAY)
        }

        val alarmId = System.currentTimeMillis()
        val alarmItem = AlarmItem(
            id = alarmId,
            timeInMillis = calendar.timeInMillis,
            message = "Alarm for ${String.format("%02d:%02d", hourPicker.value, minutePicker.value)} ${if (isPm) "PM" else "AM"}",
            isEnabled = true,
            repeatDays = if (repeatDays.isEmpty() && !dailySwitch.isChecked && !dayChips.any { it.isChecked }) null else repeatDays,
            dismissOption = selectedDismissOptionType // Use the selected dismiss option
        )
        Log.d("SetAlarmActivity", "Saving AlarmItem: $alarmItem")
        viewModel.schedule(alarmItem)
        finish()
    }
}

