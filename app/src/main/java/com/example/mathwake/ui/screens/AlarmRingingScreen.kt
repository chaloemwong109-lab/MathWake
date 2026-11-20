package com.example.mathwake.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.mathwake.R
import com.example.mathwake.data.MathDifficulty
import com.example.mathwake.logic.MathProblem
import com.example.mathwake.logic.MathProblemGenerator

@Composable
fun AlarmRingingScreen(
    alarmId: Int,
    difficulty: MathDifficulty,
    onAlarmDismissed: () -> Unit
) {
    // Prevent accidental back press during alarm ringing
    BackHandler {
        // Do nothing, force user to solve math
    }

    var problem by remember { mutableStateOf<MathProblem?>(null) }
    // rememberSaveable to keep state when rotating or switching apps
    var currentAnswer by rememberSaveable { mutableStateOf("") }
    var isError by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(difficulty) {
        if (problem == null) {
            problem = MathProblemGenerator.generateProblem(difficulty)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.wake_up_message))
            
            problem?.let { p ->
                Text(p.question)
                // In a real app, this would be a custom Numpad. 
                // For simplicity in structure, just a button to simulate correct answer
                Text("Current input: $currentAnswer")

                if (isError) {
                    Text(stringResource(R.string.incorrect_answer), color = androidx.compose.ui.graphics.Color.Red)
                }

                Button(onClick = { 
                    if (currentAnswer == p.answer.toString()) {
                        onAlarmDismissed()
                    } else {
                        // Simulate error
                        isError = true
                    }
                }) {
                    Text(stringResource(R.string.submit_answer))
                }
                
                // Cheat button for testing
                Button(onClick = { currentAnswer = p.answer.toString() }) {
                    Text("Cheat (Set correct answer)")
                }
            }
        }
    }
}