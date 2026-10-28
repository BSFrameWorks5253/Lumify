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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.SelectiveParam
import com.lumify.editor.core.SelectivePoint
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun SelectiveUPointOverlay(
    points: List<SelectivePoint>,
    selectedIndex: Int,
    containerSize: IntSize,
    onAddPoint: (Float, Float) -> Unit,
    onSelectPoint: (Int) -> Unit,
    onUpdatePoint: (Int, (SelectivePoint) -> SelectivePoint) -> Unit,
    onDeletePoint: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePoint = points.getOrNull(selectedIndex)

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Gesture layer to add, tap, and drag selective control points
        var totalDragX by remember { mutableFloatStateOf(0f) }
        var totalDragY by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points, selectedIndex, containerSize) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val tapNormX = (tapOffset.x / w).coerceIn(0f, 1f)
                        val tapNormY = (tapOffset.y / h).coerceIn(0f, 1f)

                        // Check if tapping existing point
                        val hitIndex = points.indexOfFirst { pt ->
                            val px = pt.normX * w
                            val py = pt.normY * h
                            hypot(tapOffset.x - px, tapOffset.y - py) < 48f
                        }

                        if (hitIndex != -1) {
                            onSelectPoint(hitIndex)
                        } else {
                            // Drop new U-Point
                            onAddPoint(tapNormX, tapNormY)
                        }
                    }
                }
                .pointerInput(selectedIndex) {
                    detectDragGestures(
                        onDragStart = {
                            totalDragX = 0f
                            totalDragY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount.x
                            totalDragY += dragAmount.y

                            if (selectedIndex in points.indices) {
                                // Vertical drag switches parameter: B -> C -> S -> St
                                if (abs(totalDragY) > 45f) {
                                    val dir = if (totalDragY > 0) 1 else -1
                                    val currentParam = points[selectedIndex].activeParam
                                    val values = SelectiveParam.entries
                                    val newIdx = (currentParam.ordinal + dir).coerceIn(0, values.lastIndex)
                                    onUpdatePoint(selectedIndex) { it.copy(activeParam = values[newIdx]) }
                                    totalDragY = 0f
                                }

                                // Horizontal drag adjusts value
                                if (abs(dragAmount.x) > 1.2f) {
                                    val delta = dragAmount.x * 0.45f
                                    onUpdatePoint(selectedIndex) { pt ->
                                        when (pt.activeParam) {
                                            SelectiveParam.BRIGHTNESS -> pt.copy(brightness = (pt.brightness + delta).coerceIn(-100f, 100f))
                                            SelectiveParam.CONTRAST -> pt.copy(contrast = (pt.contrast + delta).coerceIn(-100f, 100f))
                                            SelectiveParam.SATURATION -> pt.copy(saturation = (pt.saturation + delta).coerceIn(-100f, 100f))
                                            SelectiveParam.STRUCTURE -> pt.copy(structure = (pt.structure + delta).coerceIn(-100f, 100f))
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            // Draw interactive rings and center pins
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                points.forEachIndexed { idx, pt ->
                    val isSelected = (idx == selectedIndex)
                    val cx = pt.normX * w
                    val cy = pt.normY * h
                    val radiusPx = pt.radius * max(w, h)

                    // Outer radius circle
                    if (isSelected) {
                        drawCircle(
                            color = AccentCyan.copy(alpha = 0.28f),
                            radius = radiusPx,
                            center = Offset(cx, cy),
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                            )
                        )
                    }

                    // Inner pin center
                    drawCircle(
                        color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.85f),
                        radius = if (isSelected) 18f else 12f,
                        center = Offset(cx, cy)
                    )

                    drawCircle(
                        color = Color.Black,
                        radius = if (isSelected) 6f else 4f,
                        center = Offset(cx, cy)
                    )
                }
            }
        }

        // 2. Active Point Floating Control Card
        if (activePoint != null && selectedIndex in points.indices) {
            val currVal = when (activePoint.activeParam) {
                SelectiveParam.BRIGHTNESS -> activePoint.brightness
                SelectiveParam.CONTRAST -> activePoint.contrast
                SelectiveParam.SATURATION -> activePoint.saturation
                SelectiveParam.STRUCTURE -> activePoint.structure
            }.roundToInt()
            val valStr = if (currVal > 0) "+$currVal" else "$currVal"

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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(AccentCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = activePoint.activeParam.shortName,
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = activePoint.activeParam.label.uppercase(),
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Swipe ↕ to change • ↔ to tune",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = valStr,
                                    color = AccentCyan,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onDeletePoint(selectedIndex) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Point",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Param Pill Selector (B, C, S, St)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SelectiveParam.entries.forEach { param ->
                                val isSelected = (activePoint.activeParam == param)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) AccentCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, if (isSelected) AccentCyan else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable {
                                            onUpdatePoint(selectedIndex) { it.copy(activeParam = param) }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = param.shortName,
                                        color = if (isSelected) AccentCyan else TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
