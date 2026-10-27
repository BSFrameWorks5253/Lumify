package com.lumify.editor.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumify.editor.ui.theme.DarkSurfaceGlass
import com.lumify.editor.ui.theme.GlassBorderHighlight
import com.lumify.editor.ui.theme.GlassBorderLowlight

/**
 * Authentic Apple-style Liquid Glass Surface.
 * 
 * Implements the 4 physical layers of mobile liquid glass:
 * 1. Translucent frosted substrate (DarkSurfaceGlass / colorless wash at 6-12% alpha).
 * 2. Directional specular rim lighting: Asymmetric multi-stop gradient (top-left 52% alpha
 *    fading to 16%, 4%, and transparent) simulating light incident on polished beveled glass edges.
 * 3. Inner refraction sheen: Subtle gradient wash simulating physical crystal glass thickness.
 * 4. Android 12+ (API 31+) hardware-accelerated RenderEffect blur for optical depth.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = DarkSurfaceGlass,
    elevation: Dp = 12.dp,
    borderWidth: Dp = 1.2.dp,
    enableHardwareBlur: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    // Multi-stop directional specular rim stroke
    val specularRimBrush = Brush.linearGradient(
        colors = listOf(
            GlassBorderHighlight.copy(alpha = 0.52f),
            GlassBorderHighlight.copy(alpha = 0.16f),
            GlassBorderLowlight.copy(alpha = 0.05f),
            Color.Transparent
        ),
        start = Offset.Zero,
        end = Offset(450f, 650f)
    )

    // Inner physical glass refraction sheen
    val innerSheenBrush = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.05f),
            Color.Transparent
        ),
        startY = 0f,
        endY = 140f
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                spotColor = Color.Black.copy(alpha = 0.55f),
                ambientColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(shape)
            .then(
                if (enableHardwareBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.graphicsLayer {
                        renderEffect = RenderEffect.createBlurEffect(
                            18f, 18f, Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                } else Modifier
            )
            .background(backgroundColor)
            .background(innerSheenBrush)
            .border(width = borderWidth, brush = specularRimBrush, shape = shape),
        content = content
    )
}

/**
 * Compact Liquid Glass Chip / Pill tier.
 * Designed for tags, ribbons, metadata badges, and compact toolbars.
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.06f),
    content: @Composable BoxScope.() -> Unit
) {
    val pillRim = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.08f),
            Color.Transparent
        ),
        start = Offset.Zero,
        end = Offset(150f, 150f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, brush = pillRim, shape = shape),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Interactive button with spring micro-interaction and authentic liquid glass styling.
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = DarkSurfaceGlass,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 550f),
        label = "glassButtonScale"
    )

    val specularBrush = Brush.linearGradient(
        colors = listOf(
            GlassBorderHighlight.copy(alpha = 0.55f),
            Color.White.copy(alpha = 0.18f),
            Color.Transparent
        ),
        start = Offset.Zero,
        end = Offset(200f, 200f)
    )

    val innerSheen = Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.06f),
            Color.Transparent
        ),
        startY = 0f,
        endY = 60f
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.03f))
            .background(innerSheen)
            .border(width = 1.dp, brush = specularBrush, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
