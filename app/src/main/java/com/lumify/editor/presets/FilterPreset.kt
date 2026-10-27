package com.lumify.editor.presets

import com.lumify.editor.core.EditState
import com.lumify.editor.core.HslAdjustment
import com.lumify.editor.core.HslChannel

data class FilterPreset(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val applyState: (EditState) -> EditState
)

object PresetRepository {

    val allPresets: List<FilterPreset> = listOf(
        FilterPreset(
            id = "neutral",
            name = "RAW Neutral",
            category = "Essential",
            description = "Natural baseline with zero coloration"
        ) { state ->
            state.copy(
                exposure = 0f,
                contrast = 5f,
                highlights = 0f,
                shadows = 10f,
                whites = 0f,
                blacks = 0f,
                temperature = 0f,
                tint = 0f,
                vibrance = 15f,
                saturation = 0f,
                clarity = 5f,
                sharpness = 20f,
                vignette = 0f,
                appliedPresetId = "neutral"
            )
        },

        FilterPreset(
            id = "golden_hour",
            name = "Golden Hour",
            category = "Cinematic",
            description = "Warm amber sun, rich sunset glow and deep gold tones"
        ) { state ->
            val hsl = state.hslAdjustments.toMutableMap()
            hsl[HslChannel.ORANGE] = HslAdjustment(hueShift = -5f, saturation = 25f, luminance = 10f)
            hsl[HslChannel.YELLOW] = HslAdjustment(hueShift = -10f, saturation = 30f, luminance = 15f)
            hsl[HslChannel.BLUE] = HslAdjustment(hueShift = 5f, saturation = -20f, luminance = -10f)

            state.copy(
                exposure = 5f,
                contrast = 15f,
                highlights = -15f,
                shadows = 20f,
                whites = 10f,
                temperature = 32f,
                tint = 8f,
                vibrance = 25f,
                saturation = 5f,
                clarity = 10f,
                vignette = 15f,
                hslAdjustments = hsl,
                appliedPresetId = "golden_hour"
            )
        },

        FilterPreset(
            id = "teal_orange",
            name = "Teal & Orange",
            category = "Cinematic",
            description = "Modern blockbuster look: cyan shadows and rich skin tones"
        ) { state ->
            val hsl = state.hslAdjustments.toMutableMap()
            hsl[HslChannel.ORANGE] = HslAdjustment(hueShift = 5f, saturation = 35f, luminance = 10f)
            hsl[HslChannel.CYAN] = HslAdjustment(hueShift = -10f, saturation = 40f, luminance = -5f)
            hsl[HslChannel.BLUE] = HslAdjustment(hueShift = -25f, saturation = 45f, luminance = -15f)
            hsl[HslChannel.GREEN] = HslAdjustment(hueShift = 30f, saturation = -30f, luminance = 0f)

            state.copy(
                exposure = 0f,
                contrast = 25f,
                highlights = -20f,
                shadows = 15f,
                blacks = -10f,
                temperature = 10f,
                tint = -5f,
                vibrance = 30f,
                saturation = -5f,
                clarity = 15f,
                vignette = 20f,
                hslAdjustments = hsl,
                appliedPresetId = "teal_orange"
            )
        },

        FilterPreset(
            id = "moody_film",
            name = "Moody Film",
            category = "Analog",
            description = "Matte faded blacks, desaturated tones, and classic analog character"
        ) { state ->
            val hsl = state.hslAdjustments.toMutableMap()
            hsl[HslChannel.GREEN] = HslAdjustment(hueShift = 15f, saturation = -35f, luminance = -10f)
            hsl[HslChannel.YELLOW] = HslAdjustment(hueShift = 5f, saturation = -20f, luminance = -5f)

            state.copy(
                exposure = -5f,
                contrast = 10f,
                highlights = -25f,
                shadows = 30f,
                blacks = 25f, // Matte blacks lift
                temperature = -8f,
                tint = 12f,
                vibrance = -15f,
                saturation = -20f,
                clarity = -5f,
                vignette = 30f,
                sharpness = 25f,
                hslAdjustments = hsl,
                appliedPresetId = "moody_film"
            )
        },

        FilterPreset(
            id = "monochrome_pro",
            name = "Silver B&W",
            category = "Monochrome",
            description = "High dynamic range black & white with punchy silver tones"
        ) { state ->
            state.copy(
                exposure = 5f,
                contrast = 35f,
                highlights = -10f,
                shadows = 20f,
                whites = 20f,
                blacks = -25f,
                saturation = -100f,
                vibrance = 0f,
                clarity = 25f,
                sharpness = 40f,
                vignette = 25f,
                appliedPresetId = "monochrome_pro"
            )
        },

        FilterPreset(
            id = "cyberpunk",
            name = "Cyberpunk",
            category = "Creative",
            description = "Neon blues, vivid purples, and high-tech night glow"
        ) { state ->
            val hsl = state.hslAdjustments.toMutableMap()
            hsl[HslChannel.CYAN] = HslAdjustment(hueShift = 10f, saturation = 60f, luminance = 20f)
            hsl[HslChannel.MAGENTA] = HslAdjustment(hueShift = -10f, saturation = 70f, luminance = 15f)
            hsl[HslChannel.BLUE] = HslAdjustment(hueShift = 0f, saturation = 50f, luminance = 10f)

            state.copy(
                exposure = -5f,
                contrast = 30f,
                highlights = 15f,
                shadows = -15f,
                temperature = -25f,
                tint = 35f,
                vibrance = 45f,
                saturation = 20f,
                clarity = 25f,
                vignette = 35f,
                hslAdjustments = hsl,
                appliedPresetId = "cyberpunk"
            )
        },

        FilterPreset(
            id = "hdr_pop",
            name = "HDR Pop",
            category = "Landscape",
            description = "Maximum dynamic range recovery with vibrant foliage and punchy skies"
        ) { state ->
            state.copy(
                exposure = 5f,
                contrast = 15f,
                highlights = -50f,
                shadows = 55f,
                whites = 15f,
                blacks = -15f,
                temperature = 5f,
                vibrance = 40f,
                saturation = 10f,
                clarity = 30f,
                sharpness = 35f,
                vignette = 10f,
                appliedPresetId = "hdr_pop"
            )
        }
    )
}
