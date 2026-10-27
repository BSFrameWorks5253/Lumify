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
import com.lumify.editor.core.MaskType
import com.lumify.editor.core.SelectiveMask

import com.lumify.editor.core.WatermarkConfig
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

data class EditorUiState(
    val originalBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val metadata: ImageMetadata? = null,
    val editState: EditState = EditState(),
    val activeTab: EditorTab = EditorTab.LIGHT,
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
            delay(16) // ~60fps debounce for ultra responsive touch scrubbing
            val processed = ImageProcessor.processImage(source, state)
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
                val finalBitmap = ImageProcessor.processImage(source, _uiState.value.editState, isExport = true)
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
