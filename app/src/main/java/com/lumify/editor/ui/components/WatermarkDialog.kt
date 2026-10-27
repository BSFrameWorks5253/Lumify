package com.lumify.editor.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lumify.editor.core.WatermarkConfig
import com.lumify.editor.core.WatermarkPosition
import com.lumify.editor.core.WatermarkType
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.DarkSurface
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary

@Composable
fun WatermarkDialog(
    currentConfig: WatermarkConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (WatermarkConfig) -> Unit
) {
    var isEnabled by remember { mutableStateOf(currentConfig.enabled) }
    var watermarkText by remember { mutableStateOf(currentConfig.text) }
    var opacity by remember { mutableStateOf(currentConfig.opacity) }
    var sizePercent by remember { mutableStateOf(currentConfig.sizePercent) }
    var position by remember { mutableStateOf(currentConfig.position) }

    Dialog(onDismissRequest = onDismiss) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = DarkSurface.copy(alpha = 0.95f),
            borderWidth = 1.5.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Photographer Watermark",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Brand exported master photos",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.35f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceElevated
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Watermark Text Input
                OutlinedTextField(
                    value = watermarkText,
                    onValueChange = { watermarkText = it },
                    label = { Text("Watermark Text / Signature", color = TextSecondary, fontSize = 12.sp) },
                    enabled = isEnabled,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        cursorColor = AccentCyan
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Position Selector Grid
                Text(
                    text = "Position Alignment",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    WatermarkPosition.values().forEach { pos ->
                        val isSelected = pos == position
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentCyan.copy(alpha = 0.25f) else DarkSurfaceElevated)
                                .border(
                                    width = if (isSelected) 1.2.dp else 0.dp,
                                    color = if (isSelected) AccentCyan else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = isEnabled) { position = pos }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (pos) {
                                    WatermarkPosition.BOTTOM_RIGHT -> "BR"
                                    WatermarkPosition.BOTTOM_LEFT -> "BL"
                                    WatermarkPosition.TOP_RIGHT -> "TR"
                                    WatermarkPosition.TOP_LEFT -> "TL"
                                    WatermarkPosition.CENTER -> "CTR"
                                },
                                color = if (isSelected) AccentCyan else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Opacity & Scale Sliders
                AdjustSlider(
                    label = "Watermark Opacity",
                    value = opacity * 100f,
                    range = 10f..100f,
                    defaultValue = 85f,
                    unitSuffix = "%",
                    onValueChange = { opacity = it / 100f }
                )

                AdjustSlider(
                    label = "Watermark Scale",
                    value = sizePercent,
                    range = 6f..35f,
                    defaultValue = 14f,
                    unitSuffix = "%",
                    onValueChange = { sizePercent = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LiquidGlassButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        backgroundColor = DarkSurfaceElevated
                    ) {
                        Text(
                            text = "Cancel",
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    LiquidGlassButton(
                        onClick = {
                            val updated = currentConfig.copy(
                                enabled = isEnabled,
                                text = watermarkText,
                                opacity = opacity,
                                sizePercent = sizePercent,
                                position = position
                            )
                            onSaveConfig(updated)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        backgroundColor = AccentCyan.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "Save Watermark",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
