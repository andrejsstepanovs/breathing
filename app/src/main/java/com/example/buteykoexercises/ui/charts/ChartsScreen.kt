package com.example.buteykoexercises.ui.charts

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun ChartsScreen(
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val data by viewModel.dataPoints.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "CP Progress",
            style = MaterialTheme.typography.headlineMedium
        )
        
        if (data.size < 2) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Need at least 2 records to show chart", color = Color.Gray)
            }
        } else {
            // Sort by time ascending for the line chart
            val sortedData = remember(data) { data.sortedBy { it.timestamp } }
            
            LineChart(
                data = sortedData,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Fill remaining space
                    .padding(top = 16.dp, bottom = 16.dp)
            )
        }
    }
}

@Composable
fun LineChart(
    data: List<com.example.buteykoexercises.data.local.entity.ControlPauseEntity>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        // 1. Calculate Bounds
        val minTime = data.first().timestamp
        val maxTime = data.last().timestamp
        val timeRange = max(maxTime - minTime, 1L).toFloat()

        val maxCp = data.maxOf { it.durationSeconds }
        // Add 10% headroom to Y axis
        val yMax = maxCp * 1.1f 

        // 2. Define drawing area (leaving padding for labels)
        val paddingLeft = 100f // Space for Y labels
        val paddingBottom = 80f // Space for X labels
        val width = size.width - paddingLeft
        val height = size.height - paddingBottom

        // 3. Prepare Paint for Text
        val textPaint = Paint().apply {
            color = onSurface.toArgb()
            textSize = 30f
            textAlign = Paint.Align.RIGHT
        }

        // 4. Draw Axes
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, 0f),
            end = Offset(paddingLeft, height),
            strokeWidth = 2f
        )
        drawLine(
            color = Color.Gray,
            start = Offset(paddingLeft, height),
            end = Offset(size.width, height),
            strokeWidth = 2f
        )

        // 5. Draw Y Labels (0, Max/2, Max)
        val ySteps = 5
        for (i in 0..ySteps) {
            val value = (yMax / ySteps) * i
            val yPos = height - (height * (value / yMax))
            
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.0f", value),
                paddingLeft - 10f,
                yPos + 10f, // vertical center adjustment
                textPaint
            )
            // Optional: Grid lines
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(paddingLeft, yPos),
                end = Offset(size.width, yPos)
            )
        }

        // 6. Draw Line Path
        val path = Path()
        data.forEachIndexed { index, record ->
            val normalizedX = (record.timestamp - minTime).toFloat() / timeRange
            val normalizedY = record.durationSeconds / yMax

            val xPos = paddingLeft + (normalizedX * width)
            val yPos = height - (normalizedY * height)

            if (index == 0) {
                path.moveTo(xPos, yPos)
            } else {
                path.lineTo(xPos, yPos)
            }
            
            // Draw Dots
            drawCircle(
                color = primaryColor,
                radius = 8f,
                center = Offset(xPos, yPos)
            )
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 5f)
        )
        
        // 7. Draw X Labels (Min date and Max date)
        val datePaint = Paint().apply {
            color = onSurface.toArgb()
            textSize = 30f
            textAlign = Paint.Align.CENTER
        }
        val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
        
        // Start Label
        drawContext.canvas.nativeCanvas.drawText(
            dateFormat.format(Date(minTime)),
            paddingLeft,
            height + 50f,
            datePaint
        )
        
        // End Label
        drawContext.canvas.nativeCanvas.drawText(
            dateFormat.format(Date(maxTime)),
            size.width,
            height + 50f,
            datePaint
        )
    }
}
