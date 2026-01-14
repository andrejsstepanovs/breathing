package com.example.buteykoexercises.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Listen for delete completion
    LaunchedEffect(true) {
        viewModel.deletedEvent.collect {
            onBack()
        }
    }

    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d, yyyy • HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Record Details",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
            when (val s = state) {
                is DetailUiState.Loading -> CircularProgressIndicator()
                is DetailUiState.Error -> Text("Record not found.")
                
                is DetailUiState.CpDetail -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Control Pause", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${String.format("%.1f", s.record.durationSeconds)} s",
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(dateFormat.format(Date(s.record.timestamp)))
                    }
                }

                is DetailUiState.SessionDetail -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Breathing Session",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = dateFormat.format(Date(s.session.session.timestamp)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text("Loops:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        s.session.loops.forEachIndexed { index, loop ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("#${index + 1}")
                                    Text("Start CP: ${String.format("%.1f", loop.initialCp)}")
                                    val min = loop.breathingDurationSeconds / 60
                                    val sec = loop.breathingDurationSeconds % 60
                                    Text("Breath: ${String.format("%d:%02d", min, sec)}")
                                    Text("End CP: ${String.format("%.1f", loop.finalCp)}")
                                }
                            }
                        }
                        
                        if (!s.session.session.note.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Notes:", fontWeight = FontWeight.Bold)
                            Text(
                                text = s.session.session.note,
                                style = MaterialTheme.typography.bodyLarge,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.deleteRecord() },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("DELETE RECORD")
        }
    }
}
