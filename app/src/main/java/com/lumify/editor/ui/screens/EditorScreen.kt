package com.lumify.editor.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.lumify.editor.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.CropRect
import com.lumify.editor.core.HslAdjustment
import com.lumify.editor.core.HslChannel
import com.lumify.editor.presets.PresetRepository
import com.lumify.editor.ui.components.AdjustSlider
import com.lumify.editor.ui.components.CropOverlay
import com.lumify.editor.ui.components.HistogramView
import com.lumify.editor.ui.components.LiquidGlassButton
import com.lumify.editor.ui.components.LiquidGlassSurface
import com.lumify.editor.ui.components.SplitComparisonView
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.DarkSurface
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.HslBlue
import com.lumify.editor.ui.theme.HslCyan
import com.lumify.editor.ui.theme.HslGreen
import com.lumify.editor.ui.theme.HslMagenta
import com.lumify.editor.ui.theme.HslOrange
import com.lumify.editor.ui.theme.HslPurple
import com.lumify.editor.ui.theme.HslRed
import com.lumify.editor.ui.theme.HslYellow
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. Image Canvas & Interactive Viewport
        val displayBitmap = if (uiState.isComparing) uiState.originalBitmap else uiState.previewBitmap
        val original = uiState.originalBitmap

        if (displayBitmap != null && original != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale > 1f) {
                                Offset(offset.x + pan.x, offset.y + pan.y)
                            } else {
                                Offset.Zero
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = "Active Image Canvas",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )

                // Crop Overlay when in Crop Tab
                if (uiState.activeTab == EditorTab.CROP) {
                    CropOverlay(
                        cropRect = uiState.editState.cropRect ?: CropRect(),
                        selectedRatio = uiState.editState.selectedAspectRatio,
                        onCropChange = { newCrop ->
                            viewModel.updateEditState { it.copy(cropRect = newCrop) }
                        },
                        onRatioSelect = { ratio -> viewModel.setAspectRatio(ratio) },
                        onRotate90 = { viewModel.rotate90() },
                        onFlipHorizontal = { viewModel.flipHorizontal() }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
        }

        // 2. Top Liquid Glass Action Bar
        LiquidGlassSurface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = DarkSurface.copy(alpha = 0.75f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                // History & Compare controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) TextPrimary else TextMuted
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) TextPrimary else TextMuted
                        )
                    }

                    // Reset
                    IconButton(
                        onClick = { viewModel.resetEdits() },
                        enabled = uiState.editState.isModified
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (uiState.editState.isModified) AccentCyan else TextMuted
                        )
                    }

                    // Hold/Click to Compare
                    IconButton(
                        onClick = { viewModel.toggleComparison(!uiState.isComparing) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Compare",
                            tint = if (uiState.isComparing) AccentCyan else TextPrimary
                        )
                    }
                }

                // Export CTA Button
                LiquidGlassButton(
                    onClick = { viewModel.setShowExportDialog(true) },
                    modifier = Modifier.height(38.dp),
                    backgroundColor = AccentCyan.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export",
                            tint = AccentCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Export",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Live Floating Histogram & Metadata Badge
        AnimatedVisibility(
            visible = uiState.histogramData != null && uiState.activeTab != EditorTab.CROP,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 70.dp, end = 16.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                HistogramView(data = uiState.histogramData)
                uiState.metadata?.let { meta ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${meta.format} • ${meta.width}x${meta.height}${if (meta.iso != null) " • " + meta.iso else ""}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. Bottom Studio Control Panel (Liquid Glass Sheet)
        LiquidGlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            backgroundColor = DarkSurface.copy(alpha = 0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // Adjustment Slider Area (Scrollable per active tab)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    when (uiState.activeTab) {
                        EditorTab.LIGHT -> LightControls(viewModel, uiState)
                        EditorTab.COLOR -> ColorControls(viewModel, uiState)
                        EditorTab.HSL -> HslControls(viewModel, uiState)
                        EditorTab.DETAIL -> DetailControls(viewModel, uiState)
                        EditorTab.CROP -> CropControls(viewModel, uiState)
                        EditorTab.PRESETS -> PresetsControls(viewModel, uiState)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Tab Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorTabItem(
                        icon = Icons.Default.LightMode,
                        tab = EditorTab.LIGHT,
                        activeTab = uiState.activeTab,
                        onClick = { viewModel.setActiveTab(EditorTab.LIGHT) }
                    )
                    EditorTabItem(
                        icon = Icons.Default.Palette,
                        tab = EditorTab.COLOR,
                        activeTab = uiState.activeTab,
                        onClick = { viewModel.setActiveTab(EditorTab.COLOR) }
                    )
                    EditorTabItem(
                        icon = Icons.Default.InvertColors,
                        tab = EditorTab.HSL,
                        activeTab = uiState.activeTab,
                        onClick = { viewModel.setActiveTab(EditorTab.HSL) }
                    )
                    EditorTabItem(
                        icon = Icons.Default.ZoomIn,
                        tab = EditorTab.DETAIL,
                        activeTab = uiState.activeTab,
                        onClick = { viewModel.setActiveTab(EditorTab.DETAIL) }
                    )
                    EditorTabItem(
                        icon = Icons.Default.Crop,
                        tab = EditorTab.CROP,
                        activeTab = uiState.activeTab,
                        onClick = { viewModel.setActiveTab(EditorTab.CROP) }
                    )
                    EditorTabItem(
                        icon = Icons.Default.AutoAwesome,
                        tab = EditorTab.PRESETS,
                        activeTab = uiState.activeTab,
                        onClick = { viewModel.setActiveTab(EditorTab.PRESETS) }
                    )
                }
            }
        }

        // Export Modal Dialog
        if (uiState.showExportDialog) {
            ExportDialog(
                onDismiss = { viewModel.setShowExportDialog(false) },
                onConfirmExport = { format, quality, preserveExif ->
                    viewModel.exportImage(context, format, quality, preserveExif)
                }
            )
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}

@Composable
private fun EditorTabItem(
    icon: ImageVector,
    tab: EditorTab,
    activeTab: EditorTab,
    onClick: () -> Unit
) {
    val isSelected = tab == activeTab
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tab.label,
            tint = if (isSelected) AccentCyan else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = tab.label,
            color = if (isSelected) AccentCyan else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun LightControls(viewModel: EditorViewModel, uiState: EditorUiState) {
    val scroll = rememberScrollState()
    val edit = uiState.editState

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        AdjustSlider(
            label = "Exposure",
            value = edit.exposure,
            onValueChange = { viewModel.updateEditState { s -> s.copy(exposure = it) } }
        )
        AdjustSlider(
            label = "Contrast",
            value = edit.contrast,
            onValueChange = { viewModel.updateEditState { s -> s.copy(contrast = it) } }
        )
        AdjustSlider(
            label = "Highlights",
            value = edit.highlights,
            onValueChange = { viewModel.updateEditState { s -> s.copy(highlights = it) } }
        )
        AdjustSlider(
            label = "Shadows",
            value = edit.shadows,
            onValueChange = { viewModel.updateEditState { s -> s.copy(shadows = it) } }
        )
        AdjustSlider(
            label = "Whites",
            value = edit.whites,
            onValueChange = { viewModel.updateEditState { s -> s.copy(whites = it) } }
        )
        AdjustSlider(
            label = "Blacks",
            value = edit.blacks,
            onValueChange = { viewModel.updateEditState { s -> s.copy(blacks = it) } }
        )
    }
}

@Composable
private fun ColorControls(viewModel: EditorViewModel, uiState: EditorUiState) {
    val scroll = rememberScrollState()
    val edit = uiState.editState

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        AdjustSlider(
            label = "Temp (White Balance)",
            value = edit.temperature,
            onValueChange = { viewModel.updateEditState { s -> s.copy(temperature = it) } }
        )
        AdjustSlider(
            label = "Tint",
            value = edit.tint,
            onValueChange = { viewModel.updateEditState { s -> s.copy(tint = it) } }
        )
        AdjustSlider(
            label = "Vibrance",
            value = edit.vibrance,
            onValueChange = { viewModel.updateEditState { s -> s.copy(vibrance = it) } }
        )
        AdjustSlider(
            label = "Saturation",
            value = edit.saturation,
            onValueChange = { viewModel.updateEditState { s -> s.copy(saturation = it) } }
        )
    }
}

