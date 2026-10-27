package com.lumify.editor.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.Dp
import com.lumify.editor.ui.components.AmbientLiquidBackdrop
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.R
import com.lumify.editor.core.CropRect
import com.lumify.editor.core.HslAdjustment
import com.lumify.editor.core.HslChannel
import com.lumify.editor.core.MaskType
import com.lumify.editor.core.SelectiveMask
import com.lumify.editor.presets.PresetRepository
import com.lumify.editor.ui.components.AdjustSlider
import com.lumify.editor.ui.components.CropOverlay
import com.lumify.editor.ui.components.HistogramView
import android.os.Build
import androidx.compose.ui.graphics.asComposeRenderEffect
import com.lumify.editor.gpu.GpuRenderEngine
import com.lumify.editor.ui.components.ColorWheelsView
import com.lumify.editor.ui.components.LiquidGlassButton
import com.lumify.editor.ui.components.LiquidGlassSurface
import com.lumify.editor.ui.components.MaskOverlay
import com.lumify.editor.ui.components.PixelLoupe
import com.lumify.editor.ui.components.ToneCurveView
import com.lumify.editor.ui.components.WatermarkDialog
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
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var touchNormPos by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var touchScreenPos by remember { mutableStateOf(Offset(200f, 200f)) }

    AmbientLiquidBackdrop(enableDrift = false) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = maxHeight
            val controlPanelHeight = when {
                screenHeight < 620.dp -> 180.dp
                screenHeight < 780.dp -> 230.dp
                else -> 255.dp
            }
            val curveCanvasSize = when {
                screenHeight < 620.dp -> 150.dp
                screenHeight < 780.dp -> 175.dp
                else -> 195.dp
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
        // 1. Image Canvas & Interactive Viewport
        val displayBitmap = if (uiState.isComparing) uiState.originalBitmap else uiState.previewBitmap
        val original = uiState.originalBitmap

        if (displayBitmap != null && original != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .onSizeChanged { canvasSize = it }
                    .then(
                        if (uiState.isEyedropperActive) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures { tapPos ->
                                    val nx = (tapPos.x / size.width).coerceIn(0f, 1f)
                                    val ny = (tapPos.y / size.height).coerceIn(0f, 1f)
                                    viewModel.sampleEyedropper(nx, ny)
                                }
                            }
                        } else if (uiState.isLoupeActive) {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { startPos ->
                                        touchScreenPos = startPos
                                        touchNormPos = Offset(
                                            (startPos.x / size.width).coerceIn(0f, 1f),
                                            (startPos.y / size.height).coerceIn(0f, 1f)
                                        )
                                        viewModel.updateLoupePosition(touchScreenPos, touchNormPos)
                                    },
                                    onDrag = { change, _ ->
                                        touchScreenPos = change.position
                                        touchNormPos = Offset(
                                            (change.position.x / size.width).coerceIn(0f, 1f),
                                            (change.position.y / size.height).coerceIn(0f, 1f)
                                        )
                                        viewModel.updateLoupePosition(touchScreenPos, touchNormPos)
                                    }
                                )
                            }
                        } else {
                            Modifier.pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                    offset = if (scale > 1f) {
                                        Offset(offset.x + pan.x, offset.y + pan.y)
                                    } else {
                                        Offset.Zero
                                    }
                                }
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = "Active Image Canvas",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && GpuRenderEngine.isGpuAccelerationSupported) {
                                renderEffect = GpuRenderEngine.createGpuRenderEffect(
                                    uiState.editState,
                                    canvasSize.width.toFloat(),
                                    canvasSize.height.toFloat()
                                )?.asComposeRenderEffect()
                            }
                        }
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

                // Selective Mask Overlay when in Masks Tab
                if (uiState.activeTab == EditorTab.MASKS && uiState.editState.activeMask != null) {
                    MaskOverlay(
                        mask = uiState.editState.activeMask!!,
                        onMaskChange = { updated ->
                            viewModel.updateEditState { it.copy(activeMask = updated) }
                        }
                    )
                }

                // Eyedropper Live Sampler Aim Reticle
                if (uiState.isEyedropperActive) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width * touchNormPos.x, size.height * touchNormPos.y)
                        drawCircle(Color.Black, radius = 24.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
                        drawCircle(AccentCyan, radius = 22.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                        drawCircle(AccentCyan, radius = 3.dp.toPx(), center = center)
                    }
                }

                // 1:1 Pixel Loupe Floating Magnifier
                if (uiState.isLoupeActive) {
                    PixelLoupe(
                        bitmap = original,
                        touchPointNorm = touchNormPos,
                        screenTouchOffset = touchScreenPos
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
        }

        // Eyedropper / Loupe Guidance Banner
        if (uiState.isEyedropperActive || uiState.isLoupeActive) {
            LiquidGlassSurface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 64.dp)
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = DarkSurface.copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isEyedropperActive)
                            "Tap any neutral gray or white point in photo"
                        else
                            "Drag finger to check 1:1 tack-sharp focus & noise",
                        color = AccentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cancel",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            if (uiState.isEyedropperActive) viewModel.toggleEyedropper(false)
                            if (uiState.isLoupeActive) viewModel.toggleLoupe(false)
                        }
                    )
                }
            }
        }

        // 2. Top Liquid Glass Action Bar
        LiquidGlassSurface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = DarkSurface.copy(alpha = 0.85f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
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

                // Studio Precision Controls (Loupe, Eyedropper, Recipe, History)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1:1 Deep Zoom Pixel Loupe
                    IconButton(onClick = { viewModel.toggleLoupe() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_loupe),
                            contentDescription = "1:1 Pixel Loupe",
                            tint = if (uiState.isLoupeActive) AccentCyan else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // White Balance Eyedropper (Pipette)
                    IconButton(onClick = { viewModel.toggleEyedropper() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_eyedropper),
                            contentDescription = "Eyedropper",
                            tint = if (uiState.isEyedropperActive) AccentCyan else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Copy Edit Recipe
                    IconButton(onClick = { viewModel.copySettings() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy),
                            contentDescription = "Copy Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Paste Edit Recipe
                    IconButton(
                        onClick = { viewModel.pasteSettings() },
                        enabled = uiState.copiedRecipe != null
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_paste),
                            contentDescription = "Paste Settings",
                            tint = if (uiState.copiedRecipe != null) AccentCyan else TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Watermark Customization
                    IconButton(onClick = { viewModel.setShowWatermarkDialog(true) }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_watermark),
                            contentDescription = "Watermark",
                            tint = if (uiState.editState.watermark.enabled) AccentCyan else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Add to Batch Export Queue
                    IconButton(onClick = { viewModel.addToBatchQueue() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_batch_queue),
                            contentDescription = "Batch Queue",
                            tint = TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Undo
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) TextPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) TextPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Hold/Click to Compare
                    IconButton(
                        onClick = { viewModel.toggleComparison(!uiState.isComparing) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Compare",
                            tint = if (uiState.isComparing) AccentCyan else TextPrimary,
                            modifier = Modifier.size(20.dp)
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
                            tint = if (uiState.editState.isModified) AccentCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Export CTA Button
                LiquidGlassButton(
                    onClick = { viewModel.setShowExportDialog(true) },
                    modifier = Modifier.height(34.dp),
                    backgroundColor = AccentCyan.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export",
                            color = AccentCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
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
                .padding(top = 62.dp, end = 12.dp)
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
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            backgroundColor = DarkSurface.copy(alpha = 0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                // Adjustment Slider Area (Scrollable per active tab)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(controlPanelHeight)
                ) {
                    when (uiState.activeTab) {
                        EditorTab.LIGHT -> LightControls(viewModel, uiState)
                        EditorTab.COLOR -> ColorControls(viewModel, uiState)
                        EditorTab.CURVES -> CurvesControls(viewModel, uiState, curveCanvasSize)
                        EditorTab.GRADING -> ColorWheelsView(
                            colorGrading = uiState.editState.colorGrading,
                            onColorGradingChange = { updated ->
                                viewModel.updateEditState { it.copy(colorGrading = updated) }
                            }
                        )
                        EditorTab.HSL -> HslControls(viewModel, uiState)
                        EditorTab.MASKS -> MasksControls(viewModel, uiState)
                        EditorTab.OPTICS -> OpticsControls(viewModel, uiState)
                        EditorTab.DETAIL -> DetailControls(viewModel, uiState)
                        EditorTab.CROP -> CropControls(viewModel, uiState)
                        EditorTab.PRESETS -> PresetsControls(viewModel, uiState)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Pro Tab Navigation Ribbon (Scrollable for all 10 studio tools)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        EditorTabItem(
                            icon = rememberVectorPainter(Icons.Default.LightMode),
                            tab = EditorTab.LIGHT,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.LIGHT) }
                        )
                    }
                    item {
                        EditorTabItem(
                            icon = rememberVectorPainter(Icons.Default.Palette),
                            tab = EditorTab.COLOR,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.COLOR) }
                        )
                    }
                    item {
                        EditorTabItem(
                            icon = painterResource(id = R.drawable.ic_tone_curve),
                            tab = EditorTab.CURVES,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.CURVES) }
                        )
                    }
                    item {
                        EditorTabItem(
                            icon = painterResource(id = R.drawable.ic_color_wheels),
                            tab = EditorTab.GRADING,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.GRADING) }
                        )
                    }
                    item {
                        EditorTabItem(
                            icon = rememberVectorPainter(Icons.Default.InvertColors),
                            tab = EditorTab.HSL,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.HSL) }
                        )
                    }
                    item {
                        EditorTabItem(
                            icon = painterResource(id = R.drawable.ic_mask),
                            tab = EditorTab.MASKS,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.MASKS) }
                        )
                    }
                    item {
                        EditorTabItem(
                            icon = painterResource(id = R.drawable.ic_lens_correction),
                            tab = EditorTab.OPTICS,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.OPTICS) }
                        )
                    }
                    item {
                        EditorTabItem(
                            icon = rememberVectorPainter(Icons.Default.ZoomIn),
                            tab = EditorTab.DETAIL,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.DETAIL) }
                        )
                    }
                    item {
                        EditorTabItem(
                            icon = rememberVectorPainter(Icons.Default.Crop),
                            tab = EditorTab.CROP,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.CROP) }
                        )
                    }
                    item {
                        EditorTabItem(
                            icon = rememberVectorPainter(Icons.Default.AutoAwesome),
                            tab = EditorTab.PRESETS,
                            activeTab = uiState.activeTab,
                            onClick = { viewModel.setActiveTab(EditorTab.PRESETS) }
                        )
                    }
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

        // Photographer Watermark Customization Dialog
        if (uiState.showWatermarkDialog) {
            WatermarkDialog(
                currentConfig = uiState.editState.watermark,
                onDismiss = { viewModel.setShowWatermarkDialog(false) },
                onSaveConfig = { viewModel.updateWatermarkConfig(it) }
            )
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 85.dp)
        )
    }
    }
    }
}

