package com.lumify.editor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.ui.theme.AccentCyan
import kotlin.math.roundToInt

@Composable
fun SplitComparisonView(
    originalBitmap: Bitmap,
    editedBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var splitFraction by remember { mutableFloatStateOf(0.5f) }
    var containerWidth by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { containerWidth = it.width.toFloat().coerceAtLeast(1f) }
    ) {
        // 1. Edited Bitmap (Full background)
        Image(
            bitmap = editedBitmap.asImageBitmap(),
            contentDescription = "Edited Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Original Bitmap (Clipped to left side of splitFraction)
        Image(
            bitmap = originalBitmap.asImageBitmap(),
            contentDescription = "Original Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    val clipWidth = size.width * splitFraction
                    drawContext.canvas.save()
                    drawContext.canvas.clipRect(
                        Rect(0f, 0f, clipWidth, size.height)
                    )
                    this@drawWithContent.drawContent()
                    drawContext.canvas.restore()
                }
        )

        // 3. Divider Line & Draggable Knob
        val dividerX = (containerWidth * splitFraction)

        // Vertical divider line
        Box(
            modifier = Modifier
                .offset { IntOffset(dividerX.roundToInt(), 0) }
                .width(2.dp)
                .fillMaxSize()
                .background(Color.White)
        )

        // Handle knob
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (dividerX - 20.dp.toPx()).roundToInt(),
                        (containerWidth * 0.4f).roundToInt()
                    )
                }
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.75f))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newFraction = (splitFraction + (dragAmount.x / containerWidth))
                            .coerceIn(0.05f, 0.95f)
                        splitFraction = newFraction
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = "Drag to compare",
                tint = AccentCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        // Before & After labels
        Text(
            text = "ORIGINAL",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Text(
            text = "EDITED",
            color = AccentCyan,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
