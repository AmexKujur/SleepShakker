package com.example.sleepshakker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlarmAdapter(
    private val onAlarmToggle: (AlarmItem) -> Unit,
    private val onDeleteClick: (AlarmItem) -> Unit
) : ListAdapter<AlarmItem, AlarmAdapter.AlarmViewHolder>(AlarmDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = getItem(position)
        holder.bind(alarm, onAlarmToggle, onDeleteClick)
    }

    class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeTextView: TextView = itemView.findViewById(R.id.alarmTimeTextView)
        private val messageTextView: TextView = itemView.findViewById(R.id.alarmMessageTextView)
        private val repeatDaysTextView: TextView = itemView.findViewById(R.id.alarmRepeatDaysTextView)
        private val enabledSwitch: SwitchMaterial = itemView.findViewById(R.id.alarmEnabledSwitch)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteAlarmButton)

        fun bind(
            alarm: AlarmItem,
            onAlarmToggle: (AlarmItem) -> Unit,
            onDeleteClick: (AlarmItem) -> Unit
        ) {
            val calendar = Calendar.getInstance().apply { timeInMillis = alarm.timeInMillis }
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            timeTextView.text = timeFormat.format(calendar.time)
            messageTextView.text = alarm.message

            if (alarm.repeatDays.isNullOrEmpty()) {
                repeatDaysTextView.text = "One-time"
            } else if (alarm.repeatDays?.size == 7) {
                repeatDaysTextView.text = "Daily"
            } else {
                repeatDaysTextView.text = formatRepeatDays(alarm.repeatDays)
            }

            enabledSwitch.isChecked = alarm.isEnabled
            enabledSwitch.setOnCheckedChangeListener(null) // Important to prevent listener loops
            enabledSwitch.isChecked = alarm.isEnabled
            enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                onAlarmToggle(alarm.copy(isEnabled = isChecked))
            }

            deleteButton.setOnClickListener {
                onDeleteClick(alarm)
            }
        }

        private fun formatRepeatDays(days: Set<Int>?): String {
            if (days.isNullOrEmpty()) return "One-time"
            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            // Sort days to ensure consistent order: SUNDAY (1) to SATURDAY (7)
            return days.sorted().map { dayNames[it - Calendar.SUNDAY] }.joinToString(", ")
        }
    }
}

class AlarmDiffCallback : DiffUtil.ItemCallback<AlarmItem>() {
    override fun areItemsTheSame(oldItem: AlarmItem, newItem: AlarmItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: AlarmItem, newItem: AlarmItem): Boolean {
        return oldItem == newItem
    }
}
