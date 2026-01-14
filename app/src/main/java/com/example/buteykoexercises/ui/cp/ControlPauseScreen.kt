package com.example.buteykoexercises.ui.cp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.remember

@Composable
fun ControlPauseScreen(
    viewModel: ControlPauseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Control Pause",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Timer Display
        Text(
            text = String.format("%.1f", state.elapsedSeconds),
            fontSize = 96.sp,
            fontWeight = FontWeight.Bold,
            color = if (state.isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "seconds",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(64.dp))

        // Main Button
        Button(
            onClick = { viewModel.toggleTimer() },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.isRunning) Color.Red else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (state.isRunning) "STOP" else "START",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        if (!state.isRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // CHANGED: Only show RESET if time is > 0.0
                if (state.elapsedSeconds > 0f) {
                    OutlinedButton(onClick = { viewModel.reset() }) {
                        Text("RESET")
                    }
                }

                // Show Delete ONLY if we just finished a record
                if (state.canDeleteLast) {
                    OutlinedButton(
                        onClick = { viewModel.deleteLastRecord() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Red
                        )
                    ) {
                        Text("DELETE")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // --- STATS FOOTER ---

        Column(modifier = Modifier.fillMaxWidth()) {
            val dateFormat = remember {
                SimpleDateFormat("yy-MM-dd HH:mm", Locale.getDefault())
            }

            // Best Result
            state.bestRecord?.let { best ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Best:",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dateFormat.format(Date(best.timestamp)),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("${String.format("%.1f", best.durationSeconds)} s", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Recent:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)

            // Last 3 List
            if (state.recentRecords.isEmpty()) {
                Text("No records yet", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                state.recentRecords.forEach { record ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateFormat.format(Date(record.timestamp)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${String.format("%.1f", record.durationSeconds)} s",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}