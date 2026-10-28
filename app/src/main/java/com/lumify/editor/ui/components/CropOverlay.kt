package com.lumify.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.AspectRatio
import com.lumify.editor.core.CropRect
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary

@Composable
fun CropOverlay(
    cropRect: CropRect,
    selectedRatio: AspectRatio,
    onCropChange: (CropRect) -> Unit,
    onRatioSelect: (AspectRatio) -> Unit,
    onRotate90: () -> Unit,
    onFlipHorizontal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
    ) {
        // Draw Scrim & Rule-of-Thirds Grid
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(canvasSize, selectedRatio) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x / canvasSize.width
                        val dy = dragAmount.y / canvasSize.height

                        var newL = (cropRect.left + dx).coerceIn(0f, 0.9f)
                        var newT = (cropRect.top + dy).coerceIn(0f, 0.9f)
                        var newR = (cropRect.right + dx).coerceIn(newL + 0.1f, 1f)
                        var newB = (cropRect.bottom + dy).coerceIn(newT + 0.1f, 1f)

                        // If moved against borders, preserve size
                        val curW = cropRect.width
                        val curH = cropRect.height
                        if (newR == 1f) newL = (1f - curW).coerceAtLeast(0f)
                        if (newB == 1f) newT = (1f - curH).coerceAtLeast(0f)

                        onCropChange(CropRect(newL, newT, newR, newB))
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            val leftPx = cropRect.left * w
            val topPx = cropRect.top * h
            val rightPx = cropRect.right * w
            val bottomPx = cropRect.bottom * h
            val cropW = rightPx - leftPx
            val cropH = bottomPx - topPx

            // 1. Inverted Scrim (dark outside, transparent inside)
            val scrimPath = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, w, h))
                addRect(Rect(leftPx, topPx, rightPx, bottomPx))
            }
            drawPath(scrimPath, color = Color.Black.copy(alpha = 0.6f))

            // 2. Crop Border
            drawRect(
                color = Color.White,
                topLeft = Offset(leftPx, topPx),
                size = Size(cropW, cropH),
                style = Stroke(width = 2.dp.toPx())
            )

            // 3. Rule of Thirds Grid Lines
            val colStep = cropW / 3f
            val rowStep = cropH / 3f

            drawLine(
                Color.White.copy(alpha = 0.4f),
                Offset(leftPx + colStep, topPx),
                Offset(leftPx + colStep, bottomPx),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                Color.White.copy(alpha = 0.4f),
                Offset(leftPx + colStep * 2, topPx),
                Offset(leftPx + colStep * 2, bottomPx),
                strokeWidth = 1.dp.toPx()
            )

            drawLine(
                Color.White.copy(alpha = 0.4f),
                Offset(leftPx, topPx + rowStep),
                Offset(rightPx, topPx + rowStep),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                Color.White.copy(alpha = 0.4f),
                Offset(leftPx, topPx + rowStep * 2),
                Offset(rightPx, topPx + rowStep * 2),
                strokeWidth = 1.dp.toPx()
            )

            // 4. Corner Handles
            val cornerLen = 22.dp.toPx()
            val cornerStroke = 4.dp.toPx()

            // Top-Left
            drawLine(Color.White, Offset(leftPx - 2, topPx), Offset(leftPx + cornerLen, topPx), cornerStroke)
            drawLine(Color.White, Offset(leftPx, topPx - 2), Offset(leftPx, topPx + cornerLen), cornerStroke)

            // Top-Right
            drawLine(Color.White, Offset(rightPx + 2, topPx), Offset(rightPx - cornerLen, topPx), cornerStroke)
            drawLine(Color.White, Offset(rightPx, topPx - 2), Offset(rightPx, topPx + cornerLen), cornerStroke)

            // Bottom-Left
            drawLine(Color.White, Offset(leftPx - 2, bottomPx), Offset(leftPx + cornerLen, bottomPx), cornerStroke)
            drawLine(Color.White, Offset(leftPx, bottomPx + 2), Offset(leftPx, bottomPx - cornerLen), cornerStroke)

            // Bottom-Right
            drawLine(Color.White, Offset(rightPx + 2, bottomPx), Offset(rightPx - cornerLen, bottomPx), cornerStroke)
            drawLine(Color.White, Offset(rightPx, bottomPx + 2), Offset(rightPx, bottomPx - cornerLen), cornerStroke)
        }

        // Bottom Controls for Ratios & Quick Rotate
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Transform Buttons (Rotate 90, Flip)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassButton(
                    onClick = onRotate90,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RotateRight,
                        contentDescription = "Rotate 90",
                        tint = TextPrimary
                    )
                }

                LiquidGlassButton(
                    onClick = onFlipHorizontal,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Flip Horizontal",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Aspect Ratios Ribbon
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
            ) {
                items(AspectRatio.entries) { ratio ->
                    val isSelected = ratio == selectedRatio
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentCyan else DarkSurfaceElevated)
                            .clickable { onRatioSelect(ratio) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = ratio.label,
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
