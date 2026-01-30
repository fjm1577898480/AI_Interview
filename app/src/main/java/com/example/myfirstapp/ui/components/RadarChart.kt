package com.example.myfirstapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EnhancedRadarChart(data: List<Float>, modifier: Modifier = Modifier) {
    val backgroundPath = remember { Path() }
    val dataPath = remember { Path() }

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2
        val angleStep = (2 * Math.PI / 5).toFloat()

        for (level in 1..3) {
            val levelRadius = radius * (level / 3f)
            backgroundPath.reset()
            for (i in 0 until 5) {
                val angle = i * angleStep - Math.PI.toFloat() / 2
                val x = center.x + levelRadius * cos(angle)
                val y = center.y + levelRadius * sin(angle)
                if (i == 0) backgroundPath.moveTo(x, y) else backgroundPath.lineTo(x, y)
            }
            backgroundPath.close()
            drawPath(backgroundPath, color = Color.LightGray.copy(alpha = 0.3f), style = Stroke(width = 1.dp.toPx()))
        }

        if (data.any { it > 0f }) {
            dataPath.reset()
            for (i in 0 until 5) {
                val angle = i * angleStep - Math.PI.toFloat() / 2
                val currentRadius = radius * data.getOrElse(i) { 0f }
                val x = center.x + currentRadius * cos(angle)
                val y = center.y + currentRadius * sin(angle)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                drawCircle(color = Color(0xFF00C091), radius = 3.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
            }
            dataPath.close()
            drawPath(dataPath, color = Color(0x4400C091))
            drawPath(dataPath, color = Color(0xFF00C091), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
fun RadarLabels(labels: List<String>) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(labels[0], fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopCenter))
        Text(labels[1], fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp, bottom = 60.dp))
        Text(labels[2], fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(start = 30.dp, bottom = 20.dp))
        Text(labels[3], fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 20.dp))
        Text(labels[4], fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp, bottom = 60.dp))
    }
}