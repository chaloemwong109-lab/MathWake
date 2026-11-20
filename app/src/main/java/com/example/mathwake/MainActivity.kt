package com.example.mathwake

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mathwake.alarm.AlarmScheduler
import com.example.mathwake.alarm.AlarmService
import com.example.mathwake.data.Alarm
import com.example.mathwake.data.AlarmDatabase
import com.example.mathwake.data.MathDifficulty
import com.example.mathwake.ui.screens.AddEditAlarmScreen
import com.example.mathwake.ui.screens.AlarmListScreen
import com.example.mathwake.ui.screens.AlarmRingingScreen
import com.example.mathwake.ui.theme.MathWakeTheme
import kotlinx.coroutines.launch

private data class RingingPayload(
    val alarmId: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val difficulty: MathDifficulty
)

class MainActivity : ComponentActivity() {

    private var pendingRinging by mutableStateOf<RingingPayload?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        enableEdgeToEdge()

        pendingRinging = extractRingingPayload(intent)

        val database = AlarmDatabase.getDatabase(applicationContext)
        val alarmDao = database.alarmDao()
        val alarmScheduler = AlarmScheduler(applicationContext)

        setContent {
            MathWakeTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val alarms by alarmDao.getAllAlarms().collectAsState(initial = emptyList())

                var showOverlayDialog by rememberSaveable { mutableStateOf(false) }
                var showFullScreenDialog by rememberSaveable { mutableStateOf(false) }
                var hasCheckedPermissions by rememberSaveable { mutableStateOf(false) }

                fun checkPopupPermissions() {
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        showOverlayDialog = true
                    } else if (needsFullScreenPermission()) {
                        showFullScreenDialog = true
                    }
                }

