package com.example.buteykoexercises.ui.exercise

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ExerciseScreen(
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // --- NEW: Keep Screen On Logic ---
    // Active only when in Reduced Breathing (or you can enable for all steps !is Idle)
    if (state.step is ExerciseStep.Breathing) {
        KeepScreenOn()
    }
    // ---------------------------------

    // Container for all states
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // Global Round Indicator
        if (state.step !is ExerciseStep.Idle) {
            val roundNum = if (state.step is ExerciseStep.Summary) {
                state.completedLoops.size
            } else {
                state.completedLoops.size + 1
            }

            Text(
                text = "ROUND $roundNum",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Dynamic Content based on State Machine
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            when (state.step) {
                is ExerciseStep.Idle -> IdleView(onStart = { viewModel.startSession() })

                is ExerciseStep.PreCheckCp -> CpCheckView(
                    title = "Pre-Check CP",
                    seconds = state.cpTimerSeconds,
                    isRunning = state.isTimerRunning,
                    lastKnownCp = state.lastKnownCp,
                    onToggle = { viewModel.toggleCpTimer() },
                    onUseLast = { viewModel.useLastCpForPreCheck() },
                    onAbandon = { viewModel.abandonCurrentLoop() },
                    isFirstLoop = state.completedLoops.isEmpty()
                )

                is ExerciseStep.Breathing -> BreathingView(
                    seconds = state.breathingTimerSeconds,
                    onPause = { viewModel.pauseBreathing() }
                )

                is ExerciseStep.Paused -> PausedView(
                    seconds = state.breathingTimerSeconds,
                    onResume = { viewModel.resumeBreathing() },
                    onFinish = { viewModel.finishBreathing() },
                    onAbandon = { viewModel.abandonCurrentLoop() }
                )

                is ExerciseStep.RecoveryCountdown -> RecoveryView(
                    remaining = state.recoveryTimerSeconds,
                    completedLoops = state.completedLoops,
                    currentInitialCp = state.initialCp,
                    currentBreatheTime = state.breathingTimerSeconds,
                    onSkip = { viewModel.skipRecovery() }
                )

                is ExerciseStep.PostCheckCp -> CpCheckView(
                    title = "Post-Check CP",
                    seconds = state.cpTimerSeconds,
                    isRunning = state.isTimerRunning,
                    // We typically don't want to skip/use last for the *final* check
                    // of a loop, so we pass null.
                    lastKnownCp = null,
                    onToggle = { viewModel.toggleCpTimer() },
                    onUseLast = { },
                    // Pass an empty lambda or specific logic.
                    // Usually you can't abandon a loop *after* you've already finished it
                    // (which PostCheck implies), but we must satisfy the signature.
                    // Or, we can allow cancelling the *recording* of this final CP.
                    onAbandon = { viewModel.abandonCurrentLoop() },
                    isFirstLoop = false // Post-check implies we are deep in a loop
                )

                is ExerciseStep.Summary -> SummaryView(
                    initialCp = state.initialCp,
                    finalCp = state.finalCp,
                    breatheTime = state.breathingTimerSeconds,
                    onNextLoop = { viewModel.startNextLoop() },
                    onFinishSession = { comment -> viewModel.finishSession(comment) }
                )
            }
        }
    }
}

// --- HELPER: Screen Wake Lock ---
@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun IdleView(onStart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Ready to Breathe?",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("START SESSION")
        }
    }
}

@Composable
fun CpCheckView(
    title: String,
    seconds: Float,
    isRunning: Boolean,
    lastKnownCp: Float?,
    onToggle: () -> Unit,
    onUseLast: () -> Unit,
    onAbandon: () -> Unit,
    isFirstLoop: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = String.format("%.1f", seconds),
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
        )
        Text("seconds", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) Color.Red else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(120.dp).clip(CircleShape)
        ) {
            Text(if (isRunning) "STOP" else "START")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Actions when NOT running
        if (!isRunning) {
            // Skip Option
            if (lastKnownCp != null) {
                OutlinedButton(
                    onClick = onUseLast,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("SKIP & USE LAST (${String.format("%.1f", lastKnownCp)} s)")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Cancel / Abandon Option
            TextButton(
                onClick = onAbandon,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isFirstLoop) "CANCEL SESSION" else "CANCEL ROUND & FINISH",
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun BreathingView(seconds: Long, onPause: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Reduced Breathing", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(48.dp))

        val min = seconds / 60
        val sec = seconds % 60
        Text(
            text = String.format("%02d:%02d", min, sec),
            fontSize = 80.sp,
            fontWeight = FontWeight.Light
        )

        Spacer(modifier = Modifier.height(64.dp))

        Button(
            onClick = onPause,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("STOP / PAUSE")
        }
    }
}

@Composable
fun PausedView(
    seconds: Long,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onAbandon: () -> Unit
) {
    val min = seconds / 60
    val sec = seconds % 60

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Paused", style = MaterialTheme.typography.headlineMedium, color = Color.Gray)
        Text(
            text = String.format("%02d:%02d", min, sec),
            fontSize = 40.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onResume,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("RESUME")
            }

            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("NEXT STEP")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Abandon Button
        TextButton(
            onClick = onAbandon,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("ABANDON ROUND")
        }
    }
}

@Composable
fun RecoveryView(
    remaining: Int,
    completedLoops: List<CompletedLoop>,
    currentInitialCp: Float,
    currentBreatheTime: Long,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text("Recovery", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Breathe normally...", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "$remaining",
            fontSize = 100.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = onSkip) {
            Text("SKIP TIMER")
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider()
        Text(
            text = "Session Progress",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 250.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(completedLoops) { index, loop ->
                LoopStatRow(
                    label = "Loop ${index + 1}",
                    initial = loop.initialCp,
                    breathe = loop.breathingSeconds,
                    final = loop.finalCp,
                    isCurrent = false
                )
            }

            item {
                LoopStatRow(
                    label = "Loop ${completedLoops.size + 1} (Now)",
                    initial = currentInitialCp,
                    breathe = currentBreatheTime,
                    final = null,
                    isCurrent = true
                )
            }
        }
    }
}

@Composable
fun LoopStatRow(
    label: String,
    initial: Float,
    breathe: Long,
    final: Float?,
    isCurrent: Boolean
) {
    val min = breathe / 60
    val sec = breathe % 60

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(String.format("%.1f", initial), fontSize = 14.sp)
                Text(" → ", color = Color.Gray)
                Text(String.format("%02d:%02d", min, sec), fontSize = 14.sp)

                Text(" → ", color = Color.Gray)
                if (final != null) {
                    Text(String.format("%.1f", final), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("?", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SummaryView(
    initialCp: Float,
    finalCp: Float,
    breatheTime: Long,
    onNextLoop: () -> Unit,
    onFinishSession: (String) -> Unit
) {
    var comment by remember { mutableStateOf("") }
    val min = breatheTime / 60
    val sec = breatheTime % 60

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Loop Complete!", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Initial CP:")
                    Text(String.format("%.1f s", initialCp), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Breathing:")
                    Text(String.format("%02d:%02d", min, sec), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Final CP:")
                    Text(String.format("%.1f s", finalCp), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNextLoop,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ANOTHER ROUND", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Current round saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("— OR —", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Session Notes (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { onFinishSession(comment) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("FINISH SESSION")
        }
    }
}