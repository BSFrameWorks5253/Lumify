package com.lumify.editor.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flare
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vignette
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.lumify.editor.core.EditState

enum class ToolCategory(val title: String) {
    ESSENTIALS("Essentials"),
    TRANSFORM("Transform"),
    CREATIVE("Creative"),
    PHOTOSHOP_PRO("Photoshop PC Pro"),
    BURHANUDDIN_CRAZY("Burhanuddin's Crazy Lab")
}

data class ToolParameter(
    val id: String,
    val name: String,
    val minValue: Float = -100f,
    val maxValue: Float = 100f,
    val defaultValue: Float = 0f,
    val getValue: (EditState) -> Float,
    val setValue: (EditState, Float) -> EditState
)

enum class SnapseedTool(
    val title: String,
    val category: ToolCategory,
    val description: String,
    val icon: ImageVector,
    val parameters: List<ToolParameter> = emptyList()
) {
    TUNE_IMAGE(
        title = "Tune Image",
        category = ToolCategory.ESSENTIALS,
        description = "Adjust exposure and color automatically or manually with fine, precise control",
        icon = Icons.Default.Tune,
        parameters = listOf(
            ToolParameter("brightness", "Brightness", -100f, 100f, 0f, { it.exposure }, { s, v -> s.copy(exposure = v) }),
            ToolParameter("contrast", "Contrast", -100f, 100f, 0f, { it.contrast }, { s, v -> s.copy(contrast = v) }),
            ToolParameter("saturation", "Saturation", -100f, 100f, 0f, { it.saturation }, { s, v -> s.copy(saturation = v) }),
            ToolParameter("ambiance", "Ambiance", -100f, 100f, 0f, { it.ambiance }, { s, v -> s.copy(ambiance = v) }),
            ToolParameter("highlights", "Highlights", -100f, 100f, 0f, { it.highlights }, { s, v -> s.copy(highlights = v) }),
            ToolParameter("shadows", "Shadows", -100f, 100f, 0f, { it.shadows }, { s, v -> s.copy(shadows = v) }),
            ToolParameter("warmth", "Warmth", -100f, 100f, 0f, { it.temperature }, { s, v -> s.copy(temperature = v) })
        )
    ),

    DETAILS(
        title = "Details",
        category = ToolCategory.ESSENTIALS,
        description = "Magically bring out surface structures to make subjects pop",
        icon = Icons.Default.Texture,
        parameters = listOf(
            ToolParameter("structure", "Structure", -100f, 100f, 0f, { it.structure }, { s, v -> s.copy(structure = v) }),
            ToolParameter("sharpening", "Sharpening", 0f, 100f, 0f, { it.sharpness }, { s, v -> s.copy(sharpness = v) })
        )
    ),

    CURVES(
        title = "Curves",
        category = ToolCategory.ESSENTIALS,
        description = "Have precise control over the brightness and RGB color levels in your photos",
        icon = Icons.Default.ShowChart
    ),

    WHITE_BALANCE(
        title = "White Balance",
        category = ToolCategory.ESSENTIALS,
        description = "Adjust the colors so that the image looks more natural",
        icon = Icons.Default.Thermostat,
        parameters = listOf(
            ToolParameter("temperature", "Temperature", -100f, 100f, 0f, { it.temperature }, { s, v -> s.copy(temperature = v) }),
            ToolParameter("tint", "Tint", -100f, 100f, 0f, { it.tint }, { s, v -> s.copy(tint = v) })
        )
    ),

    SELECTIVE(
        title = "Selective",
        category = ToolCategory.ESSENTIALS,
        description = "The renown U-Point™ technology. Tap to drop control points to selectively adjust color and light",
        icon = Icons.Default.Adjust
    ),

    HEALING(
        title = "Healing",
        category = ToolCategory.ESSENTIALS,
        description = "Remove the uninvited neighbor, dust spots, or blemishes seamlessly",
        icon = Icons.Default.AutoFixHigh
    ),

    CROP(
        title = "Crop",
        category = ToolCategory.TRANSFORM,
        description = "Crop to standard aspect ratios or freely with grid alignment",
        icon = Icons.Default.Crop
    ),

    ROTATE(
        title = "Rotate",
        category = ToolCategory.TRANSFORM,
        description = "Rotate 90°, or straighten a skewed horizon",
        icon = Icons.Default.RotateRight,
        parameters = listOf(
            ToolParameter("straighten", "Straighten", -45f, 45f, 0f, { it.fineRotation }, { s, v -> s.copy(fineRotation = v) })
        )
    ),

    GLAMOUR_GLOW(
        title = "Glamour Glow",
        category = ToolCategory.CREATIVE,
        description = "Add a fine glow to images, great for fashion or portraits",
        icon = Icons.Default.Flare,
        parameters = listOf(
            ToolParameter("glow", "Glow", 0f, 100f, 0f, { it.glamourGlow }, { s, v -> s.copy(glamourGlow = v) }),
            ToolParameter("saturation", "Saturation", -100f, 100f, 0f, { it.saturation }, { s, v -> s.copy(saturation = v) }),
            ToolParameter("warmth", "Warmth", -100f, 100f, 0f, { it.temperature }, { s, v -> s.copy(temperature = v) })
        )
    ),

    DRAMA(
        title = "Drama",
        category = ToolCategory.CREATIVE,
        description = "Add a hint of doomsday to subtle artistic effect in your photos",
        icon = Icons.Default.AutoAwesome,
        parameters = listOf(
            ToolParameter("filter_strength", "Filter Strength", 0f, 100f, 0f, { it.drama }, { s, v -> s.copy(drama = v) }),
            ToolParameter("saturation", "Saturation", -100f, 100f, 0f, { it.saturation }, { s, v -> s.copy(saturation = v) })
        )
    ),

    VINTAGE(
        title = "Vintage",
        category = ToolCategory.CREATIVE,
        description = "The style of color film photos from the 50s, 60s or 70s",
        icon = Icons.Default.Movie,
        parameters = listOf(
            ToolParameter("brightness", "Brightness", -100f, 100f, 0f, { it.exposure }, { s, v -> s.copy(exposure = v) }),
            ToolParameter("saturation", "Saturation", -100f, 100f, 0f, { it.saturation }, { s, v -> s.copy(saturation = v) }),
            ToolParameter("style_strength", "Style Strength", 0f, 100f, 0f, { it.vintage }, { s, v -> s.copy(vintage = v) }),
            ToolParameter("vignette", "Vignette", 0f, 100f, 0f, { it.vignette }, { s, v -> s.copy(vignette = v) })
        )
    ),

    GRAINY_FILM(
        title = "Grainy Film",
        category = ToolCategory.CREATIVE,
        description = "Get modern film looks with realistic grain engine",
        icon = Icons.Default.Grain,
        parameters = listOf(
            ToolParameter("grain", "Grain", 0f, 100f, 0f, { it.grain }, { s, v -> s.copy(grain = v) }),
            ToolParameter("style_strength", "Style Strength", -100f, 100f, 0f, { it.contrast }, { s, v -> s.copy(contrast = v) })
        )
    ),

    RETROLUX(
        title = "Retrolux",
        category = ToolCategory.CREATIVE,
        description = "Go truly retro with light leaks, scratches, film styles",
        icon = Icons.Default.Lightbulb,
        parameters = listOf(
            ToolParameter("light_leak", "Light Leak", 0f, 100f, 0f, { it.retrolux }, { s, v -> s.copy(retrolux = v) }),
            ToolParameter("scratches", "Scratches", 0f, 100f, 0f, { it.grain }, { s, v -> s.copy(grain = v) }),
            ToolParameter("warmth", "Warmth", -100f, 100f, 0f, { it.temperature }, { s, v -> s.copy(temperature = v) })
        )
    ),

    BLACK_AND_WHITE(
        title = "Black & White",
        category = ToolCategory.CREATIVE,
        description = "Classic Black and White look straight out of the darkroom",
        icon = Icons.Default.Brightness6,
        parameters = listOf(
            ToolParameter("contrast", "Contrast", -100f, 100f, 0f, { it.contrast }, { s, v -> s.copy(contrast = v) }),
            ToolParameter("brightness", "Brightness", -100f, 100f, 0f, { it.exposure }, { s, v -> s.copy(exposure = v) }),
            ToolParameter("grain", "Grain", 0f, 100f, 0f, { it.grain }, { s, v -> s.copy(grain = v) })
        )
    ),

    NOIR(
        title = "Noir",
        category = ToolCategory.CREATIVE,
        description = "Cinematic Black and White film with realistic grain and matte wash",
        icon = Icons.Default.Movie,
        parameters = listOf(
            ToolParameter("wash", "Wash", 0f, 100f, 0f, { it.noir }, { s, v -> s.copy(noir = v) }),
            ToolParameter("contrast", "Contrast", -100f, 100f, 0f, { it.contrast }, { s, v -> s.copy(contrast = v) }),
            ToolParameter("grain", "Grain", 0f, 100f, 0f, { it.grain }, { s, v -> s.copy(grain = v) })
        )
    ),

    PORTRAIT(
        title = "Portrait",
        category = ToolCategory.CREATIVE,
        description = "Add focus to the eyes, face-specific lighting or smoothen skin",
        icon = Icons.Default.Face,
        parameters = listOf(
            ToolParameter("face_spotlight", "Face Spotlight", 0f, 100f, 0f, { it.portraitSpotlight }, { s, v -> s.copy(portraitSpotlight = v) }),
            ToolParameter("skin_smoothing", "Skin Smoothing", 0f, 100f, 0f, { it.portraitSmooth }, { s, v -> s.copy(portraitSmooth = v) }),
            ToolParameter("eye_clarity", "Eye Clarity", -100f, 100f, 0f, { it.clarity }, { s, v -> s.copy(clarity = v) })
        )
    ),

    LENS_BLUR(
        title = "Lens Blur",
        category = ToolCategory.CREATIVE,
        description = "Soften the background with beautiful bokeh effect",
        icon = Icons.Default.BlurOn,
        parameters = listOf(
            ToolParameter("blur_strength", "Blur Strength", 0f, 100f, 0f, { it.lensBlur }, { s, v -> s.copy(lensBlur = v) }),
            ToolParameter("vignette", "Vignette Strength", 0f, 100f, 0f, { it.vignette }, { s, v -> s.copy(vignette = v) })
        )
    ),

    SELECTIVE_MASKS(
        title = "Selective Masks",
        category = ToolCategory.CREATIVE,
        description = "Apply exposure and contrast gradient masks to specific regions",
        icon = Icons.Default.Timeline
    ),

    DOUBLE_EXPOSURE(
        title = "Double Exposure",
        category = ToolCategory.CREATIVE,
        description = "Blend two photos choosing from blend modes inspired by analog film techniques",
        icon = Icons.Default.Layers,
        parameters = listOf(
            ToolParameter("opacity", "Opacity", 0f, 100f, 60f, { it.doubleExposure.opacity * 100f }, { s, v -> s.copy(doubleExposure = s.doubleExposure.copy(opacity = v / 100f)) })
        )
    ),

    // --- Photoshop PC Pro Tools ---

    PHOTOSHOP_LEVELS(
        title = "Photoshop Levels",
        category = ToolCategory.PHOTOSHOP_PRO,
        description = "Adjust input black point, midtone gamma, and white clipping point",
        icon = Icons.Default.Timeline,
        parameters = listOf(
            ToolParameter("black_point", "Black Point", 0f, 100f, 0f, { it.levelsBlack }, { s, v -> s.copy(levelsBlack = v) }),
            ToolParameter("midtone_gamma", "Midtone Gamma", -100f, 100f, 0f, { it.levelsGamma }, { s, v -> s.copy(levelsGamma = v) }),
            ToolParameter("white_point", "White Point", 0f, 100f, 0f, { it.levelsWhite }, { s, v -> s.copy(levelsWhite = v) })
        )
    ),

    PHOTOSHOP_COLOR_BALANCE(
        title = "Color Balance",
        category = ToolCategory.PHOTOSHOP_PRO,
        description = "Photoshop PC color balance across cyan-red, magenta-green, and yellow-blue",
        icon = Icons.Default.ColorLens,
        parameters = listOf(
            ToolParameter("cyan_red", "Cyan - Red", -100f, 100f, 0f, { it.colorBalanceCyanRed }, { s, v -> s.copy(colorBalanceCyanRed = v) }),
            ToolParameter("magenta_green", "Magenta - Green", -100f, 100f, 0f, { it.colorBalanceMagentaGreen }, { s, v -> s.copy(colorBalanceMagentaGreen = v) }),
            ToolParameter("yellow_blue", "Yellow - Blue", -100f, 100f, 0f, { it.colorBalanceYellowBlue }, { s, v -> s.copy(colorBalanceYellowBlue = v) })
        )
    ),

    // --- Burhanuddin's Crazy Lab Features ---

    CYBERPUNK(
        title = "Cyberpunk Neon",
        category = ToolCategory.BURHANUDDIN_CRAZY,
        description = "Electric neon cyan shadows & hot magenta sci-fi atmospheric bloom",
        icon = Icons.Default.Flare,
        parameters = listOf(
            ToolParameter("neon_bloom", "Neon Bloom", 0f, 100f, 0f, { it.cyberpunkNeon }, { s, v -> s.copy(cyberpunkNeon = v) }),
            ToolParameter("contrast", "Contrast", -100f, 100f, 0f, { it.contrast }, { s, v -> s.copy(contrast = v) }),
            ToolParameter("saturation", "Saturation", -100f, 100f, 0f, { it.saturation }, { s, v -> s.copy(saturation = v) })
        )
    ),

    COLOR_WHEELS(
        title = "Cinema 3-Way",
        category = ToolCategory.BURHANUDDIN_CRAZY,
        description = "Hollywood lift/gamma/gain 3-way color grading wheels",
        icon = Icons.Default.Palette
    ),

    HSL_MASTER(
        title = "8-Channel HSL",
        category = ToolCategory.BURHANUDDIN_CRAZY,
        description = "Surgical color isolation and hue/saturation/luminance shifting",
        icon = Icons.Default.ColorLens
    ),

    BURHANUDDIN_STAMP(
        title = "Artist Signature",
        category = ToolCategory.BURHANUDDIN_CRAZY,
        description = "Burhanuddin custom photographer watermark & branding stamp",
        icon = Icons.Default.BrandingWatermark
    )
}
