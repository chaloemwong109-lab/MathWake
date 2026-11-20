package com.example.mathwake.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.mathwake.R

@Composable
fun AddEditAlarmScreen(
    alarmId: Int?,
    onNavigateBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_desc)) },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    // TODO: Delete from DB
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.delete_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(if (alarmId == null) R.string.add_alarm_title else R.string.edit_alarm_title))
            
            Button(onClick = onNavigateBack) {
                Text(stringResource(R.string.save))
            }

            if (alarmId != null) {
                Button(onClick = { showDeleteDialog = true }) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}