@Composable
private fun HslControls(viewModel: EditorViewModel, uiState: EditorUiState) {
    val currentChannel = uiState.activeHslChannel
    val adjustment = uiState.editState.hslAdjustments[currentChannel] ?: HslAdjustment()

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal Color Dots Ribbon
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(HslChannel.values()) { channel ->
                val isSelected = channel == currentChannel
                val channelColor = getHslChannelColor(channel)

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(channelColor)
                        .clickable { viewModel.setActiveHslChannel(channel) }
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_color_droplet),
                        contentDescription = channel.displayName,
                        tint = if (isSelected) Color.White else Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AdjustSlider(
            label = "${currentChannel.displayName} Hue Shift",
            value = adjustment.hueShift,
            onValueChange = { viewModel.updateHslAdjustment(currentChannel, adjustment.copy(hueShift = it)) },
            activeColor = getHslChannelColor(currentChannel)
        )
        AdjustSlider(
            label = "${currentChannel.displayName} Saturation",
            value = adjustment.saturation,
            onValueChange = { viewModel.updateHslAdjustment(currentChannel, adjustment.copy(saturation = it)) },
            activeColor = getHslChannelColor(currentChannel)
        )
        AdjustSlider(
            label = "${currentChannel.displayName} Luminance",
            value = adjustment.luminance,
            onValueChange = { viewModel.updateHslAdjustment(currentChannel, adjustment.copy(luminance = it)) },
            activeColor = getHslChannelColor(currentChannel)
        )
    }
}

