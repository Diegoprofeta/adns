package com.eyalm.adns.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eyalm.adns.data.nextdns.analytics.NamedSeries

@Composable
fun rememberMaterialSeriesColors(): List<Color> {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme) {
        listOf(
            scheme.primary,
            scheme.tertiary,
            scheme.secondary,
            scheme.error,
            scheme.outline,
            scheme.inversePrimary,
        )
    }
}

@Composable
fun WavyLineChart(
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 5.dp,
    maxY: Float? = null,
    showGrid: Boolean = false,
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val maxVal = maxY ?: points.maxOrNull() ?: 1f
        val minVal = if (maxY != null) 0f else (points.minOrNull() ?: 0f)
        val range = (maxVal - minVal).coerceAtLeast(1f)

        if (showGrid) {
            val gridColor = lineColor.copy(alpha = 0.12f)
            val gridStroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            for (step in 1..3) {
                val y = height * (step / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = gridStroke.width,
                    pathEffect = gridStroke.pathEffect
                )
            }
        }

        val path = Path()
        val stepX = width / (points.size - 1)

        val coords = points.mapIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value - minVal) / range) * height
            Offset(x, y)
        }

        path.moveTo(coords[0].x, coords[0].y)
        for (i in 0 until coords.size - 1) {
            val p0 = coords[i]
            val p1 = coords[i + 1]
            val controlPointX = (p0.x + p1.x) / 2
            path.cubicTo(controlPointX, p0.y, controlPointX, p1.y, p1.x, p1.y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

@Composable
fun MultiWavyLineChart(
    seriesList: List<NamedSeries>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 5.dp,
    selectedSeriesName: String? = null,
    showGrid: Boolean = false,
) {
    Canvas(modifier = modifier) {
        val validSeriesWithColors = seriesList.mapIndexedNotNull { index, series ->
            if (series.points.size >= 2) {
                val color = colors.getOrElse(index) { colors.firstOrNull() ?: Color.Gray }
                series to color
            } else null
        }
        if (validSeriesWithColors.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height

        val maxVal = validSeriesWithColors.flatMap { it.first.points }.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val minVal = 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        if (showGrid) {
            val gridColor = colors.firstOrNull()?.copy(alpha = 0.12f) ?: Color.Gray.copy(alpha = 0.12f)
            val gridStroke = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            for (step in 1..3) {
                val y = height * (step / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = gridStroke.width,
                    pathEffect = gridStroke.pathEffect
                )
            }
        }

        validSeriesWithColors.forEach { (series, color) ->
            val isSelected = selectedSeriesName == null || selectedSeriesName == series.name
            val alpha = if (isSelected) 1f else 0.18f
            val actualStroke = if (selectedSeriesName == series.name) strokeWidth * 1.2f else strokeWidth

            val stepX = width / (series.points.size - 1)
            val coords = series.points.mapIndexed { pIdx, value ->
                val x = pIdx * stepX
                val y = height - ((value - minVal) / range) * height
                Offset(x, y)
            }

            val path = Path()
            path.moveTo(coords[0].x, coords[0].y)
            for (i in 0 until coords.size - 1) {
                val p0 = coords[i]
                val p1 = coords[i + 1]
                val controlPointX = (p0.x + p1.x) / 2
                path.cubicTo(controlPointX, p0.y, controlPointX, p1.y, p1.x, p1.y)
            }

            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(
                    width = actualStroke.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

