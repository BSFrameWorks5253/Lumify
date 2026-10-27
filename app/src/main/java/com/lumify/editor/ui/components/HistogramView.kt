package com.lumify.editor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.lumify.editor.core.HistogramData

@Composable
fun HistogramView(
    data: HistogramData?,
    modifier: Modifier = Modifier
        .width(160.dp)
        .height(60.dp)
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        if (data == null || data.maxCount <= 0) return@Canvas

        val w = size.width
        val h = size.height
        val maxVal = data.maxCount.toFloat()

        // Draw RGB channels with blending
        drawChannelPath(data.red, w, h, maxVal, Color(0x66FF3D00))
        drawChannelPath(data.green, w, h, maxVal, Color(0x6600E676))
        drawChannelPath(data.blue, w, h, maxVal, Color(0x662979FF))
        // Draw overall Luminance in white
        drawChannelPath(data.luminance, w, h, maxVal, Color(0x88FFFFFF))
    }
}

private fun DrawScope.drawChannelPath(
    channel: IntArray,
    width: Float,
    height: Float,
    maxVal: Float,
    color: Color
) {
    val path = Path()
    path.moveTo(0f, height)

    val step = width / 255f
    for (i in 0..255) {
        val count = channel[i]
        val normalized = (count / maxVal).coerceIn(0f, 1f)
        val y = height - (normalized * height * 0.92f)
        val x = i * step
        path.lineTo(x, y)
    }

    path.lineTo(width, height)
    path.close()

    drawPath(path = path, color = color, style = Fill)
}