@Composable
private fun EditorTabItem(
    icon: Painter,
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
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = tab.label,
            tint = if (isSelected) AccentCyan else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
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
        // Quick White Balance Eyedropper CTA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "White Balance",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            LiquidGlassButton(
                onClick = { viewModel.toggleEyedropper() },
                modifier = Modifier.height(30.dp),
                backgroundColor = if (uiState.isEyedropperActive) AccentCyan.copy(alpha = 0.35f) else DarkSurfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_eyedropper),
                        contentDescription = "Pipette",
                        tint = AccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isEyedropperActive) "Cancel Pipette" else "Tap Pipette",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        AdjustSlider(
            label = "Temperature (Cool / Warm)",
            value = edit.temperature,
            onValueChange = { viewModel.updateEditState { s -> s.copy(temperature = it) } }
        )
        AdjustSlider(
            label = "Tint (Green / Magenta)",
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
private fun CurvesControls(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    canvasDimension: Dp = 175.dp
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ToneCurveView(
            curves = uiState.editState.toneCurves,
            canvasDimension = canvasDimension,
            onCurvesChange = { updated ->
                viewModel.updateEditState { it.copy(toneCurves = updated) }
            }
        )
    }
}

@Composable
private fun MasksControls(viewModel: EditorViewModel, uiState: EditorUiState) {
    val scroll = rememberScrollState()
    val activeMask = uiState.editState.activeMask

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 16.dp)) {
        // Mask Type Selection Ribbon
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LiquidGlassButton(
                onClick = { viewModel.createOrUpdateMask(MaskType.LINEAR_GRADIENT) },
                modifier = Modifier.weight(1f).height(36.dp),
                backgroundColor = if (activeMask?.type == MaskType.LINEAR_GRADIENT) AccentCyan.copy(alpha = 0.25f) else DarkSurfaceElevated
            ) {
                Text(
                    text = "Linear Gradient",
                    color = if (activeMask?.type == MaskType.LINEAR_GRADIENT) AccentCyan else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            LiquidGlassButton(
                onClick = { viewModel.createOrUpdateMask(MaskType.RADIAL_GRADIENT) },
                modifier = Modifier.weight(1f).height(36.dp),
                backgroundColor = if (activeMask?.type == MaskType.RADIAL_GRADIENT) AccentCyan.copy(alpha = 0.25f) else DarkSurfaceElevated
            ) {
                Text(
                    text = "Radial Vignette",
                    color = if (activeMask?.type == MaskType.RADIAL_GRADIENT) AccentCyan else TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeMask != null) {
                LiquidGlassButton(
                    onClick = { viewModel.clearMask() },
                    modifier = Modifier.height(36.dp),
                    backgroundColor = Color.Red.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Clear",
                        color = Color(0xFFFF6B6B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }

        if (activeMask != null) {
            Text(
                text = "Adjust Mask Tone (${activeMask.type.label}):",
                color = AccentCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 2.dp)
            )

            AdjustSlider(
                label = "Mask Exposure",
                value = activeMask.exposure,
                onValueChange = { v ->
                    viewModel.updateEditState { it.copy(activeMask = activeMask.copy(exposure = v)) }
                }
            )
            AdjustSlider(
                label = "Mask Contrast",
                value = activeMask.contrast,
                onValueChange = { v ->
                    viewModel.updateEditState { it.copy(activeMask = activeMask.copy(contrast = v)) }
                }
            )
            AdjustSlider(
                label = "Mask Saturation",
                value = activeMask.saturation,
                onValueChange = { v ->
                    viewModel.updateEditState { it.copy(activeMask = activeMask.copy(saturation = v)) }
                }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select Linear Gradient (sky darkening) or Radial Vignette to apply selective local edits",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun OpticsControls(viewModel: EditorViewModel, uiState: EditorUiState) {
    val scroll = rememberScrollState()
    val edit = uiState.editState

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        AdjustSlider(
            label = "Lens Distortion (Barrel / Pincushion)",
            value = edit.lensDistortion,
            range = -100f..100f,
            defaultValue = 0f,
            onValueChange = { viewModel.updateEditState { s -> s.copy(lensDistortion = it) } }
        )
        AdjustSlider(
            label = "Defringe (Chromatic Aberration Removal)",
            value = edit.defringeAmount,
            range = 0f..100f,
            defaultValue = 0f,
            onValueChange = { viewModel.updateEditState { s -> s.copy(defringeAmount = it) } }
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
    val context = LocalContext.current
    val presets = PresetRepository.allPresets

    // File picker launcher for custom .cube 3D LUT
    val lutFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCubeLut(context, it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 3D LUT Action Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Studio Presets & 3D LUTs",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            LiquidGlassButton(
                onClick = { lutFileLauncher.launch("*/*") },
                modifier = Modifier.height(30.dp),
                backgroundColor = if (uiState.editState.customLut != null) AccentCyan.copy(alpha = 0.3f) else DarkSurfaceElevated
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cube_lut),
                        contentDescription = "Import LUT",
                        tint = AccentCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.editState.customLut != null) "LUT: ${uiState.editState.customLut?.title}" else "Import .cube LUT",
                        color = AccentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // If custom LUT loaded, show intensity slider
        if (uiState.editState.customLut != null) {
            AdjustSlider(
                label = "3D LUT Intensity",
                value = uiState.editState.lutIntensity,
                range = 0f..100f,
                defaultValue = 100f,
                onValueChange = { v ->
                    viewModel.updateEditState { it.copy(lutIntensity = v) }
                }
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
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
