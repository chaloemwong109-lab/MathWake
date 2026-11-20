package com.example.mathwake.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.mathwake.MainActivity
import com.example.mathwake.R

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var isPaused = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                pauseAlarm()
                return START_STICKY
            }
            ACTION_RESUME -> {
                resumeAlarm()
                return START_STICKY
            }
            ACTION_STOP -> {
                stopAlarmAndSelf()
                return START_NOT_STICKY
            }
        }

        val alarmId = intent?.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        val difficulty = intent?.getStringExtra(AlarmScheduler.EXTRA_DIFFICULTY) ?: "EASY"
        val label = intent?.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: ""
        val hour = intent?.getIntExtra(AlarmScheduler.EXTRA_HOUR, 7) ?: 7
        val minute = intent?.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0) ?: 0

        isAlarmActive = true
        isPaused = false
        createNotificationChannel()

        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("IS_RINGING", true)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_DIFFICULTY, difficulty)
            putExtra(AlarmScheduler.EXTRA_LABEL, label)
            putExtra(AlarmScheduler.EXTRA_HOUR, hour)
            putExtra(AlarmScheduler.EXTRA_MINUTE, minute)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = label.ifBlank { getString(R.string.notification_alarm_desc) }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_alarm_title))
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startAlarmSoundAndVibration()

        try {
            startActivity(fullScreenIntent)
        } catch (_: Exception) {
            // ระบบจะใช้ fullScreenIntent ผ่าน Notification แทนเมื่อแอปอยู่เบื้องหลัง
        }

        return START_STICKY
    }

    private fun startAlarmSoundAndVibration() {
        stopMediaAndVibration()

        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startVibration()
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
    }

    private fun pauseAlarm() {
        isPaused = true
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (_: Exception) {}
        vibrator?.cancel()
    }

    private fun resumeAlarm() {
        if (!isAlarmActive) return
        if (isPaused) {
            isPaused = false
            try {
                if (mediaPlayer != null) {
                    mediaPlayer?.start()
                    startVibration()
                } else {
                    startAlarmSoundAndVibration()
                }
            } catch (_: Exception) {
                startAlarmSoundAndVibration()
            }
        }
    }

    private fun stopMediaAndVibration() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun stopAlarmAndSelf() {
        isAlarmActive = false
        isPaused = false
        stopMediaAndVibration()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        isAlarmActive = false
        stopMediaAndVibration()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setSound(null, null)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "MATHWAKE_ALARM_CHANNEL_V2"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.example.mathwake.ACTION_START"
        const val ACTION_PAUSE = "com.example.mathwake.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.mathwake.ACTION_RESUME"
        const val ACTION_STOP = "com.example.mathwake.ACTION_STOP"

        @Volatile
        var isAlarmActive: Boolean = false
    }
}