                // เมื่อผู้ใช้กลับมาจากหน้าตั้งค่า Display over other apps ให้เช็คสิทธิ์ถัดไปต่อ
                val overlaySettingsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    if (Settings.canDrawOverlays(this@MainActivity) && needsFullScreenPermission()) {
                        showFullScreenDialog = true
                    }
                }

                val notificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {
                    checkPopupPermissions()
                }

                // เช็คสิทธิ์เฉพาะตอนเปิดแอปปกติครั้งแรก ไม่แทรกตอนปลุกดังหรือตอนหมุนจอ
                LaunchedEffect(Unit) {
                    if (pendingRinging != null || hasCheckedPermissions) return@LaunchedEffect
                    hasCheckedPermissions = true

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            checkPopupPermissions()
                        }
                    } else {
                        checkPopupPermissions()
                    }
                }

                // Dialog แนะนำก่อนพาไปหน้า Display over other apps
                if (showOverlayDialog) {
                    AlertDialog(
                        onDismissRequest = { showOverlayDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.permission_overlay_title),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.permission_overlay_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showOverlayDialog = false
                                    val overlayIntent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                    overlaySettingsLauncher.launch(overlayIntent)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.permission_go_to_settings))
                            }
                        },
                        dismissButton = {
                            FilledTonalButton(
                                onClick = { showOverlayDialog = false },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.permission_later))
                            }
                        }
                    )
                }

                // Dialog แนะนำก่อนพาไปหน้า Full-Screen Intent (Android 14+)
                if (showFullScreenDialog) {
                    AlertDialog(
                        onDismissRequest = { showFullScreenDialog = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = {
                            Text(
                                text = stringResource(R.string.permission_fullscreen_title),
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Text(
                                text = stringResource(R.string.permission_fullscreen_desc),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showFullScreenDialog = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                        val fullScreenIntent = Intent(
                                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                            Uri.parse("package:$packageName")
                                        )
                                        startActivity(fullScreenIntent)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.permission_go_to_settings))
                            }
                        },
                        dismissButton = {
                            FilledTonalButton(
                                onClick = { showFullScreenDialog = false },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.permission_later))
                            }
                        }
                    )
                }

                // เมื่อมีปลุกดังเข้ามา ให้สลับไปหน้าโจทย์และเปิดหน้าจอค้างไว้จนกว่าจะตอบถูก
                val currentRinging = pendingRinging
                LaunchedEffect(currentRinging) {
                    if (currentRinging != null) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        if (navController.currentDestination?.route != "ringing") {
                            navController.navigate("ringing") {
                                launchSingleTop = true
                            }
                        }
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = if (currentRinging != null) "ringing" else "list"
                    ) {
                        composable("list") {
                            AlarmListScreen(
                                alarms = alarms,
                                onNavigateToAdd = { navController.navigate("add_edit/-1") },
                                onNavigateToEdit = { id -> navController.navigate("add_edit/$id") },
                                onToggleAlarm = { alarm, enabled ->
                                    scope.launch {
                                        val updated = alarm.copy(isActive = enabled)
                                        alarmDao.updateAlarm(updated)
                                        if (enabled) {
                                            alarmScheduler.schedule(updated)
                                        } else {
                                            alarmScheduler.cancel(updated)
                                        }
                                    }
                                },
                                onTestAlarm = { alarm ->
                                    val testHour = alarm?.hour ?: 7
                                    val testMinute = alarm?.minute ?: 0
                                    val testLabel = alarm?.label ?: ""
                                    val testDiff = alarm?.difficulty ?: MathDifficulty.MEDIUM
                                    val testId = alarm?.id ?: 9999

                                    val serviceIntent = Intent(this@MainActivity, AlarmService::class.java).apply {
                                        action = AlarmService.ACTION_START
                                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, testId)
                                        putExtra(AlarmScheduler.EXTRA_DIFFICULTY, testDiff.name)
                                        putExtra(AlarmScheduler.EXTRA_LABEL, testLabel)
                                        putExtra(AlarmScheduler.EXTRA_HOUR, testHour)
                                        putExtra(AlarmScheduler.EXTRA_MINUTE, testMinute)
                                    }
                                    startForegroundService(serviceIntent)
                                }
                            )
                        }

                        composable(
                            route = "add_edit/{alarmId}",
                            arguments = listOf(navArgument("alarmId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val argId = backStackEntry.arguments?.getInt("alarmId") ?: -1
                            val isEdit = argId != -1
                            val existingAlarm = remember(alarms, argId) {
                                alarms.firstOrNull { it.id == argId }
                            }

                            AddEditAlarmScreen(
                                existingAlarm = existingAlarm,
                                isEditMode = isEdit,
                                onSave = { hour, minute, label, difficulty ->
                                    scope.launch {
                                        if (existingAlarm != null) {
                                            val updated = existingAlarm.copy(
                                                hour = hour,
                                                minute = minute,
                                                label = label,
                                                difficulty = difficulty,
                                                isActive = true
                                            )
                                            alarmDao.updateAlarm(updated)
                                            alarmScheduler.schedule(updated)
                                        } else {
                                            val newAlarm = Alarm(
                                                hour = hour,
                                                minute = minute,
                                                label = label,
                                                difficulty = difficulty,
                                                isActive = true
                                            )
                                            val newId = alarmDao.insertAlarm(newAlarm).toInt()
                                            alarmScheduler.schedule(newAlarm.copy(id = newId))
                                        }
                                        navController.popBackStack()
                                    }
                                },
                                onDelete = { alarmToDelete ->
                                    scope.launch {
                                        alarmScheduler.cancel(alarmToDelete)
                                        alarmDao.deleteAlarm(alarmToDelete)
                                        navController.popBackStack()
                                    }
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("ringing") {
                            val payload = pendingRinging ?: RingingPayload(
                                alarmId = -1,
                                hour = 7,
                                minute = 0,
                                label = "",
                                difficulty = MathDifficulty.EASY
                            )

                            AlarmRingingScreen(
                                alarmId = payload.alarmId,
                                hour = payload.hour,
                                minute = payload.minute,
                                label = payload.label,
                                difficulty = payload.difficulty,
                                onStopAlarmService = {
                                    stopAlarmService()
                                    if (payload.alarmId > 0 && payload.alarmId != 9999) {
                                        scope.launch {
                                            alarmDao.getAlarmById(payload.alarmId)?.let { alarm ->
                                                if (alarm.repeatDays.isBlank()) {
                                                    alarmDao.updateAlarm(alarm.copy(isActive = false))
                                                }
                                            }
                                        }
                                    }
                                },
                                onAlarmDismissed = {
                                    stopAlarmService()
                                    pendingRinging = null
                                    navController.navigate("list") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val payload = extractRingingPayload(intent)
        if (payload != null) {
            pendingRinging = payload
        }
    }

    private fun needsFullScreenPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            return !notificationManager.canUseFullScreenIntent()
        }
        return false
    }

    private fun stopAlarmService() {
        startService(
            Intent(this, AlarmService::class.java).apply {
                action = AlarmService.ACTION_STOP
            }
        )
    }

    private fun extractRingingPayload(intent: Intent?): RingingPayload? {
        if (intent?.getBooleanExtra("IS_RINGING", false) != true) return null
        val diffName = intent.getStringExtra(AlarmScheduler.EXTRA_DIFFICULTY) ?: "EASY"
        val diff = runCatching { MathDifficulty.valueOf(diffName) }.getOrDefault(MathDifficulty.EASY)
        return RingingPayload(
            alarmId = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1),
            hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 7),
            minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0),
            label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL) ?: "",
            difficulty = diff
        )
    }
}