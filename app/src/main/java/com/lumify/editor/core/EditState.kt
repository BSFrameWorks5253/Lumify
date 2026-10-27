package com.lumify.editor.core

enum class HslChannel(val displayName: String, val baseHue: Float) {
    RED("Red", 0f),
    ORANGE("Orange", 30f),
    YELLOW("Yellow", 60f),
    GREEN("Green", 120f),
    CYAN("Cyan", 180f),
    BLUE("Blue", 240f),
    PURPLE("Purple", 280f),
    MAGENTA("Magenta", 320f)
}

data class HslAdjustment(
    val hueShift: Float = 0f,       // -100 to +100
    val saturation: Float = 0f,     // -100 to +100
    val luminance: Float = 0f       // -100 to +100
) {
    val isDefault: Boolean
        get() = hueShift == 0f && saturation == 0f && luminance == 0f
}

data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

enum class AspectRatio(val label: String, val ratio: Float?) {
    ORIGINAL("Original", null),
    FREE("Free", null),
    SQUARE("1:1", 1f),
    PORTRAIT_4_5("4:5", 4f / 5f),
    LANDSCAPE_16_9("16:9", 16f / 9f),
    STORY_9_16("9:16", 9f / 16f),
    PHOTO_3_2("3:2", 3f / 2f),
    PHOTO_4_3("4:3", 4f / 3f)
}

data class EditState(
    // Tone & Light
    val exposure: Float = 0f,       // -100 to 100
    val contrast: Float = 0f,       // -100 to 100
    val highlights: Float = 0f,     // -100 to 100
    val shadows: Float = 0f,        // -100 to 100
    val whites: Float = 0f,         // -100 to 100
    val blacks: Float = 0f,         // -100 to 100

    // Color & White Balance
    val temperature: Float = 0f,    // -100 (Cool) to +100 (Warm)
    val tint: Float = 0f,           // -100 (Green) to +100 (Magenta)
    val vibrance: Float = 0f,       // -100 to 100
    val saturation: Float = 0f,     // -100 to 100

    // Detail & Optics
    val clarity: Float = 0f,        // -100 to 100
    val sharpness: Float = 0f,      // 0 to 100
    val vignette: Float = 0f,       // 0 to 100

    // Geometry & Crop
    val rotationDegrees: Float = 0f, // 0 to 360
    val fineRotation: Float = 0f,    // -45 to +45
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRect: CropRect? = null,
    val selectedAspectRatio: AspectRatio = AspectRatio.ORIGINAL,

    // HSL 8-channel Color Grading
    val hslAdjustments: Map<HslChannel, HslAdjustment> = HslChannel.values().associateWith { HslAdjustment() },

    // Active Preset
    val appliedPresetId: String? = null
) {
    val isModified: Boolean
        get() = exposure != 0f || contrast != 0f || highlights != 0f || shadows != 0f ||
                whites != 0f || blacks != 0f || temperature != 0f || tint != 0f ||
                vibrance != 0f || saturation != 0f || clarity != 0f || sharpness != 0f ||
                vignette != 0f || rotationDegrees != 0f || fineRotation != 0f ||
                flipHorizontal || flipVertical || cropRect != null ||
                hslAdjustments.values.any { !it.isDefault } || appliedPresetId != null
}

class HistoryManager(initialState: EditState = EditState()) {
    private val undoStack = mutableListOf<EditState>()
    private val redoStack = mutableListOf<EditState>()
    var currentState: EditState = initialState
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun pushState(newState: EditState) {
        if (newState == currentState) return
        undoStack.add(currentState)
        if (undoStack.size > 50) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        currentState = newState
    }

    fun undo(): EditState? {
        if (!canUndo) return null
        redoStack.add(currentState)
        currentState = undoStack.removeAt(undoStack.lastIndex)
        return currentState
    }

    fun redo(): EditState? {
        if (!canRedo) return null
        undoStack.add(currentState)
        currentState = redoStack.removeAt(redoStack.lastIndex)
        return currentState
    }

    fun reset(): EditState {
        undoStack.add(currentState)
        redoStack.clear()
        currentState = EditState()
        return currentState
    }
}
