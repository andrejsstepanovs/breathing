package com.example.buteykoexercises.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    // --- ADDED THIS PARAMETER ---
    onItemClick: (String, Long) -> Unit
) {
    val items by viewModel.historyItems.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No records yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(items) { item ->
                    // --- WRAP ITEMS IN SURFACE FOR CLICK ---
                    Surface(
                        onClick = {
                            val type = if (item is HistoryItem.StandaloneCp) "CP" else "SESSION"
                            onItemClick(type, item.id)
                        },
                        // We use Surface to handle the ripple effect and click event cleanly
                        color = Color.Transparent
                    ) {
                        when (item) {
                            is HistoryItem.StandaloneCp -> StandaloneCpItem(item)
                            is HistoryItem.Session -> SessionItem(item)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun StandaloneCpItem(item: HistoryItem.StandaloneCp) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Control Pause",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = DateFormatter.format(item.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = String.format("%.1f s", item.duration),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SessionItem(item: HistoryItem.Session) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Date and Loop Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Breathing Session",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = DateFormatter.format(item.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Badge(containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                    Text(
                        text = "${item.loops.size} Loop(s)",
                        modifier = Modifier.padding(4.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

            // Loops List - Improved Layout
            item.loops.forEachIndexed { index, loop ->
                val min = loop.breatheTime / 60
                val sec = loop.breatheTime % 60

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    // Line 1: Loop Number + Breathing Time (Highlight)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Loop ${index + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%d:%02d", min, sec),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Line 2: CP Progress (Start -> End)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start CP
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start CP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = String.format("%.1f s", loop.initialCp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        // Arrow visual
                        Text(
                            text = "→",
                            modifier = Modifier.padding(horizontal = 8.dp),
                            color = Color.Gray
                        )

                        // End CP
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End CP",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Text(
                                text = String.format("%.1f s", loop.finalCp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold // Highlight final result
                            )
                        }
                    }
                }

                // Divider between loops (except last one)
                if (index < item.loops.size - 1) {
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                }
            }

            // Note footer
            if (!item.note.isNullOrBlank()) {
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Note: ${item.note}",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}