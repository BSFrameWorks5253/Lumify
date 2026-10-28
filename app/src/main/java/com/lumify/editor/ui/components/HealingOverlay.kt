package com.lumify.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.HealingPoint
import com.lumify.editor.core.HealingStroke
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary

@Composable
fun HealingOverlay(
    strokes: List<HealingStroke>,
    onAddStroke: (HealingStroke) -> Unit,
    onUndoStroke: () -> Unit,
    onClearStrokes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPoints = remember { mutableStateListOf<HealingPoint>() }
    var brushRadius by remember { mutableFloatStateOf(0.04f) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Interactive Drawing Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(brushRadius) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            currentPoints.clear()
                            currentPoints.add(HealingPoint(startOffset.x / w, startOffset.y / h))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            val pt = HealingPoint(
                                (change.position.x / w).coerceIn(0f, 1f),
                                (change.position.y / h).coerceIn(0f, 1f)
                            )
                            currentPoints.add(pt)
                        },
                        onDragEnd = {
                            if (currentPoints.isNotEmpty()) {
                                onAddStroke(
                                    HealingStroke(
                                        points = currentPoints.toList(),
                                        radius = brushRadius
                                    )
                                )
                                currentPoints.clear()
                            }
                        },
                        onDragCancel = {
                            currentPoints.clear()
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val strokeWidthPx = brushRadius * 2f * w

            // Draw current active stroke
            if (currentPoints.size > 1) {
                for (i in 0 until currentPoints.lastIndex) {
                    val p1 = currentPoints[i]
                    val p2 = currentPoints[i + 1]
                    drawLine(
                        color = Color(0xFFFF3366).copy(alpha = 0.65f),
                        start = Offset(p1.x * w, p1.y * h),
                        end = Offset(p2.x * w, p2.y * h),
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            } else if (currentPoints.size == 1) {
                val p = currentPoints.first()
                drawCircle(
                    color = Color(0xFFFF3366).copy(alpha = 0.65f),
                    radius = strokeWidthPx / 2f,
                    center = Offset(p.x * w, p.y * h)
                )
            }
        }

        // 2. Bottom Brush Controls Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 85.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LiquidGlassSurface(
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "BRUSH",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(
                            value = brushRadius,
                            onValueChange = { brushRadius = it },
                            valueRange = 0.015f..0.09f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentCyan,
                                activeTrackColor = AccentCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Undo last stroke
                        IconButton(
                            onClick = onUndoStroke,
                            enabled = strokes.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Healing Stroke",
                                tint = if (strokes.isNotEmpty()) AccentCyan else TextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Clear all strokes
                        IconButton(
                            onClick = onClearStrokes,
                            enabled = strokes.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Clear All Healing Strokes",
                                tint = if (strokes.isNotEmpty()) TextPrimary else TextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
