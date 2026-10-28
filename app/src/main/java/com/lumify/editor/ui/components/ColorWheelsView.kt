package com.lumify.editor.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.ColorGradingState
import com.lumify.editor.core.ColorWheelChannel
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class WheelRange(val label: String, val defaultHue: Float) {
    SHADOWS("Shadows", 210f),
    MIDTONES("Midtones", 30f),
    HIGHLIGHTS("Highlights", 45f)
}

/**
 * Hollywood / DaVinci Resolve 3-Way Color Wheels for Cinema Split Toning.
 * Displays interactive 360-degree chromatic disc with draggable hue/saturation puck,
 * circular luminance arc, and global Blending & Balance controls.
 */
@Composable
fun ColorWheelsView(
    colorGrading: ColorGradingState,
    onColorGradingChange: (ColorGradingState) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(WheelRange.SHADOWS) }

    val activeChannel = when (selectedRange) {
        WheelRange.SHADOWS -> colorGrading.shadows
        WheelRange.MIDTONES -> colorGrading.midtones
        WheelRange.HIGHLIGHTS -> colorGrading.highlights
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Range Ribbon Selector (Shadows / Midtones / Highlights)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelRange.entries.forEach { range ->
                val isSelected = range == selectedRange
                val channel = when (range) {
                    WheelRange.SHADOWS -> colorGrading.shadows
                    WheelRange.MIDTONES -> colorGrading.midtones
                    WheelRange.HIGHLIGHTS -> colorGrading.highlights
                }
                val hasTuning = !channel.isDefault

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AccentCyan.copy(alpha = 0.22f) else DarkSurfaceElevated)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) AccentCyan else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedRange = range }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasTuning) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(AccentCyan)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = range.label,
                            color = if (isSelected) AccentCyan else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Area: Color Disc & Draggable Puck
        val wheelSize = 126.dp
        Box(
            modifier = Modifier
                .size(wheelSize)
                .clip(CircleShape)
                .background(Color(0xFF101217)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(selectedRange, activeChannel) {
                        detectTapGestures(
                            onDoubleTap = {
                                // Double tap resets active wheel to neutral
                                val resetChannel = ColorWheelChannel()
                                val updated = when (selectedRange) {
                                    WheelRange.SHADOWS -> colorGrading.copy(shadows = resetChannel)
                                    WheelRange.MIDTONES -> colorGrading.copy(midtones = resetChannel)
                                    WheelRange.HIGHLIGHTS -> colorGrading.copy(highlights = resetChannel)
                                }
                                onColorGradingChange(updated)
                            }
                        )
                    }
                    .pointerInput(selectedRange, activeChannel) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = min(size.width, size.height) / 2f - 8.dp.toPx()

                            val dx = change.position.x - center.x
                            val dy = change.position.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)

                            val normSat = (dist / radius).coerceIn(0f, 1f) * 100f
                            var angle = atan2(dy, dx) * (180f / PI.toFloat())
                            if (angle < 0) angle += 360f

                            val newChannel = activeChannel.copy(
                                hue = angle,
                                saturation = normSat
                            )

                            val updated = when (selectedRange) {
                                WheelRange.SHADOWS -> colorGrading.copy(shadows = newChannel)
                                WheelRange.MIDTONES -> colorGrading.copy(midtones = newChannel)
                                WheelRange.HIGHLIGHTS -> colorGrading.copy(highlights = newChannel)
                            }
                            onColorGradingChange(updated)
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 8.dp.toPx()

                // 360-degree chromatic sweep ring
                val sweepColors = listOf(
                    Color.Red, Color.Yellow, Color.Green,
                    Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
                drawCircle(
                    brush = Brush.sweepGradient(sweepColors, center),
                    radius = radius,
                    center = center
                )

                // White radial falloff to center (desaturates toward center)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Dark border ring
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Neutral center crosshair
                drawCircle(Color.Black.copy(alpha = 0.4f), radius = 3.dp.toPx(), center = center)

                // Draggable Color Puck
                val rad = activeChannel.hue * (PI.toFloat() / 180f)
                val puckDist = (activeChannel.saturation / 100f) * radius
                val puckX = center.x + puckDist * cos(rad)
                val puckY = center.y + puckDist * sin(rad)

                // Puck outer shadow ring
                drawCircle(Color.Black, radius = 9.dp.toPx(), center = Offset(puckX, puckY))
                drawCircle(AccentCyan, radius = 7.dp.toPx(), center = Offset(puckX, puckY))
                drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(puckX, puckY))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Active Channel Luminance Slider
        AdjustSlider(
            label = "${selectedRange.label} Luminance",
            value = activeChannel.luminance,
            onValueChange = { lum ->
                val newChannel = activeChannel.copy(luminance = lum)
                val updated = when (selectedRange) {
                    WheelRange.SHADOWS -> colorGrading.copy(shadows = newChannel)
                    WheelRange.MIDTONES -> colorGrading.copy(midtones = newChannel)
                    WheelRange.HIGHLIGHTS -> colorGrading.copy(highlights = newChannel)
                }
                onColorGradingChange(updated)
            }
        )

        // Global Blending & Balance Sliders
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AdjustSlider(
                    label = "Blending",
                    value = colorGrading.blending,
                    range = 0f..100f,
                    defaultValue = 50f,
                    onValueChange = { onColorGradingChange(colorGrading.copy(blending = it)) }
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                AdjustSlider(
                    label = "Balance",
                    value = colorGrading.balance,
                    range = -100f..100f,
                    defaultValue = 0f,
                    onValueChange = { onColorGradingChange(colorGrading.copy(balance = it)) }
                )
            }
        }
    }
}
