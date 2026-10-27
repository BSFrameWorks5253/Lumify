package com.lumify.editor.core

import java.util.UUID

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

data class ToneCurvePoint(val x: Float, val y: Float)

fun defaultCurvePoints(): List<ToneCurvePoint> = listOf(
    ToneCurvePoint(0.0f, 0.0f),
    ToneCurvePoint(0.25f, 0.25f),
    ToneCurvePoint(0.5f, 0.5f),
    ToneCurvePoint(0.75f, 0.75f),
    ToneCurvePoint(1.0f, 1.0f)
)

data class ToneCurves(
    val rgbPoints: List<ToneCurvePoint> = defaultCurvePoints(),
    val redPoints: List<ToneCurvePoint> = defaultCurvePoints(),
    val greenPoints: List<ToneCurvePoint> = defaultCurvePoints(),
    val bluePoints: List<ToneCurvePoint> = defaultCurvePoints()
) {
    val isDefault: Boolean
        get() = isCurveDefault(rgbPoints) && isCurveDefault(redPoints) &&
                isCurveDefault(greenPoints) && isCurveDefault(bluePoints)

    private fun isCurveDefault(points: List<ToneCurvePoint>): Boolean {
        if (points.size != 5) return false
        return points[0].y == 0.0f && points[1].y == 0.25f &&
               points[2].y == 0.5f && points[3].y == 0.75f && points[4].y == 1.0f
    }
}

enum class MaskType(val label: String) {
    LINEAR_GRADIENT("Linear Gradient"),
    RADIAL_GRADIENT("Radial Gradient")
}

data class SelectiveMask(
    val id: String = UUID.randomUUID().toString(),
    val type: MaskType = MaskType.LINEAR_GRADIENT,
    val startX: Float = 0.5f,
    val startY: Float = 0.15f,
    val endX: Float = 0.5f,
    val endY: Float = 0.65f,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.5f,
    val radiusX: Float = 0.35f,
    val radiusY: Float = 0.35f,
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f
) {
    val isModified: Boolean
        get() = exposure != 0f || contrast != 0f || saturation != 0f
}

data class ColorWheelChannel(
    val hue: Float = 0f,         // 0 to 360
    val saturation: Float = 0f,  // 0 to 100
    val luminance: Float = 0f    // -100 to 100
) {
    val isDefault: Boolean get() = saturation == 0f && luminance == 0f
}

data class ColorGradingState(
    val shadows: ColorWheelChannel = ColorWheelChannel(),
    val midtones: ColorWheelChannel = ColorWheelChannel(),
    val highlights: ColorWheelChannel = ColorWheelChannel(),
    val balance: Float = 0f,     // -100 to 100
    val blending: Float = 50f    // 0 to 100
) {
    val isDefault: Boolean
        get() = shadows.isDefault && midtones.isDefault && highlights.isDefault && balance == 0f && blending == 50f
}

enum class WatermarkType {
    TEXT,
    GRAPHIC
}

enum class WatermarkPosition(val label: String) {
    BOTTOM_RIGHT("Bottom Right"),
    BOTTOM_LEFT("Bottom Left"),
    TOP_RIGHT("Top Right"),
    TOP_LEFT("Top Left"),
    CENTER("Center")
}

data class WatermarkConfig(
    val enabled: Boolean = false,
    val type: WatermarkType = WatermarkType.TEXT,
    val text: String = "© Lumify Photography",
    val graphicUri: String? = null,
    val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val opacity: Float = 0.85f,   // 0.1 to 1.0
    val sizePercent: Float = 14f, // 5% to 40%
    val marginPercent: Float = 3f
)

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

    // Lens Correction & Defringing
    val lensDistortion: Float = 0f, // -100 to 100 (Barrel to Pincushion)
    val defringeAmount: Float = 0f, // 0 to 100 (Chromatic aberration removal)

    // Geometry & Crop
    val rotationDegrees: Float = 0f, // 0 to 360
    val fineRotation: Float = 0f,    // -45 to +45
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRect: CropRect? = null,
    val selectedAspectRatio: AspectRatio = AspectRatio.ORIGINAL,

    // HSL 8-channel Color Grading
    val hslAdjustments: Map<HslChannel, HslAdjustment> = HslChannel.values().associateWith { HslAdjustment() },

    // Interactive Tone Curves
    val toneCurves: ToneCurves = ToneCurves(),

    // Selective Masks
    val activeMask: SelectiveMask? = null,

    // 3-Way Color Wheels (Cinema Split Toning)
    val colorGrading: ColorGradingState = ColorGradingState(),

    // Photographer Watermark
    val watermark: WatermarkConfig = WatermarkConfig(),

    // Custom 3D LUT
    val customLut: Lut3D? = null,
    val lutIntensity: Float = 100f,

    // Active Preset
    val appliedPresetId: String? = null
) {
    val isModified: Boolean
        get() = exposure != 0f || contrast != 0f || highlights != 0f || shadows != 0f ||
                whites != 0f || blacks != 0f || temperature != 0f || tint != 0f ||
                vibrance != 0f || saturation != 0f || clarity != 0f || sharpness != 0f ||
                vignette != 0f || lensDistortion != 0f || defringeAmount != 0f ||
                rotationDegrees != 0f || fineRotation != 0f ||
                flipHorizontal || flipVertical || cropRect != null ||
                !toneCurves.isDefault || activeMask?.isModified == true ||
                !colorGrading.isDefault || watermark.enabled ||
                customLut != null ||
                hslAdjustments.values.any { !it.isDefault } || appliedPresetId != null

    /**
     * Creates an exportable copy/paste recipe containing all tonal and color adjustments
     * without geometry (so aspect ratio/rotation of different photos is preserved).
     */
    fun createRecipeCopy(): EditState {
        return this.copy(
            rotationDegrees = 0f,
            fineRotation = 0f,
            flipHorizontal = false,
            flipVertical = false,
            cropRect = null,
            selectedAspectRatio = AspectRatio.ORIGINAL
        )
    }
}

data class Lut3D(
    val title: String,
    val size: Int,
    val table: FloatArray // size * size * size * 3
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Lut3D) return false
        return title == other.title && size == other.size && table.contentEquals(other.table)
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + size
        result = 31 * result + table.contentHashCode()
        return result
    }
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
