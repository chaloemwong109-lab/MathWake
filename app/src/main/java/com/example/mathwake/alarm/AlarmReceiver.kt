package com.example.mathwake.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.mathwake.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AlarmDatabase.getDatabase(context).alarmDao()
                    val scheduler = AlarmScheduler(context)
                    dao.getAllAlarms().first().filter { it.isActive }.forEach { alarm ->
                        scheduler.schedule(alarm)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        val alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val difficulty = intent.getStringExtra(AlarmScheduler.EXTRA_DIFFICULTY) ?: "EASY"
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: ""
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 7)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)

        if (alarmId != -1) {
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                action = AlarmService.ACTION_START
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmScheduler.EXTRA_DIFFICULTY, difficulty)
                putExtra(AlarmScheduler.EXTRA_LABEL, label)
                putExtra(AlarmScheduler.EXTRA_HOUR, hour)
                putExtra(AlarmScheduler.EXTRA_MINUTE, minute)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}