@Composable
private fun DetailControls(viewModel: EditorViewModel, uiState: EditorUiState) {
    val scroll = rememberScrollState()
    val edit = uiState.editState

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        AdjustSlider(
            label = "Sharpness",
            value = edit.sharpness,
            range = 0f..100f,
            defaultValue = 0f,
            onValueChange = { viewModel.updateEditState { s -> s.copy(sharpness = it) } }
        )
        AdjustSlider(
            label = "Clarity (Midtone Contrast)",
            value = edit.clarity,
            onValueChange = { viewModel.updateEditState { s -> s.copy(clarity = it) } }
        )
        AdjustSlider(
            label = "Vignette",
            value = edit.vignette,
            range = 0f..100f,
            defaultValue = 0f,
            onValueChange = { viewModel.updateEditState { s -> s.copy(vignette = it) } }
        )
    }
}

@Composable
private fun CropControls(viewModel: EditorViewModel, uiState: EditorUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Use gesture handles on canvas to crop",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Aspect Ratio: ${uiState.editState.selectedAspectRatio.label} • Rotation: ${uiState.editState.rotationDegrees.toInt()}°",
            color = TextMuted,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        AdjustSlider(
            label = "Fine Tilt Angle",
            value = uiState.editState.fineRotation,
            range = -45f..45f,
            defaultValue = 0f,
            unitSuffix = "°",
            onValueChange = { viewModel.updateEditState { s -> s.copy(fineRotation = it) } }
        )
    }
}

@Composable
private fun PresetsControls(viewModel: EditorViewModel, uiState: EditorUiState) {
    val presets = PresetRepository.allPresets

    LazyRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        items(presets) { preset ->
            val isApplied = preset.id == uiState.editState.appliedPresetId

            LiquidGlassSurface(
                modifier = Modifier
                    .width(130.dp)
                    .clickable { viewModel.applyPreset(preset) },
                shape = RoundedCornerShape(18.dp),
                backgroundColor = if (isApplied) AccentCyan.copy(alpha = 0.15f) else DarkSurfaceElevated.copy(alpha = 0.7f),
                borderWidth = if (isApplied) 2.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = preset.name,
                        color = if (isApplied) AccentCyan else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = preset.description,
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = preset.category.uppercase(),
                        color = if (isApplied) AccentCyan else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun getHslChannelColor(channel: HslChannel): Color {
    return when (channel) {
        HslChannel.RED -> HslRed
        HslChannel.ORANGE -> HslOrange
        HslChannel.YELLOW -> HslYellow
        HslChannel.GREEN -> HslGreen
        HslChannel.CYAN -> HslCyan
        HslChannel.BLUE -> HslBlue
        HslChannel.PURPLE -> HslPurple
        HslChannel.MAGENTA -> HslMagenta
    }
}
