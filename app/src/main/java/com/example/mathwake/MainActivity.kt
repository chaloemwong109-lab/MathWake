package com.example.mathwake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mathwake.data.MathDifficulty
import com.example.mathwake.ui.screens.AddEditAlarmScreen
import com.example.mathwake.ui.screens.AlarmListScreen
import com.example.mathwake.ui.screens.AlarmRingingScreen
import com.example.mathwake.ui.theme.MathWakeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val isRinging = intent.getBooleanExtra("IS_RINGING", false)
        val ringingAlarmId = intent.getIntExtra("ALARM_ID", -1)
        val ringingDifficulty = intent.getStringExtra("DIFFICULTY") ?: "EASY"
        
        setContent {
            MathWakeTheme {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController, 
                        startDestination = if (isRinging) "ringing" else "list"
                    ) {
                        composable("list") {
                            AlarmListScreen(
                                onNavigateToAdd = { navController.navigate("add_edit/-1") },
                                onNavigateToEdit = { id -> navController.navigate("add_edit/$id") }
                            )
                        }
                        
                        composable(
                            "add_edit/{alarmId}",
                            arguments = listOf(navArgument("alarmId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val alarmId = backStackEntry.arguments?.getInt("alarmId")
                            AddEditAlarmScreen(
                                alarmId = if (alarmId == -1) null else alarmId,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        
                        composable("ringing") {
                            AlarmRingingScreen(
                                alarmId = ringingAlarmId,
                                difficulty = MathDifficulty.valueOf(ringingDifficulty),
                                onAlarmDismissed = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }
}