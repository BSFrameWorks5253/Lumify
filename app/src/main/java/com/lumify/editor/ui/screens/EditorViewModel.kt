package com.lumify.editor.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumify.editor.core.AspectRatio
import com.lumify.editor.core.CropRect
import com.lumify.editor.core.EditState
import com.lumify.editor.core.HistogramData
import com.lumify.editor.core.HistoryManager
import com.lumify.editor.core.HslAdjustment
import com.lumify.editor.core.HslChannel
import com.lumify.editor.core.ImageMetadata
import com.lumify.editor.core.ImageProcessor
import com.lumify.editor.core.RawImageDecoder
import com.lumify.editor.presets.FilterPreset
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

import androidx.compose.ui.geometry.Offset
import com.lumify.editor.core.DoubleExposureState
import com.lumify.editor.core.HealingStroke
import com.lumify.editor.core.MaskType
import com.lumify.editor.core.SelectiveMask
import com.lumify.editor.core.SelectivePoint
import com.lumify.editor.core.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.lumify.editor.export.BatchQueueItem
import com.lumify.editor.export.BatchQueueManager
import com.lumify.editor.gpu.GpuRenderEngine

enum class EditorTab(val label: String) {
    LIGHT("Light"),
    COLOR("Color"),
    CURVES("Curves"),
    GRADING("Grading"),
    HSL("HSL"),
    MASKS("Masks"),
    OPTICS("Optics"),
    DETAIL("Detail"),
    CROP("Crop"),
    PRESETS("Presets")
}

enum class SnapseedTab(val label: String) {
    LOOKS("LOOKS"),
    TOOLS("TOOLS"),
    EXPORT("EXPORT")
}

data class EditorUiState(
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val metadata: ImageMetadata? = null,
    val editState: EditState = EditState(),
    val activeTab: EditorTab = EditorTab.LIGHT,
    val activeSnapseedTab: SnapseedTab = SnapseedTab.LOOKS,
    val activeTool: SnapseedTool? = null,
    val activeParamIndex: Int = 0,
    val toolSnapshotState: EditState? = null,
    val isComparingTool: Boolean = false,
    val showToolsSheet: Boolean = false,
    val showExportSheet: Boolean = false,
    val showViewEditsDialog: Boolean = false,
    val showExifDialog: Boolean = false,
    val showCrazyLabDialog: Boolean = false,
    val editStack: List<EditStackItem> = emptyList(),
    val showSplitCompare: Boolean = false,
    val splitComparePosition: Float = 0.5f,
    val showZebraFalseColor: Boolean = false,
    val currentHudValue: Float = 0f,
    val isDraggingGesture: Boolean = false,
    val activeHslChannel: HslChannel = HslChannel.RED,
    val isComparing: Boolean = false,
    val isProcessing: Boolean = false,
    val histogramData: HistogramData? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val showExportDialog: Boolean = false,
    val showWatermarkDialog: Boolean = false,
    val exportMessage: String? = null,
    val isEyedropperActive: Boolean = false,
    val isLoupeActive: Boolean = false,
    val loupeTouchOffset: Offset = Offset.Zero,
    val loupeTouchNorm: Offset = Offset(0.5f, 0.5f),
    val customPresets: List<FilterPreset> = emptyList(),
    val showCreatePresetDialog: Boolean = false,
    val showQrShareDialog: Boolean = false,
    val showQrImportDialog: Boolean = false,
    val sharingPresetName: String = "My Look",
    val copiedRecipe: EditState? = null,
    val isGpuAccelerationSupported: Boolean = GpuRenderEngine.isGpuAccelerationSupported
)

class EditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val historyManager = HistoryManager()
    private var processJob: Job? = null

    fun loadImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            try {
                val (bitmap, metadata) = RawImageDecoder.decodeImage(context, uri)
                historyManager.reset()
                _uiState.value = _uiState.value.copy(
                    originalBitmap = bitmap,
                    previewBitmap = bitmap,
                    metadata = metadata,
                    editState = EditState(),
                    isProcessing = false
                )
                updateHistogram(bitmap)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    exportMessage = "Error loading image: ${e.message}"
                )
            }
        }
    }

    fun setBitmapDirectly(bitmap: Bitmap, metadata: ImageMetadata) {
        historyManager.reset()
        _uiState.value = _uiState.value.copy(
            originalBitmap = bitmap,
            previewBitmap = bitmap,
            metadata = metadata,
            editState = EditState(),
            isProcessing = false
        )
        updateHistogram(bitmap)
    }

    fun setActiveTab(tab: EditorTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun setActiveHslChannel(channel: HslChannel) {
        _uiState.value = _uiState.value.copy(activeHslChannel = channel)
    }

    fun toggleComparison(showOriginal: Boolean) {
        _uiState.value = _uiState.value.copy(isComparing = showOriginal)
    }

    fun setShowExportDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExportDialog = show)
    }

    fun selectSnapseedTab(tab: SnapseedTab) {
        _uiState.value = _uiState.value.copy(
            activeSnapseedTab = tab,
            showToolsSheet = (tab == SnapseedTab.TOOLS),
            showExportSheet = (tab == SnapseedTab.EXPORT)
        )
    }

    fun openSnapseedTool(tool: SnapseedTool) {
        val currentEdit = _uiState.value.editState
        val initialParam = tool.parameters.firstOrNull()
        val initialVal = initialParam?.getValue?.invoke(currentEdit) ?: 0f

        _uiState.value = _uiState.value.copy(
            activeTool = tool,
            activeParamIndex = 0,
            toolSnapshotState = currentEdit,
            showToolsSheet = false,
            isComparingTool = false,
            currentHudValue = initialVal
        )
    }

    fun cancelSnapseedTool() {
        val snapshot = _uiState.value.toolSnapshotState ?: _uiState.value.editState
        _uiState.value = _uiState.value.copy(
            editState = snapshot,
            activeTool = null,
            toolSnapshotState = null,
            isComparingTool = false
        )
        scheduleProcessing(snapshot)
    }

    fun applySnapseedTool() {
        val tool = _uiState.value.activeTool ?: return
        val current = _uiState.value.editState
        historyManager.pushState(current, toolName = tool.title, summary = "Applied ${tool.title}")
        _uiState.value = _uiState.value.copy(
            activeTool = null,
            toolSnapshotState = null,
            isComparingTool = false,
            editStack = historyManager.currentEditStack
        )
        syncHistoryFlags()
    }

    fun setParamIndex(index: Int) {
        val tool = _uiState.value.activeTool ?: return
        if (index in tool.parameters.indices) {
            val param = tool.parameters[index]
            val value = param.getValue(_uiState.value.editState)
            _uiState.value = _uiState.value.copy(
                activeParamIndex = index,
                currentHudValue = value
            )
        }
    }

    fun nextParam() {
        val tool = _uiState.value.activeTool ?: return
        if (tool.parameters.isNotEmpty()) {
            val next = (_uiState.value.activeParamIndex + 1) % tool.parameters.size
            setParamIndex(next)
        }
    }

    fun prevParam() {
        val tool = _uiState.value.activeTool ?: return
        if (tool.parameters.isNotEmpty()) {
            val prev = (_uiState.value.activeParamIndex - 1 + tool.parameters.size) % tool.parameters.size
            setParamIndex(prev)
        }
    }

    fun adjustActiveParameter(delta: Float) {
        val tool = _uiState.value.activeTool ?: return
        val param = tool.parameters.getOrNull(_uiState.value.activeParamIndex) ?: return
        val currentVal = param.getValue(_uiState.value.editState)
        val newVal = (currentVal + delta).coerceIn(param.minValue, param.maxValue)
        val updatedState = param.setValue(_uiState.value.editState, newVal)
        _uiState.value = _uiState.value.copy(
            editState = updatedState,
            currentHudValue = newVal
        )
        scheduleProcessing(updatedState)
    }

    fun toggleToolComparison(showBefore: Boolean) {
        _uiState.value = _uiState.value.copy(isComparingTool = showBefore)
    }

    fun setShowToolsSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showToolsSheet = show)
        if (!show && _uiState.value.activeSnapseedTab == SnapseedTab.TOOLS) {
            _uiState.value = _uiState.value.copy(activeSnapseedTab = SnapseedTab.LOOKS)
        }
    }

    fun setShowExportSheet(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExportSheet = show)
        if (!show && _uiState.value.activeSnapseedTab == SnapseedTab.EXPORT) {
            _uiState.value = _uiState.value.copy(activeSnapseedTab = SnapseedTab.LOOKS)
        }
    }

    fun setShowViewEditsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showViewEditsDialog = show,
            editStack = historyManager.currentEditStack
        )
    }

    fun removeStackLayer(index: Int) {
        val newState = historyManager.removeStackLayer(index) ?: return
        _uiState.value = _uiState.value.copy(
            editState = newState,
            editStack = historyManager.currentEditStack
        )
        syncHistoryFlags()
        scheduleProcessing(newState)
    }

    fun setShowExifDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showExifDialog = show)
    }

    fun setShowCrazyLabDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCrazyLabDialog = show)
    }

    fun toggleSplitComparison() {
        _uiState.value = _uiState.value.copy(showSplitCompare = !_uiState.value.showSplitCompare)
    }

    fun setSplitPosition(pos: Float) {
        _uiState.value = _uiState.value.copy(splitComparePosition = pos.coerceIn(0.05f, 0.95f))
    }

    fun toggleZebraFalseColor() {
        _uiState.value = _uiState.value.copy(showZebraFalseColor = !_uiState.value.showZebraFalseColor)
    }

    fun loadOfflinePresets(context: Context) {
        val loaded = com.lumify.editor.presets.CustomPresetManager.loadCustomPresets(context)
        _uiState.value = _uiState.value.copy(customPresets = loaded)
    }

    fun setShowCreatePresetDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showCreatePresetDialog = show)
    }

    fun createOfflinePreset(context: Context, name: String, category: String = "My Presets") {
        val currentEdit = _uiState.value.editState
        val created = com.lumify.editor.presets.CustomPresetManager.savePreset(context, name, category, currentEdit)
        val updatedList = com.lumify.editor.presets.CustomPresetManager.loadCustomPresets(context)
        _uiState.value = _uiState.value.copy(
            customPresets = updatedList,
            showCreatePresetDialog = false,
            exportMessage = "Saved preset '$name' offline!"
        )
    }

    fun deleteOfflinePreset(context: Context, id: String) {
        com.lumify.editor.presets.CustomPresetManager.deletePreset(context, id)
        val updatedList = com.lumify.editor.presets.CustomPresetManager.loadCustomPresets(context)
        _uiState.value = _uiState.value.copy(
            customPresets = updatedList,
            exportMessage = "Preset deleted"
        )
    }

    var secondaryBitmap: Bitmap? = null
        private set

    // Snapseed Selective U-Point
    fun addSelectivePoint(normX: Float, normY: Float) {
        val currentPoints = _uiState.value.editState.selectivePoints.toMutableList()
        val newPoint = SelectivePoint(normX = normX, normY = normY)
        currentPoints.add(newPoint)
        val newIndex = currentPoints.lastIndex
        val newState = _uiState.value.editState.copy(
            selectivePoints = currentPoints,
            activeSelectivePointIndex = newIndex
        )
        _uiState.value = _uiState.value.copy(editState = newState)
        scheduleProcessing(newState)
    }

    fun selectSelectivePoint(index: Int) {
        val newState = _uiState.value.editState.copy(activeSelectivePointIndex = index)
        _uiState.value = _uiState.value.copy(editState = newState)
    }

    fun updateSelectivePoint(index: Int, transform: (SelectivePoint) -> SelectivePoint) {
        val currentPoints = _uiState.value.editState.selectivePoints.toMutableList()
        if (index in currentPoints.indices) {
            currentPoints[index] = transform(currentPoints[index])
            val newState = _uiState.value.editState.copy(selectivePoints = currentPoints)
            _uiState.value = _uiState.value.copy(editState = newState)
            scheduleProcessing(newState)
        }
    }

    fun deleteSelectivePoint(index: Int) {
        val currentPoints = _uiState.value.editState.selectivePoints.toMutableList()
        if (index in currentPoints.indices) {
            currentPoints.removeAt(index)
            val newIndex = if (currentPoints.isEmpty()) -1 else (index - 1).coerceAtLeast(0)
            val newState = _uiState.value.editState.copy(
                selectivePoints = currentPoints,
                activeSelectivePointIndex = newIndex
            )
            _uiState.value = _uiState.value.copy(editState = newState)
            scheduleProcessing(newState)
        }
    }

    // Healing / Spot Removal
    fun addHealingStroke(stroke: HealingStroke) {
        val currentStrokes = _uiState.value.editState.healingStrokes + stroke
        val newState = _uiState.value.editState.copy(healingStrokes = currentStrokes)
        _uiState.value = _uiState.value.copy(editState = newState)
        scheduleProcessing(newState)
    }

    fun undoHealingStroke() {
        val currentStrokes = _uiState.value.editState.healingStrokes
        if (currentStrokes.isNotEmpty()) {
            val newState = _uiState.value.editState.copy(healingStrokes = currentStrokes.dropLast(1))
            _uiState.value = _uiState.value.copy(editState = newState)
            scheduleProcessing(newState)
        }
    }

    fun clearHealingStrokes() {
        val newState = _uiState.value.editState.copy(healingStrokes = emptyList())
        _uiState.value = _uiState.value.copy(editState = newState)
        scheduleProcessing(newState)
    }

    // Double Exposure
    fun setDoubleExposureSecondaryImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bmp = android.graphics.BitmapFactory.decodeStream(stream)
                secondaryBitmap = bmp
                val newState = _uiState.value.editState.copy(
                    doubleExposure = _uiState.value.editState.doubleExposure.copy(secondaryImageUri = uri.toString())
                )
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(editState = newState)
                    scheduleProcessing(newState)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(exportMessage = "Failed to load blend image: ${e.message}")
            }
        }
    }

    fun setDoubleExposureState(deState: DoubleExposureState) {
        val newState = _uiState.value.editState.copy(doubleExposure = deState)
        _uiState.value = _uiState.value.copy(editState = newState)
        scheduleProcessing(newState)
    }

    // QR Preset Sharing & Importing
    fun openQrShare(presetName: String = "My Look") {
        _uiState.value = _uiState.value.copy(
            showQrShareDialog = true,
            sharingPresetName = presetName
        )
    }

    fun closeQrShare() {
        _uiState.value = _uiState.value.copy(showQrShareDialog = false)
    }

    fun openQrImport() {
        _uiState.value = _uiState.value.copy(showQrImportDialog = true)
    }

    fun closeQrImport() {
        _uiState.value = _uiState.value.copy(showQrImportDialog = false)
    }

    fun importPreset(context: Context, preset: FilterPreset) {
        val saved = com.lumify.editor.presets.CustomPresetManager.savePreset(
            context,
            preset.name,
            "Imported",
            preset.applyState(EditState())
        )
        loadOfflinePresets(context)
        applyPreset(saved)
        _uiState.value = _uiState.value.copy(
            showQrImportDialog = false,
            exportMessage = "Imported & applied preset '${preset.name}'"
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }

    fun updateEditState(transform: (EditState) -> EditState) {
        val newState = transform(_uiState.value.editState)
        _uiState.value = _uiState.value.copy(editState = newState)
        scheduleProcessing(newState)
    }

    fun onSliderDragFinished() {
        historyManager.pushState(_uiState.value.editState)
        syncHistoryFlags()
    }

    fun applyPreset(preset: FilterPreset) {
        val newState = preset.applyState(_uiState.value.editState)
        historyManager.pushState(newState)
        _uiState.value = _uiState.value.copy(editState = newState)
        syncHistoryFlags()
        scheduleProcessing(newState)
    }

    fun undo() {
        val previousState = historyManager.undo() ?: return
        _uiState.value = _uiState.value.copy(editState = previousState)
        syncHistoryFlags()
        scheduleProcessing(previousState)
    }

    fun redo() {
        val nextState = historyManager.redo() ?: return
        _uiState.value = _uiState.value.copy(editState = nextState)
        syncHistoryFlags()
        scheduleProcessing(nextState)
    }

    fun resetEdits() {
        val emptyState = historyManager.reset()
        _uiState.value = _uiState.value.copy(editState = emptyState)
        syncHistoryFlags()
        scheduleProcessing(emptyState)
    }

    fun updateHslAdjustment(channel: HslChannel, adjustment: HslAdjustment) {
        val currentHsl = _uiState.value.editState.hslAdjustments.toMutableMap()
        currentHsl[channel] = adjustment
        updateEditState { it.copy(hslAdjustments = currentHsl) }
    }

    fun setAspectRatio(ratio: AspectRatio) {
        val curCrop = _uiState.value.editState.cropRect ?: CropRect()
        val bmp = _uiState.value.originalBitmap ?: return

        val targetRatio = ratio.ratio
        val newCrop = if (targetRatio == null) {
            CropRect()
        } else {
            val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
            if (targetRatio > imgRatio) {
                val newH = imgRatio / targetRatio
                val top = (1f - newH) / 2f
                CropRect(left = 0f, top = top, right = 1f, bottom = top + newH)
            } else {
                val newW = targetRatio / imgRatio
                val left = (1f - newW) / 2f
                CropRect(left = left, top = 0f, right = left + newW, bottom = 1f)
            }
        }

        updateEditState {
            it.copy(cropRect = newCrop, selectedAspectRatio = ratio)
        }
        onSliderDragFinished()
    }

    fun rotate90() {
        updateEditState {
            it.copy(rotationDegrees = (it.rotationDegrees + 90f) % 360f)
        }
        onSliderDragFinished()
    }

    fun flipHorizontal() {
        updateEditState {
            it.copy(flipHorizontal = !it.flipHorizontal)
        }
        onSliderDragFinished()
    }

    // --- Phase 2: Eyedropper & Masking ---

    fun toggleEyedropper(active: Boolean? = null) {
        val next = active ?: !_uiState.value.isEyedropperActive
        _uiState.value = _uiState.value.copy(
            isEyedropperActive = next,
            isLoupeActive = if (next) false else _uiState.value.isLoupeActive
        )
    }

    fun sampleEyedropper(normX: Float, normY: Float) {
        val bmp = _uiState.value.originalBitmap ?: return
        val (dTemp, dTint) = ImageProcessor.sampleNeutralWhiteBalance(bmp, normX, normY)
        val current = _uiState.value.editState
        val next = current.copy(
            temperature = (current.temperature + dTemp).coerceIn(-100f, 100f),
            tint = (current.tint + dTint).coerceIn(-100f, 100f)
        )
        historyManager.pushState(next)
        _uiState.value = _uiState.value.copy(
            editState = next,
            isEyedropperActive = false,
            exportMessage = "White balance calibrated to neutral point"
        )
        scheduleProcessing(next)
        syncHistoryFlags()
    }

    fun createOrUpdateMask(type: MaskType) {
        val currentMask = _uiState.value.editState.activeMask
        val newMask = if (currentMask != null && currentMask.type == type) {
            currentMask
        } else {
            SelectiveMask(type = type)
        }
        updateEditState { it.copy(activeMask = newMask) }
    }

    fun updateActiveMask(mask: SelectiveMask) {
        updateEditState { it.copy(activeMask = mask) }
    }

    fun clearMask() {
        updateEditState { it.copy(activeMask = null) }
        onSliderDragFinished()
    }

    // --- Phase 3: 1:1 Pixel Loupe & Batch Preset Workflow ---

    fun toggleLoupe(active: Boolean? = null) {
        val next = active ?: !_uiState.value.isLoupeActive
        _uiState.value = _uiState.value.copy(
            isLoupeActive = next,
            isEyedropperActive = if (next) false else _uiState.value.isEyedropperActive
        )
    }

    fun updateLoupePosition(screenOffset: Offset, normOffset: Offset) {
        _uiState.value = _uiState.value.copy(
            loupeTouchOffset = screenOffset,
            loupeTouchNorm = normOffset
        )
    }

    fun copySettings() {
        val recipe = _uiState.value.editState.createRecipeCopy()
        _uiState.value = _uiState.value.copy(
            copiedRecipe = recipe,
            exportMessage = "Edit recipe copied to clipboard"
        )
    }

    fun copyRecipe() = copySettings()
    fun pasteRecipe() = pasteSettings()

    fun pasteSettings() {
        val recipe = _uiState.value.copiedRecipe
        if (recipe == null) {
            _uiState.value = _uiState.value.copy(exportMessage = "No recipe copied yet")
            return
        }
        val current = _uiState.value.editState
        val merged = current.copy(
            exposure = recipe.exposure,
            contrast = recipe.contrast,
            highlights = recipe.highlights,
            shadows = recipe.shadows,
            whites = recipe.whites,
            blacks = recipe.blacks,
            temperature = recipe.temperature,
            tint = recipe.tint,
            vibrance = recipe.vibrance,
            saturation = recipe.saturation,
            clarity = recipe.clarity,
            sharpness = recipe.sharpness,
            vignette = recipe.vignette,
            lensDistortion = recipe.lensDistortion,
            defringeAmount = recipe.defringeAmount,
            hslAdjustments = recipe.hslAdjustments,
            toneCurves = recipe.toneCurves,
            activeMask = recipe.activeMask,
            customLut = recipe.customLut,
            lutIntensity = recipe.lutIntensity
        )
        historyManager.pushState(merged)
        _uiState.value = _uiState.value.copy(
            editState = merged,
            exportMessage = "Settings applied successfully"
        )
        scheduleProcessing(merged)
        syncHistoryFlags()
    }

    fun importCubeLut(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val input = context.contentResolver.openInputStream(uri)
                if (input != null) {
                    val lut = ImageProcessor.parseCubeLut(input)
                    if (lut != null) {
                        updateEditState { it.copy(customLut = lut, lutIntensity = 100f) }
                        onSliderDragFinished()
                        _uiState.value = _uiState.value.copy(exportMessage = "3D LUT loaded: ${lut.title}")
                    } else {
                        _uiState.value = _uiState.value.copy(exportMessage = "Invalid or unsupported .cube LUT file")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(exportMessage = "Failed to load LUT: ${e.message}")
            }
        }
    }

    // --- Watermark & Batch Export Pipeline ---

    fun setShowWatermarkDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showWatermarkDialog = show)
    }

    fun updateWatermarkConfig(config: WatermarkConfig) {
        updateEditState { it.copy(watermark = config) }
        onSliderDragFinished()
    }

    fun addToBatchQueue(format: ExportFormat = ExportFormat.JPEG, quality: Int = 95) {
        val bmp = _uiState.value.originalBitmap ?: return
        val currentEdit = _uiState.value.editState
        val title = _uiState.value.metadata?.format ?: "Photo"

        BatchQueueManager.addItem(
            BatchQueueItem(
                title = title,
                bitmap = bmp,
                editState = currentEdit,
                format = format,
                quality = quality
            )
        )
        val count = BatchQueueManager.queue.value.size
        _uiState.value = _uiState.value.copy(
            exportMessage = "Added to Batch Queue ($count photos queued)"
        )
    }

    fun startBatchExport(context: Context) {
        if (BatchQueueManager.queue.value.isEmpty()) {
            _uiState.value = _uiState.value.copy(exportMessage = "Batch queue is empty. Add photos to queue first.")
            return
        }
        BatchQueueManager.startBatchExport(context)
        _uiState.value = _uiState.value.copy(
            exportMessage = "Batch export started in background notification"
        )
    }

    private fun scheduleProcessing(state: EditState) {
        val source = _uiState.value.originalBitmap ?: return
        processJob?.cancel()
        processJob = viewModelScope.launch {
            delay(12) // ultra responsive touch scrubbing
            val processed = ImageProcessor.processImage(source, state, secondaryBitmap = secondaryBitmap)
            _uiState.value = _uiState.value.copy(previewBitmap = processed)
            updateHistogram(processed)
        }
    }

    private fun updateHistogram(bitmap: Bitmap) {
        viewModelScope.launch {
            val hist = ImageProcessor.computeHistogram(bitmap)
            _uiState.value = _uiState.value.copy(histogramData = hist)
        }
    }

    private fun syncHistoryFlags() {
        _uiState.value = _uiState.value.copy(
            canUndo = historyManager.canUndo,
            canRedo = historyManager.canRedo
        )
    }

    fun exportImage(
        context: Context,
        format: ExportFormat,
        quality: Int,
        preserveExif: Boolean
    ) {
        viewModelScope.launch {
            val source = _uiState.value.originalBitmap ?: return@launch
            _uiState.value = _uiState.value.copy(isProcessing = true, showExportDialog = false)

            try {
                // Render full resolution
                val finalBitmap = ImageProcessor.processImage(
                    source,
                    _uiState.value.editState,
                    secondaryBitmap = secondaryBitmap,
                    isExport = true
                )
                val fileName = "LUMIFY_${System.currentTimeMillis()}.${format.extension.lowercase().substringBefore(' ')}"

                val success = saveBitmapToGallery(context, finalBitmap, fileName, format.compressFormat, quality)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    exportMessage = if (success) "Saved to Gallery: $fileName" else "Failed to save image."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    exportMessage = "Export failed: ${e.message}"
                )
            }
        }
    }

    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: Bitmap.CompressFormat,
        quality: Int
    ): Boolean {
        var outputStream: OutputStream? = null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Lumify")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return false
                outputStream = context.contentResolver.openOutputStream(uri)
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/Lumify"
                val file = File(imagesDir)
                if (!file.exists()) file.mkdirs()
                val image = File(imagesDir, filename)
                outputStream = FileOutputStream(image)
            }

            outputStream?.let {
                bitmap.compress(format, quality, it)
                it.flush()
                true
            } ?: false
        } catch (e: Exception) {
            false
        } finally {
            outputStream?.close()
        }
    }
}
