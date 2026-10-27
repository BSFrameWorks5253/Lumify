package com.lumify.editor.ui.components

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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumify.editor.ui.theme.DarkSurfaceGlass
import com.lumify.editor.ui.theme.GlassBorderHighlight
import com.lumify.editor.ui.theme.GlassBorderLowlight

/**
 * High-performance Liquid Glass surface with directional specular rim lighting
 * and frosted translucency designed for mobile GPUs.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    backgroundColor: Color = DarkSurfaceGlass,
    elevation: Dp = 12.dp,
    borderWidth: Dp = 1.2.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val specularRimBrush = Brush.linearGradient(
        colors = listOf(
            GlassBorderHighlight,
            GlassBorderHighlight.copy(alpha = 0.2f),
            GlassBorderLowlight,
            Color.Transparent
        ),
        start = Offset.Zero,
        end = Offset(400f, 600f)
    )

    Box(
        modifier = modifier
            .shadow(elevation, shape = shape, spotColor = Color.Black.copy(alpha = 0.45f))
            .clip(shape)
            .background(backgroundColor)
            .border(width = borderWidth, brush = specularRimBrush, shape = shape),
        content = content
    )
}

/**
 * Interactive button with spring micro-interaction and liquid glass styling.
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
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "glassButtonScale"
    )

    val specularBrush = Brush.linearGradient(
        colors = listOf(
            GlassBorderHighlight,
            Color.White.copy(alpha = 0.15f),
            Color.Transparent
        ),
        start = Offset.Zero,
        end = Offset(200f, 200f)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.03f))
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
