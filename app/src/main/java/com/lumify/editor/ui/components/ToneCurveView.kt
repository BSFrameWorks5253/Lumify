package com.lumify.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.ImageProcessor
import com.lumify.editor.core.ToneCurvePoint
import com.lumify.editor.core.ToneCurves
import com.lumify.editor.core.defaultCurvePoints
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.HslBlue
import com.lumify.editor.ui.theme.HslGreen
import com.lumify.editor.ui.theme.HslRed
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary
import kotlin.math.sqrt

enum class CurveChannel(val label: String, val color: Color) {
    RGB("RGB", Color.White),
    RED("Red", HslRed),
    GREEN("Green", HslGreen),
    BLUE("Blue", HslBlue)
}

@Composable
fun ToneCurveView(
    curves: ToneCurves,
    onCurvesChange: (ToneCurves) -> Unit,
    modifier: Modifier = Modifier,
    canvasDimension: androidx.compose.ui.unit.Dp = 175.dp
) {
    var selectedChannel by remember { mutableStateOf(CurveChannel.RGB) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var activePointIdx by remember { mutableIntStateOf(-1) }

    val activePoints = when (selectedChannel) {
        CurveChannel.RGB -> curves.rgbPoints
        CurveChannel.RED -> curves.redPoints
        CurveChannel.GREEN -> curves.greenPoints
        CurveChannel.BLUE -> curves.bluePoints
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Channel Selector Ribbon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurveChannel.values().forEach { channel ->
                val isSelected = channel == selectedChannel
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) channel.color.copy(alpha = 0.2f) else DarkSurfaceElevated)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) channel.color else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedChannel = channel }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = channel.label,
                        color = if (isSelected) channel.color else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Tone Curve Canvas
        Box(
            modifier = Modifier
                .size(canvasDimension)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .onSizeChanged { canvasSize = it }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedChannel, activePoints) {
                        detectTapGestures(
                            onDoubleTap = {
                                // Double tap resets active channel to linear
                                val resetList = defaultCurvePoints()
                                val updated = when (selectedChannel) {
                                    CurveChannel.RGB -> curves.copy(rgbPoints = resetList)
                                    CurveChannel.RED -> curves.copy(redPoints = resetList)
                                    CurveChannel.GREEN -> curves.copy(greenPoints = resetList)
                                    CurveChannel.BLUE -> curves.copy(bluePoints = resetList)
                                }
                                onCurvesChange(updated)
                            }
                        )
                    }
                    .pointerInput(canvasSize, selectedChannel, activePoints) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val w = canvasSize.width.toFloat()
                                val h = canvasSize.height.toFloat()
                                val normX = offset.x / w
                                val normY = 1f - (offset.y / h)

                                // Find closest point within touch radius
                                var closestIdx = -1
                                var minDist = 0.18f
                                for (i in activePoints.indices) {
                                    val pt = activePoints[i]
                                    val d = sqrt((pt.x - normX).let { it * it } + (pt.y - normY).let { it * it })
                                    if (d < minDist) {
                                        minDist = d
                                        closestIdx = i
                                    }
                                }
                                activePointIdx = closestIdx
                            },
                            onDrag = { change, _ ->
                                if (activePointIdx in activePoints.indices) {
                                    change.consume()
                                    val w = canvasSize.width.toFloat()
                                    val h = canvasSize.height.toFloat()
                                    val newX = (change.position.x / w).coerceIn(0f, 1f)
                                    val newY = (1f - (change.position.y / h)).coerceIn(0f, 1f)

                                    val mutablePoints = activePoints.toMutableList()
                                    // Lock X for endpoints, free Y
                                    val clampedX = when (activePointIdx) {
                                        0 -> 0f
                                        activePoints.lastIndex -> 1f
                                        else -> newX
                                    }
                                    mutablePoints[activePointIdx] = ToneCurvePoint(clampedX, newY)

                                    val updatedCurves = when (selectedChannel) {
                                        CurveChannel.RGB -> curves.copy(rgbPoints = mutablePoints)
                                        CurveChannel.RED -> curves.copy(redPoints = mutablePoints)
                                        CurveChannel.GREEN -> curves.copy(greenPoints = mutablePoints)
                                        CurveChannel.BLUE -> curves.copy(bluePoints = mutablePoints)
                                    }
                                    onCurvesChange(updatedCurves)
                                }
                            },
                            onDragEnd = { activePointIdx = -1 },
                            onDragCancel = { activePointIdx = -1 }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height

                // Draw Grid (25%, 50%, 75%)
                val gridColor = Color.White.copy(alpha = 0.12f)
                val gridStroke = Stroke(1.dp.toPx())
                for (ratio in listOf(0.25f, 0.5f, 0.75f)) {
                    drawLine(gridColor, Offset(w * ratio, 0f), Offset(w * ratio, h), strokeWidth = 1.dp.toPx())
                    drawLine(gridColor, Offset(0f, h * ratio), Offset(w, h * ratio), strokeWidth = 1.dp.toPx())
                }

                // Diagonal Linear Reference Line
                drawLine(
                    Color.White.copy(alpha = 0.2f),
                    Offset(0f, h),
                    Offset(w, 0f),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw Spline Curve through LUT samples
                val lut = ImageProcessor.buildCurveLut(activePoints)
                val curvePath = Path()
                curvePath.moveTo(0f, h - (lut[0] / 255f * h))

                val step = w / 255f
                for (i in 1..255) {
                    val px = i * step
                    val py = h - (lut[i] / 255f * h)
                    curvePath.lineTo(px, py)
                }

                drawPath(
                    path = curvePath,
                    color = selectedChannel.color,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Draw Control Points
                for (i in activePoints.indices) {
                    val pt = activePoints[i]
                    val px = pt.x * w
                    val py = h - (pt.y * h)
                    val isSelectedPoint = i == activePointIdx

                    drawCircle(
                        color = Color.Black,
                        radius = if (isSelectedPoint) 8.dp.toPx() else 6.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = selectedChannel.color,
                        radius = if (isSelectedPoint) 6.dp.toPx() else 4.dp.toPx(),
                        center = Offset(px, py)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Drag points to adjust tone • Double tap to reset",
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}
