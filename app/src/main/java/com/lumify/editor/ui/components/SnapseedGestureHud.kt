package com.lumify.editor.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.ui.screens.SnapseedTool
import com.lumify.editor.ui.screens.ToolParameter
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.DarkSurface
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import kotlin.math.roundToInt

/**
 * Snapseed Top Header Bar:
 * Displays active parameter name, value (-100 to +100), and horizontal fill progress bar.
 */
@Composable
fun SnapseedTopHud(
    activeParam: ToolParameter?,
    currentValue: Float,
    modifier: Modifier = Modifier
) {
    if (activeParam == null) return

    val min = activeParam.minValue
    val max = activeParam.maxValue
    val range = max - min
    val intVal = currentValue.roundToInt()
    val valueStr = if (intVal > 0) "+$intVal" else "$intVal"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeParam.name.uppercase(),
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Text(
                        text = valueStr,
                        color = AccentCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Snapseed Bi-directional or Uni-directional Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    if (min < 0f && max > 0f) {
                        // Centered zero: fills left for negative, right for positive
                        val normPos = (currentValue / max).coerceIn(-1f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(kotlin.math.abs(normPos) * 0.5f)
                                .height(3.dp)
                                .align(if (normPos >= 0f) Alignment.CenterStart else Alignment.CenterEnd)
                                .background(AccentCyan)
                        )
                    } else {
                        // Starts from left
                        val normPos = if (range > 0f) ((currentValue - min) / range).coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(normPos)
                                .height(3.dp)
                                .background(AccentCyan)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Snapseed Vertical Swipe HUD Menu:
 * Floats in center of screen when dragging up/down, showing list of parameters.
 */
@Composable
fun SnapseedFloatingParamSelector(
    parameters: List<ToolParameter>,
    selectedIndex: Int,
    visible: Boolean,
    onSelectParam: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        LiquidGlassSurface(
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                parameters.forEachIndexed { index, param ->
                    val isSelected = (index == selectedIndex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) AccentCyan.copy(alpha = 0.22f) else Color.Transparent)
                            .clickable { onSelectParam(index) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = param.name,
                            color = if (isSelected) AccentCyan else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AccentCyan)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Snapseed Bottom Tool Action Bar:
 * ✕ (Cancel) | ☰ (Parameters Menu) | ◫ (Compare) | ✓ (Apply)
 */
@Composable
fun SnapseedBottomToolBar(
    onCancel: () -> Unit,
    onOpenParamList: () -> Unit,
    onCompareToggle: (Boolean) -> Unit,
    onApply: () -> Unit,
    hasParameters: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidGlassSurface(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel '✕'
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Parameter list quick toggle
                    if (hasParameters) {
                        IconButton(
                            onClick = onOpenParamList,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Parameters",
                                tint = AccentCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    // Compare Button
                    IconButton(
                        onClick = { /* Toggle */ },
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Compare",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Apply '✓'
                IconButton(
                    onClick = onApply,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
