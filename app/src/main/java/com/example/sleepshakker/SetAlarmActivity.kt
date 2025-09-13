package com.example.sleepshakker

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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.button.MaterialButton // Added for MaterialButton
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

// Data class for dismiss options (remains the same)
data class DismissOption(
    val label: String,
    val iconResId: Int,
    val typeIdentifier: String
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

    private var isDailySwitchUpdatingChips = false
    private var isChipUpdatingDailySwitch = false

    private lateinit var dismissOptionsRadioGroup: RadioGroup
    private var selectedDismissOptionType: String = "SHAKE" // Default
    private val dismissOptionsList = listOf(
        DismissOption("Shake Phone", R.drawable.ic_vibration, "SHAKE"),
        DismissOption("Math Quiz", android.R.drawable.ic_dialog_info, "MATH"),
        DismissOption("Turn On Lights", R.drawable.ic_lightbulb, "LUX_CHALLENGE")
    )

    private lateinit var alarmDao: AlarmDao
    private lateinit var saveAlarmButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_alarm)

        val alarmScheduler = AlarmScheduler(this)
        val viewModelFactory = SetAlarmViewModelFactory(alarmScheduler)
        viewModel = ViewModelProvider(this, viewModelFactory)[SetAlarmViewModel::class.java]

        alarmDao = AppDatabase.getDatabase(applicationContext).alarmDao()

        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        amPmGroup = findViewById(R.id.amPmGroup)
        dailySwitch = findViewById(R.id.dailySwitch)
        dismissOptionsRadioGroup = findViewById(R.id.dismissOptionsRadioGroup)
        saveAlarmButton = findViewById(R.id.saveAlarmButton)

        dayChipGroup = findViewById(R.id.dayChipGroup)
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

        saveAlarmButton.setOnClickListener {
            saveAlarm()
        }
    }

    private fun setupTimePickers() {
        hourPicker.minValue = 1
        hourPicker.maxValue = 12
        // --- Add Formatter for Hour Picker ---
        hourPicker.setFormatter { value ->
            String.format(Locale.getDefault(), "%02d", value)
        }
        // --- End Formatter for Hour Picker ---

        minutePicker.minValue = 0
        minutePicker.maxValue = 59
        // --- Add Formatter for Minute Picker ---
        minutePicker.setFormatter { value ->
            String.format(Locale.getDefault(), "%02d", value)
        }
        // --- End Formatter for Minute Picker ---

        val calendar = Calendar.getInstance()
        val currentHour24 = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        hourPicker.value = when (currentHour24) {
            0 -> 12 // Midnight
            in 1..12 -> currentHour24
            else -> currentHour24 - 12 // PM hours
        }
        minutePicker.value = currentMinute

        if (currentHour24 < 12) {
            amPmGroup.check(R.id.amButton)
        } else {
            amPmGroup.check(R.id.pmButton)
        }
    }

    private fun setupDaySelectionLogic() {
        dailySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isDailySwitchUpdatingChips) return@setOnCheckedChangeListener
            isChipUpdatingDailySwitch = true
            dayChips.forEach { it.isChecked = isChecked }
            isChipUpdatingDailySwitch = false
        }

        dayChips.forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ ->
                if (isChipUpdatingDailySwitch) return@setOnCheckedChangeListener
                isDailySwitchUpdatingChips = true
                dailySwitch.isChecked = dayChips.all { it.isChecked }
                isDailySwitchUpdatingChips = false
            }
        }
        isDailySwitchUpdatingChips = true
        dailySwitch.isChecked = dayChips.all { it.isChecked }
        isDailySwitchUpdatingChips = false
    }

    private fun setupDismissOptions() {
        val inflater = LayoutInflater.from(this)
        dismissOptionsRadioGroup.removeAllViews()
        var firstRadioButtonId = -1
        dismissOptionsList.forEachIndexed { index, option ->
            val itemView = inflater.inflate(R.layout.dismiss_option_item, dismissOptionsRadioGroup, false) as LinearLayout
            val iconView = itemView.findViewById<ImageView>(R.id.optionIcon)
            val labelView = itemView.findViewById<TextView>(R.id.optionLabel)
            val radioButton = itemView.findViewById<RadioButton>(R.id.optionRadioButton)

            iconView.setImageResource(option.iconResId)
            labelView.text = option.label
            radioButton.id = View.generateViewId()
            radioButton.tag = option.typeIdentifier

            if (index == 0) {
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
                selectedDismissOptionType = checkedRadioButton.tag as? String ?: "SHAKE"
                Log.d("SetAlarmActivity", "Dismiss option selected: $selectedDismissOptionType")
            } else {
                selectedDismissOptionType = "SHAKE"
            }
        }
    }

    private fun saveAlarm() {
        val baseCalendar = Calendar.getInstance()
        val selectedHour12 = hourPicker.value
        val selectedMinute = minutePicker.value
        val isPm = amPmGroup.checkedRadioButtonId == R.id.pmButton

        var hour24 = selectedHour12
        if (isPm && selectedHour12 < 12) {
            hour24 = selectedHour12 + 12
        } else if (!isPm && selectedHour12 == 12) {
            hour24 = 0
        }

        baseCalendar.set(Calendar.HOUR_OF_DAY, hour24)
        baseCalendar.set(Calendar.MINUTE, selectedMinute)
        baseCalendar.set(Calendar.SECOND, 0)
        baseCalendar.set(Calendar.MILLISECOND, 0)

        val finalRepeatDays: MutableSet<Int>?
        val targetSelectedDays = mutableSetOf<Int>()
        if (chipSunday.isChecked) targetSelectedDays.add(Calendar.SUNDAY)
        if (chipMonday.isChecked) targetSelectedDays.add(Calendar.MONDAY)
        if (chipTuesday.isChecked) targetSelectedDays.add(Calendar.TUESDAY)
        if (chipWednesday.isChecked) targetSelectedDays.add(Calendar.WEDNESDAY)
        if (chipThursday.isChecked) targetSelectedDays.add(Calendar.THURSDAY)
        if (chipFriday.isChecked) targetSelectedDays.add(Calendar.FRIDAY)
        if (chipSaturday.isChecked) targetSelectedDays.add(Calendar.SATURDAY)

        val alarmTimeCalendar: Calendar

        if (dailySwitch.isChecked) {
            finalRepeatDays = (Calendar.SUNDAY..Calendar.SATURDAY).toMutableSet()
            alarmTimeCalendar = calculateNextAlarmTime(baseCalendar.clone() as Calendar, finalRepeatDays)
        } else {
            if (targetSelectedDays.isNotEmpty()) {
                finalRepeatDays = targetSelectedDays
                alarmTimeCalendar = calculateNextAlarmTime(baseCalendar.clone() as Calendar, finalRepeatDays)
            } else {
                finalRepeatDays = null
                alarmTimeCalendar = calculateNextAlarmTime(baseCalendar.clone() as Calendar, null)
            }
        }

        val amPmString = if (amPmGroup.checkedRadioButtonId == R.id.amButton) "AM" else "PM"
        // Use selectedHour12 for display to correctly show 12 for 12 AM/PM
        val displayHour = selectedHour12

        val alarmItem = AlarmItem(
            timeInMillis = alarmTimeCalendar.timeInMillis,
            message = "Alarm for ${String.format(Locale.getDefault(), "%02d:%02d", displayHour, selectedMinute)} $amPmString",
            isEnabled = true,
            repeatDays = finalRepeatDays,
            dismissOption = selectedDismissOptionType
        )

        lifecycleScope.launch {
            val newId = alarmDao.insert(alarmItem)
            val scheduledAlarmItem = alarmItem.copy(id = newId)
            Log.d("SetAlarmActivity", "Saving AlarmItem: $scheduledAlarmItem, calculated time: ${alarmTimeCalendar.time}, repeatDays: $finalRepeatDays")
            viewModel.schedule(scheduledAlarmItem)
            finish()
        }
    }

    private fun calculateNextAlarmTime(calendar: Calendar, targetDays: Set<Int>?): Calendar {
        val now = System.currentTimeMillis()

        if (targetDays == null || targetDays.isEmpty() || targetDays.size == 7) {
            if (calendar.timeInMillis < now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar
        }

        val clonedCalendar = calendar.clone() as Calendar
        while (true) {
            val dayOfWeek = clonedCalendar.get(Calendar.DAY_OF_WEEK)

            if (targetDays.contains(dayOfWeek)) {
                if (clonedCalendar.timeInMillis >= now) {
                    return clonedCalendar
                }
            }
            clonedCalendar.add(Calendar.DAY_OF_YEAR, 1)

            if (clonedCalendar.timeInMillis > now + (366L * 24 * 60 * 60 * 1000)) {
                Log.e("SetAlarmActivity", "Alarm calculation exceeded 1 year in future, using immediate next valid time as fallback.")
                val fallbackCalendar = calendar.clone() as Calendar
                if (fallbackCalendar.timeInMillis < now) {
                    fallbackCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return fallbackCalendar
            }
        }
    }
}
