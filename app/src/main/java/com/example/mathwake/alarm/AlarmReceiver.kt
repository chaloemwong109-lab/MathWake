package com.example.mathwake.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Need to reschedule all active alarms
            return
        }

        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val difficulty = intent.getStringExtra("DIFFICULTY") ?: "EASY"
        
        if (alarmId != -1) {
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra("ALARM_ID", alarmId)
                putExtra("DIFFICULTY", difficulty)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}