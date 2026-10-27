package com.lumify.editor.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.lumify.editor.ui.theme.DarkBackground

/**
 * Ambient Liquid Mesh Backdrop.
 * 
 * In accordance with Apple Liquid Glass design principles:
 * "Glass shows what is behind it. Behind a flat black background, frosted glass renders as
 * a flat dark gray box with no optical life. The ambient chromatic mesh is part of the recipe:
 * subtle, off-screen radial light orbs in cyan, deep violet, and warm amber fixed to the viewport
 * behind the content. This provides the exact ambient spectral energy that frosted glass refracts."
 */
@Composable
fun AmbientLiquidBackdrop(
    modifier: Modifier = Modifier,
    enableDrift: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambientMesh")
    val driftAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientMeshDrift"
    )

    val drift = if (enableDrift) driftAnim else 0.5f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Chromatic Ambient Mesh Layer
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Deep Cyan Orb (Top-Right / Camera Focal Light)
            val cyanCenter = Offset(
                x = w * (0.85f - 0.08f * drift),
                y = h * (0.12f + 0.06f * drift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.075f),
                        Color(0xFF00B4D8).copy(alpha = 0.035f),
                        Color.Transparent
                    ),
                    center = cyanCenter,
                    radius = w * 0.72f
                ),
                center = cyanCenter,
                radius = w * 0.72f
            )

            // 2. Electric Violet / Indigo Orb (Mid-Left / Chromatic Depth)
            val violetCenter = Offset(
                x = w * (0.10f + 0.08f * drift),
                y = h * (0.45f - 0.05f * drift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C4DFF).copy(alpha = 0.065f),
                        Color(0xFF651FFF).copy(alpha = 0.025f),
                        Color.Transparent
                    ),
                    center = violetCenter,
                    radius = w * 0.65f
                ),
                center = violetCenter,
                radius = w * 0.65f
            )

            // 3. Warm Studio Tungsten / Amber Glow (Bottom-Right / Optical Balance)
            val amberCenter = Offset(
                x = w * (0.75f - 0.05f * drift),
                y = h * (0.82f + 0.04f * drift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF59E0B).copy(alpha = 0.045f),
                        Color(0xFFD97706).copy(alpha = 0.015f),
                        Color.Transparent
                    ),
                    center = amberCenter,
                    radius = w * 0.55f
                ),
                center = amberCenter,
                radius = w * 0.55f
            )
        }

        // Foreground UI Content
        content()
    }
}
