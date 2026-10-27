package com.lumify.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.lumify.editor.core.MaskType
import com.lumify.editor.core.SelectiveMask
import com.lumify.editor.ui.theme.AccentCyan
import kotlin.math.sqrt

/**
 * Visual interactive overlay for Selective Masking (Linear & Radial Gradients).
 * Displays gradient direction guides, feather boundaries, and draggable handles.
 */
@Composable
fun MaskOverlay(
    mask: SelectiveMask,
    onMaskChange: (SelectiveMask) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedHandle by remember { mutableStateOf<String?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(mask) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val nx = pos.x / w
                        val ny = pos.y / h

                        val touchDist = 0.12f

                        when (mask.type) {
                            MaskType.LINEAR_GRADIENT -> {
                                val dStart = sqrt((nx - mask.startX).let { it * it } + (ny - mask.startY).let { it * it })
                                val dEnd = sqrt((nx - mask.endX).let { it * it } + (ny - mask.endY).let { it * it })

                                draggedHandle = when {
                                    dStart < touchDist && dStart <= dEnd -> "start"
                                    dEnd < touchDist -> "end"
                                    else -> null
                                }
                            }
                            MaskType.RADIAL_GRADIENT -> {
                                val dCenter = sqrt((nx - mask.centerX).let { it * it } + (ny - mask.centerY).let { it * it })
                                val edgeX = mask.centerX + mask.radiusX
                                val dEdge = sqrt((nx - edgeX).let { it * it } + (ny - mask.centerY).let { it * it })

                                draggedHandle = when {
                                    dCenter < touchDist -> "center"
                                    dEdge < touchDist -> "radius"
                                    else -> null
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        val handle = draggedHandle ?: return@detectDragGestures
                        change.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val dx = dragAmount.x / w
                        val dy = dragAmount.y / h

                        when (mask.type) {
                            MaskType.LINEAR_GRADIENT -> {
                                if (handle == "start") {
                                    onMaskChange(
                                        mask.copy(
                                            startX = (mask.startX + dx).coerceIn(0f, 1f),
                                            startY = (mask.startY + dy).coerceIn(0f, 1f)
                                        )
                                    )
                                } else if (handle == "end") {
                                    onMaskChange(
                                        mask.copy(
                                            endX = (mask.endX + dx).coerceIn(0f, 1f),
                                            endY = (mask.endY + dy).coerceIn(0f, 1f)
                                        )
                                    )
                                }
                            }
                            MaskType.RADIAL_GRADIENT -> {
                                if (handle == "center") {
                                    onMaskChange(
                                        mask.copy(
                                            centerX = (mask.centerX + dx).coerceIn(0f, 1f),
                                            centerY = (mask.centerY + dy).coerceIn(0f, 1f)
                                        )
                                    )
                                } else if (handle == "radius") {
                                    onMaskChange(
                                        mask.copy(
                                            radiusX = (mask.radiusX + dx).coerceIn(0.05f, 0.8f),
                                            radiusY = (mask.radiusY + dy).coerceIn(0.05f, 0.8f)
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onDragEnd = { draggedHandle = null },
                    onDragCancel = { draggedHandle = null }
                )
            }
    ) {
        val w = size.width
        val h = size.height

        when (mask.type) {
            MaskType.LINEAR_GRADIENT -> {
                val p1 = Offset(mask.startX * w, mask.startY * h)
                val p2 = Offset(mask.endX * w, mask.endY * h)

                // Main direction vector line
                drawLine(
                    color = AccentCyan,
                    start = p1,
                    end = p2,
                    strokeWidth = 2.dp.toPx()
                )

                // Perpendicular feather guides
                val dir = p2 - p1
                val perp = Offset(-dir.y, dir.x)
                val perpLen = sqrt(perp.x * perp.x + perp.y * perp.y)
                if (perpLen > 0.001f) {
                    val normPerp = perp / perpLen * (w * 0.4f)
                    val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

                    // Start line (full effect)
                    drawLine(
                        color = AccentCyan.copy(alpha = 0.8f),
                        start = p1 - normPerp,
                        end = p1 + normPerp,
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // End line (feather boundary)
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f),
                        start = p2 - normPerp,
                        end = p2 + normPerp,
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dashed
                    )
                }

                // Drag handles
                drawCircle(Color.Black, radius = 9.dp.toPx(), center = p1)
                drawCircle(AccentCyan, radius = 7.dp.toPx(), center = p1)

                drawCircle(Color.Black, radius = 9.dp.toPx(), center = p2)
                drawCircle(Color.White, radius = 7.dp.toPx(), center = p2)
            }
            MaskType.RADIAL_GRADIENT -> {
                val center = Offset(mask.centerX * w, mask.centerY * h)
                val rx = mask.radiusX * w
                val ry = mask.radiusY * h

                // Outer feather ellipse
                drawOval(
                    color = AccentCyan.copy(alpha = 0.8f),
                    topLeft = Offset(center.x - rx, center.y - ry),
                    size = Size(rx * 2, ry * 2),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Inner core ellipse (50% feather)
                drawOval(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(center.x - rx * 0.5f, center.y - ry * 0.5f),
                    size = Size(rx, ry),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                )

                // Center handle
                drawCircle(Color.Black, radius = 9.dp.toPx(), center = center)
                drawCircle(AccentCyan, radius = 7.dp.toPx(), center = center)

                // Radial radius handle
                val edgePoint = Offset(center.x + rx, center.y)
                drawCircle(Color.Black, radius = 8.dp.toPx(), center = edgePoint)
                drawCircle(Color.White, radius = 6.dp.toPx(), center = edgePoint)
            }
        }
    }
}
