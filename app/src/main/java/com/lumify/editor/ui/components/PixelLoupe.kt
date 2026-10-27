package com.lumify.editor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkSurface
import kotlin.math.roundToInt

/**
 * 1:1 Deep Zoom Pixel Loupe:
 * A floating circular 130dp optical loupe showing uncompressed 1:1 / 2:1 pixel detail
 * directly under or offset from the photographer's finger to inspect tack-sharp focus and noise.
 */
@Composable
fun PixelLoupe(
    bitmap: Bitmap?,
    touchPointNorm: Offset, // Normalized [0, 1] coordinate on image
    screenTouchOffset: Offset, // Screen pixel coordinate
    modifier: Modifier = Modifier,
    zoomFactor: Float = 2.0f
) {
    if (bitmap == null) return

    val loupeDiameter = 136.dp

    // Calculate crop rectangle in original bitmap coordinate space
    val origW = bitmap.width
    val origH = bitmap.height

    val centerX = (touchPointNorm.x * origW).toInt().coerceIn(0, origW - 1)
    val centerY = (touchPointNorm.y * origH).toInt().coerceIn(0, origH - 1)

    // Crop size depends on zoom factor (e.g. 100px x 100px for 2x)
    val cropRadius = (60 / zoomFactor).toInt()
    val cropLeft = (centerX - cropRadius).coerceIn(0, origW - 1)
    val cropTop = (centerY - cropRadius).coerceIn(0, origH - 1)
    val cropWidth = (cropRadius * 2).coerceAtMost(origW - cropLeft).coerceAtLeast(1)
    val cropHeight = (cropRadius * 2).coerceAtMost(origH - cropTop).coerceAtLeast(1)

    val cropBmp = try {
        Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
    } catch (e: Exception) {
        null
    }

    // Offset the loupe above finger so the finger doesn't block the view
    val offsetX = (screenTouchOffset.x - 70).roundToInt()
    val offsetY = (screenTouchOffset.y - 180).coerceAtLeast(80f).roundToInt()

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX, offsetY) }
            .size(loupeDiameter)
            .shadow(16.dp, CircleShape, spotColor = AccentCyan.copy(alpha = 0.5f))
            .border(2.5.dp, AccentCyan, CircleShape)
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (cropBmp != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val imageBitmap = cropBmp.asImageBitmap()
                drawImage(
                    image = imageBitmap,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                )

                val center = Offset(size.width / 2f, size.height / 2f)

                // Precision Reticle Crosshairs
                val reticleColor = AccentCyan.copy(alpha = 0.85f)
                val reticleLen = 14.dp.toPx()
                val reticleGap = 4.dp.toPx()

                // Horizontal crosshair lines
                drawLine(reticleColor, Offset(center.x - reticleLen, center.y), Offset(center.x - reticleGap, center.y), strokeWidth = 1.5.dp.toPx())
                drawLine(reticleColor, Offset(center.x + reticleGap, center.y), Offset(center.x + reticleLen, center.y), strokeWidth = 1.5.dp.toPx())

                // Vertical crosshair lines
                drawLine(reticleColor, Offset(center.x, center.y - reticleLen), Offset(center.x, center.y - reticleGap), strokeWidth = 1.5.dp.toPx())
                drawLine(reticleColor, Offset(center.x, center.y + reticleGap), Offset(center.x, center.y + reticleLen), strokeWidth = 1.5.dp.toPx())

                // Center aim dot
                drawCircle(reticleColor, radius = 2.dp.toPx(), center = center)

                // Outer bezel gauge
                drawCircle(Color.White.copy(alpha = 0.2f), radius = size.minDimension / 2f - 2.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
            }
        }

        // Loupe Badge on Bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-6).dp)
                .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                .border(0.8.dp, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
        ) {
            Text(
                text = "1:1 FOCUS",
                color = AccentCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
