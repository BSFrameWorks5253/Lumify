package com.lumify.editor.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.CropRect
import com.lumify.editor.core.EditState
import com.lumify.editor.gpu.GpuRenderEngine
import com.lumify.editor.presets.FilterPreset
import com.lumify.editor.presets.PresetRepository
import com.lumify.editor.ui.components.AmbientLiquidBackdrop
import com.lumify.editor.ui.components.ColorWheelsView
import com.lumify.editor.ui.components.CropOverlay
import com.lumify.editor.ui.components.DoubleExposureOverlay
import com.lumify.editor.ui.components.HealingOverlay
import com.lumify.editor.ui.components.HistogramView
import com.lumify.editor.ui.components.LiquidGlassButton
import com.lumify.editor.ui.components.LiquidGlassSurface
import com.lumify.editor.ui.components.MaskOverlay
import com.lumify.editor.ui.components.PixelLoupe
import com.lumify.editor.ui.components.QrPresetImportDialog
import com.lumify.editor.ui.components.QrPresetShareDialog
import com.lumify.editor.ui.components.SelectiveUPointOverlay
import com.lumify.editor.ui.components.SnapseedBottomToolBar
import com.lumify.editor.ui.components.SnapseedExportSheet
import com.lumify.editor.ui.components.SnapseedFloatingParamSelector
import com.lumify.editor.ui.components.SnapseedToolsSheet
import com.lumify.editor.ui.components.SnapseedTopHud
import com.lumify.editor.ui.components.SnapseedViewEditsDialog
import com.lumify.editor.ui.components.SplitComparisonView
import com.lumify.editor.ui.components.ToneCurveView
import com.lumify.editor.ui.components.WatermarkDialog
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.AccentEmerald
import com.lumify.editor.ui.theme.AccentPurple
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.DarkSurface
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary
import kotlin.math.abs

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

    LaunchedEffect(Unit) {
        viewModel.loadOfflinePresets(context)
    }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
    var touchNormPos by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var touchScreenPos by remember { mutableStateOf(Offset(200f, 200f)) }

    // Gesture tracking for in-tool mode
    var isDraggingGesture by remember { mutableStateOf(false) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var showFloatingParamSelector by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showParamsBottomSheet by remember { mutableStateOf(false) }

    AmbientLiquidBackdrop(enableDrift = false) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val screenHeight = maxHeight

            Box(modifier = Modifier.fillMaxSize()) {

                // 1. Image Canvas & Interactive Viewport
                val displayBitmap = when {
                    uiState.isComparing || uiState.isComparingTool -> uiState.originalBitmap
                    else -> uiState.previewBitmap
                }
                val original = uiState.originalBitmap

                if (displayBitmap != null && original != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .onSizeChanged { canvasSize = it }
                            .then(
                                // If inside a tool with parameters, handle Snapseed drag gestures!
                                if (uiState.activeTool != null && uiState.activeTool?.parameters?.isNotEmpty() == true) {
                                    Modifier.pointerInput(uiState.activeTool) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDraggingGesture = true
                                                totalDragY = 0f
                                                showFloatingParamSelector = false
                                            },
                                            onDragEnd = {
                                                isDraggingGesture = false
                                                showFloatingParamSelector = false
                                                viewModel.onSliderDragFinished()
                                            },
                                            onDragCancel = {
                                                isDraggingGesture = false
                                                showFloatingParamSelector = false
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val deltaX = dragAmount.x
                                                val deltaY = dragAmount.y

                                                totalDragY += deltaY

                                                // Vertical threshold: switches parameters up or down!
                                                if (abs(totalDragY) > 55f) {
                                                    showFloatingParamSelector = true
                                                    if (totalDragY < -55f) {
                                                        viewModel.prevParam()
                                                        totalDragY = 0f
                                                    } else if (totalDragY > 55f) {
                                                        viewModel.nextParam()
                                                        totalDragY = 0f
                                                    }
                                                }

                                                // Horizontal drag: adjusts parameter value smoothly!
                                                if (abs(deltaX) > 1.2f) {
                                                    viewModel.adjustActiveParameter(deltaX * 0.45f)
                                                }
                                            }
                                        )
                                    }
                                } else if (uiState.isEyedropperActive) {
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
                                    // Pinch-to-zoom and pan + Press-and-hold canvas to compare before/after!
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                viewModel.toggleComparison(true)
                                                tryAwaitRelease()
                                                viewModel.toggleComparison(false)
                                            }
                                        )
                                    }.pointerInput(Unit) {
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

                        // Split Comparison View (Crazy Feature)
                        if (uiState.showSplitCompare) {
                            SplitComparisonView(
                                originalBitmap = original,
                                editedBitmap = displayBitmap,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Crop Tool Overlay
                        if (uiState.activeTool == SnapseedTool.CROP) {
                            CropOverlay(
                                cropRect = uiState.editState.cropRect ?: CropRect(),
                                selectedRatio = uiState.editState.selectedAspectRatio,
                                onCropChange = { newCrop ->
                                    viewModel.updateEditState { it.copy(cropRect = newCrop) }
                                },
                                onRatioSelect = { newRatio ->
                                    viewModel.setAspectRatio(newRatio)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Selective Masking Overlay
                        if (uiState.activeTool == SnapseedTool.SELECTIVE_MASKS && uiState.editState.activeMask != null) {
                            MaskOverlay(
                                mask = uiState.editState.activeMask!!,
                                onMaskChange = { updatedMask ->
                                    viewModel.updateActiveMask(updatedMask)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Snapseed Selective (U-Point) Control Points Overlay
                        if (uiState.activeTool == SnapseedTool.SELECTIVE) {
                            SelectiveUPointOverlay(
                                points = uiState.editState.selectivePoints,
                                selectedIndex = uiState.editState.activeSelectivePointIndex,
                                containerSize = canvasSize,
                                onAddPoint = { nx, ny -> viewModel.addSelectivePoint(nx, ny) },
                                onSelectPoint = { viewModel.selectSelectivePoint(it) },
                                onUpdatePoint = { idx, tf -> viewModel.updateSelectivePoint(idx, tf) },
                                onDeletePoint = { viewModel.deleteSelectivePoint(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Healing / Spot Removal Overlay
                        if (uiState.activeTool == SnapseedTool.HEALING) {
                            HealingOverlay(
                                strokes = uiState.editState.healingStrokes,
                                onAddStroke = { viewModel.addHealingStroke(it) },
                                onUndoStroke = { viewModel.undoHealingStroke() },
                                onClearStrokes = { viewModel.clearHealingStrokes() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Double Exposure Overlay
                        if (uiState.activeTool == SnapseedTool.DOUBLE_EXPOSURE) {
                            DoubleExposureOverlay(
                                state = uiState.editState.doubleExposure,
                                onStateChange = { viewModel.setDoubleExposureState(it) },
                                onPickImage = { viewModel.setDoubleExposureSecondaryImage(context, it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // 8x Pixel Loupe
                        if (uiState.isLoupeActive) {
                            PixelLoupe(
                                bitmap = displayBitmap,
                                touchPointNorm = uiState.loupeTouchNorm,
                                screenTouchOffset = uiState.loupeTouchOffset
                            )
                        }

                        // Floating In-Tool Parameter Selector (when swiping vertically)
                        if (uiState.activeTool != null && uiState.activeTool?.parameters?.isNotEmpty() == true) {
                            SnapseedFloatingParamSelector(
                                parameters = uiState.activeTool!!.parameters,
                                selectedIndex = uiState.activeParamIndex,
                                visible = showFloatingParamSelector,
                                onSelectParam = {
                                    viewModel.setParamIndex(it)
                                    showFloatingParamSelector = false
                                },
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 40.dp)
                            )
                        }

                        // "BEFORE / ORIGINAL" Floating Badge when holding to compare
                        if (uiState.isComparing || uiState.isComparingTool) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .statusBarsPadding()
                                    .padding(top = 16.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBackground.copy(alpha = 0.85f))
                                    .border(1.dp, AccentCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "ORIGINAL",
                                    color = AccentCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                }

                // 2. Processing Spinner
                if (uiState.isProcessing) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(DarkBackground.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // 3. Top Bars
                if (uiState.activeTool != null) {
                    // IN-TOOL TOP HUD (Snapseed Style)
                    val activeParam = uiState.activeTool!!.parameters.getOrNull(uiState.activeParamIndex)
                    SnapseedTopHud(
                        activeParam = activeParam,
                        currentValue = uiState.currentHudValue,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                } else {
                    // MAIN EDITOR TOP BAR (Snapseed Style with Liquid Glass)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // "OPEN" Button on the left
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(AccentCyan.copy(alpha = 0.15f))
                                        .clickable { onNavigateBack() }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = "Open Photo",
                                        tint = AccentCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "OPEN",
                                        color = AccentCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                // Right action items: Stacks (View Edits), Info, Split Compare, More Menu
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Undo / Redo
                                    IconButton(
                                        onClick = { viewModel.undo() },
                                        enabled = uiState.canUndo,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo",
                                            tint = if (uiState.canUndo) TextPrimary else TextMuted.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.redo() },
                                        enabled = uiState.canRedo,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Redo,
                                            contentDescription = "Redo",
                                            tint = if (uiState.canRedo) TextPrimary else TextMuted.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Stacks / View Edits icon (Snapseed hallmark)
                                    IconButton(
                                        onClick = { viewModel.setShowViewEditsDialog(true) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = "View Edits",
                                            tint = if (uiState.editStack.isNotEmpty()) AccentCyan else TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Info EXIF & Histogram icon
                                    IconButton(
                                        onClick = { viewModel.setShowExifDialog(true) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Image Info",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Overflow Menu
                                    Box {
                                        IconButton(
                                            onClick = { showOverflowMenu = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Menu",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showOverflowMenu,
                                            onDismissRequest = { showOverflowMenu = false },
                                            modifier = Modifier.background(DarkSurfaceElevated)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Split View Comparison", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.Compare, null, tint = AccentCyan) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.toggleSplitComparison()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("8x Loupe Inspector", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.ZoomIn, null, tint = AccentCyan) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.toggleLoupe()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Save Current Look Offline", color = AccentCyan) },
                                                leadingIcon = { Icon(Icons.Default.BookmarkAdd, null, tint = AccentCyan) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.setShowCreatePresetDialog(true)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Share Look as QR Code", color = AccentCyan) },
                                                leadingIcon = { Icon(Icons.Default.QrCode2, null, tint = AccentCyan) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.openQrShare("Custom Look")
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Import Look (QR / .lumify)", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.FileDownload, null, tint = AccentCyan) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.openQrImport()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Copy Edit Recipe", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = TextPrimary) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.copyRecipe()
                                                }
                                            )
                                            if (uiState.copiedRecipe != null) {
                                                DropdownMenuItem(
                                                    text = { Text("Paste Edit Recipe", color = AccentCyan) },
                                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = AccentCyan) },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        viewModel.pasteRecipe()
                                                    }
                                                )
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Revert All Edits", color = TextSecondary) },
                                                leadingIcon = { Icon(Icons.Default.Refresh, null, tint = TextSecondary) },
                                                onClick = {
                                                    showOverflowMenu = false
                                                    viewModel.resetEdits()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. In-Tool Specific Overlays (Curves, Color Wheels, etc.)
                if (uiState.activeTool == SnapseedTool.CURVES) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    ) {
                        ToneCurveView(
                            curves = uiState.editState.toneCurves,
                            onCurvesChange = { newCurves ->
                                viewModel.updateEditState { it.copy(toneCurves = newCurves) }
                            },
                            canvasDimension = 220.dp
                        )
                    }
                } else if (uiState.activeTool == SnapseedTool.COLOR_WHEELS) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 80.dp)
                    ) {
                        ColorWheelsView(
                            colorGrading = uiState.editState.colorGrading,
                            onColorGradingChange = { newGrading ->
                                viewModel.updateEditState { it.copy(colorGrading = newGrading) }
                            }
                        )
                    }
                }

                // 5. Bottom Navigation & Action Bars
                if (uiState.activeTool != null) {
                    // IN-TOOL ACTION BAR: ✕ (Cancel) | ☰ (Params) | ◫ (Compare) | ✓ (Apply)
                    SnapseedBottomToolBar(
                        onCancel = { viewModel.cancelSnapseedTool() },
                        onOpenParamList = { showParamsBottomSheet = true },
                        onCompareToggle = { viewModel.toggleToolComparison(it) },
                        onApply = { viewModel.applySnapseedTool() },
                        hasParameters = uiState.activeTool!!.parameters.isNotEmpty(),
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                } else {
                    // MAIN SNAPSEED 3-TAB BOTTOM BAR: [LOOKS] [TOOLS] [EXPORT]
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        // If LOOKS tab is selected, show horizontal presets carousel!
                        if (uiState.activeSnapseedTab == SnapseedTab.LOOKS) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. "+ Save Look" Offline Preset Creator Card
                                item {
                                    Column(
                                        modifier = Modifier
                                            .width(76.dp)
                                            .clickable { viewModel.setShowCreatePresetDialog(true) },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(AccentCyan.copy(alpha = 0.15f))
                                                .border(1.dp, AccentCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Save Custom Preset",
                                                tint = AccentCyan,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "+ Save Look",
                                            color = AccentCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 2. "Import QR" Offline Recipe Card
                                item {
                                    Column(
                                        modifier = Modifier
                                            .width(76.dp)
                                            .clickable { viewModel.openQrImport() },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = "Import Preset QR",
                                                tint = TextPrimary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = "Import QR",
                                            color = TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 2. Custom Offline Presets Saved by User
                                items(uiState.customPresets) { customPreset ->
                                    val isSelected = (uiState.editState.appliedPresetId == customPreset.id)
                                    SnapseedLookCard(
                                        preset = customPreset,
                                        isSelected = isSelected,
                                        isCustom = true,
                                        onShareQr = {
                                            viewModel.openQrShare(customPreset.name)
                                        },
                                        onDelete = {
                                            viewModel.deleteOfflinePreset(context, customPreset.id)
                                        },
                                        onClick = {
                                            viewModel.applyPreset(customPreset)
                                        }
                                    )
                                }

                                // 3. Built-in Snapseed & Crazy Presets
                                items(PresetRepository.allPresets) { preset ->
                                    val isSelected = (uiState.editState.appliedPresetId == preset.id)
                                    SnapseedLookCard(
                                        preset = preset,
                                        isSelected = isSelected,
                                        onShareQr = {
                                            viewModel.openQrShare(preset.name)
                                        },
                                        onClick = {
                                            viewModel.applyPreset(preset)
                                        }
                                    )
                                }
                            }
                        }

                        // Bottom 3 Primary Tabs Floating Liquid Glass Pill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LiquidGlassSurface(
                                shape = RoundedCornerShape(26.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SnapseedTabItem(
                                        label = "LOOKS",
                                        isSelected = (uiState.activeSnapseedTab == SnapseedTab.LOOKS),
                                        onClick = { viewModel.selectSnapseedTab(SnapseedTab.LOOKS) }
                                    )

                                    SnapseedTabItem(
                                        label = "TOOLS",
                                        isSelected = (uiState.activeSnapseedTab == SnapseedTab.TOOLS),
                                        onClick = { viewModel.selectSnapseedTab(SnapseedTab.TOOLS) }
                                    )

                                    SnapseedTabItem(
                                        label = "EXPORT",
                                        isSelected = (uiState.activeSnapseedTab == SnapseedTab.EXPORT),
                                        onClick = { viewModel.selectSnapseedTab(SnapseedTab.EXPORT) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 6. Tools Sheet Modal
                if (uiState.showToolsSheet) {
                    SnapseedToolsSheet(
                        onDismiss = { viewModel.setShowToolsSheet(false) },
                        onSelectTool = { tool ->
                            viewModel.openSnapseedTool(tool)
                        }
                    )
                }

                // 7. Export Sheet Modal
                if (uiState.showExportSheet) {
                    SnapseedExportSheet(
                        onDismiss = { viewModel.setShowExportSheet(false) },
                        onShare = {
                            val bmp = uiState.previewBitmap ?: return@SnapseedExportSheet
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_TEXT, "Edited with Snapseed by Burhanuddin")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share with"))
                        },
                        onSaveCopy = {
                            viewModel.exportImage(context, ExportFormat.JPEG, 95, true)
                        },
                        onOpenExportDialog = {
                            viewModel.setShowExportDialog(true)
                        },
                        onExportWithSignature = {
                            viewModel.updateEditState {
                                it.copy(watermark = it.watermark.copy(enabled = true, text = "© Burhanuddin Photography"))
                            }
                            viewModel.exportImage(context, ExportFormat.JPEG, 98, true)
                        }
                    )
                }

                // 8. View Edits Dialog (Stacks)
                if (uiState.showViewEditsDialog) {
                    SnapseedViewEditsDialog(
                        editStack = uiState.editStack,
                        onDismiss = { viewModel.setShowViewEditsDialog(false) },
                        onDeleteLayer = { viewModel.removeStackLayer(it) },
                        onRevertAll = { viewModel.resetEdits() }
                    )
                }

                // 9. Info & EXIF Dialog
                if (uiState.showExifDialog) {
                    AlertDialog(
                        onDismissRequest = { viewModel.setShowExifDialog(false) },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = AccentCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Photo Information & EXIF", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        },
                        text = {
                            Column {
                                uiState.metadata?.let { meta ->
                                    Text("Camera: ${meta.cameraMake} ${meta.cameraModel}", color = TextPrimary, fontSize = 13.sp)
                                    Text("Lens: ${meta.lensModel}", color = TextSecondary, fontSize = 12.sp)
                                    Text("Focal Length: ${meta.focalLength} • ${meta.aperture}", color = TextSecondary, fontSize = 12.sp)
                                    Text("ISO: ${meta.iso} • Shutter: ${meta.shutterSpeed}", color = TextSecondary, fontSize = 12.sp)
                                    Text("Resolution: ${meta.width} × ${meta.height} (${meta.megapixels})", color = TextSecondary, fontSize = 12.sp)
                                    Text("Format: ${meta.format}", color = AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } ?: Text("No EXIF data found in image.", color = TextMuted)

                                Spacer(modifier = Modifier.height(14.dp))

                                uiState.histogramData?.let { hist ->
                                    Text("RGB & Luminance Waveform", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HistogramView(hist, modifier = Modifier.fillMaxWidth().height(60.dp))
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { viewModel.setShowExifDialog(false) }) {
                                Text("Close", color = AccentCyan, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = DarkSurfaceElevated
                    )
                }

                // In-Tool Parameter Selection Dialog
                if (showParamsBottomSheet && uiState.activeTool?.parameters?.isNotEmpty() == true) {
                    AlertDialog(
                        onDismissRequest = { showParamsBottomSheet = false },
                        title = {
                            Text("${uiState.activeTool!!.title} Parameters", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.activeTool!!.parameters.forEachIndexed { index, param ->
                                    val isSelected = (index == uiState.activeParamIndex)
                                    val value = kotlin.math.round(param.getValue(uiState.editState)).toInt()
                                    val valStr = if (value > 0) "+$value" else "$value"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) AccentCyan.copy(alpha = 0.2f) else DarkSurface)
                                            .clickable {
                                                viewModel.setParamIndex(index)
                                                showParamsBottomSheet = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = param.name,
                                            color = if (isSelected) AccentCyan else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = valStr,
                                            color = if (isSelected) AccentCyan else TextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showParamsBottomSheet = false }) {
                                Text("Done", color = AccentCyan, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = DarkSurfaceElevated
                    )
                }

                // 10. Watermark & Export Dialog
                if (uiState.showExportDialog) {
                    ExportDialog(
                        onDismiss = { viewModel.setShowExportDialog(false) },
                        onConfirmExport = { format, quality, preserveExif ->
                            viewModel.exportImage(context, format, quality, preserveExif)
                        }
                    )
                }

                if (uiState.showWatermarkDialog) {
                    WatermarkDialog(
                        currentConfig = uiState.editState.watermark,
                        onDismiss = { viewModel.setShowWatermarkDialog(false) },
                        onSaveConfig = { newConfig ->
                            viewModel.updateWatermarkConfig(newConfig)
                        }
                    )
                }

                // 11. Offline Custom Preset Creation Dialog
                if (uiState.showCreatePresetDialog) {
                    var presetNameInput by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { viewModel.setShowCreatePresetDialog(false) },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BookmarkAdd, null, tint = AccentCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Look Offline", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        },
                        text = {
                            Column {
                                Text(
                                    "Save your current color grade, tone curves, ambiance, and photo adjustments as a 100% free offline custom preset.",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedTextField(
                                    value = presetNameInput,
                                    onValueChange = { presetNameInput = it },
                                    label = { Text("Preset Name") },
                                    placeholder = { Text("e.g. Moody Film Look") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = AccentCyan,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedLabelColor = AccentCyan
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (presetNameInput.isNotBlank()) {
                                        viewModel.createOfflinePreset(context, presetNameInput.trim())
                                    }
                                },
                                enabled = presetNameInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                            ) {
                                Text("Save Preset", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.setShowCreatePresetDialog(false) }) {
                                Text("Cancel", color = TextMuted)
                            }
                        },
                        containerColor = DarkSurfaceElevated
                    )
                }

                // 12. QR Code Preset Sharing Dialog
                if (uiState.showQrShareDialog) {
                    QrPresetShareDialog(
                        presetName = uiState.sharingPresetName,
                        editState = uiState.editState,
                        onDismiss = { viewModel.closeQrShare() }
                    )
                }

                // 13. QR Code / .lumify Preset Import Dialog
                if (uiState.showQrImportDialog) {
                    QrPresetImportDialog(
                        onDismiss = { viewModel.closeQrImport() },
                        onImportSuccess = { importedPreset ->
                            viewModel.importPreset(context, importedPreset)
                        }
                    )
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                )
            }
        }
    }
}

@Composable
fun SnapseedTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) AccentCyan else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun SnapseedLookCard(
    preset: FilterPreset,
    isSelected: Boolean,
    isCustom: Boolean = false,
    onShareQr: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isCrazy = (preset.category == "Burhanuddin Crazy")
    val borderColor = when {
        isSelected -> AccentCyan
        isCustom -> AccentEmerald.copy(alpha = 0.6f)
        isCrazy -> AccentPurple.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.12f)
    }

    Column(
        modifier = Modifier
            .width(76.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    when {
                        isCustom -> AccentEmerald.copy(alpha = 0.15f)
                        isCrazy -> AccentPurple.copy(alpha = 0.2f)
                        else -> DarkSurfaceElevated
                    }
                )
                .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isCustom -> Icons.Default.Bookmark
                    isCrazy -> Icons.Default.AutoAwesome
                    else -> Icons.Default.Palette
                },
                contentDescription = preset.name,
                tint = when {
                    isSelected -> AccentCyan
                    isCustom -> AccentEmerald
                    isCrazy -> AccentPurple
                    else -> TextSecondary
                },
                modifier = Modifier.size(24.dp)
            )

            // If QR share callback available, show small QR chip on top left
            if (onShareQr != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(3.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(DarkBackground.copy(alpha = 0.85f))
                        .clickable { onShareQr() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "Share Preset QR",
                        tint = AccentCyan,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // If custom offline preset, show small delete chip on top right
            if (isCustom && onDelete != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(DarkBackground.copy(alpha = 0.85f))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete Preset",
                        tint = TextMuted,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = preset.name,
            color = when {
                isSelected -> AccentCyan
                isCustom -> AccentEmerald
                isCrazy -> AccentPurple
                else -> TextPrimary
            },
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}
