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
            id = "current",
            name = "Current",
            category = "Snapseed Looks",
            description = "Natural unedited state"
        ) { state ->
            EditState()
        },

        FilterPreset(
            id = "portrait",
            name = "Portrait",
            category = "Snapseed Looks",
            description = "Soft flattering skin tones with gentle highlight glow"
        ) { state ->
            state.copy(
                exposure = 6f,
                contrast = -5f,
                highlights = -10f,
                shadows = 15f,
                temperature = 12f,
                tint = 4f,
                vibrance = 16f,
                clarity = -8f,
                glamourGlow = 25f,
                portraitSmooth = 30f,
                portraitSpotlight = 20f,
                appliedPresetId = "portrait"
            )
        },

        FilterPreset(
            id = "smooth",
            name = "Smooth",
            category = "Snapseed Looks",
            description = "Dreamy soft-focus with clean even tones"
        ) { state ->
            state.copy(
                exposure = 4f,
                contrast = -12f,
                highlights = -18f,
                shadows = 22f,
                temperature = 6f,
                vibrance = 14f,
                clarity = -15f,
                glamourGlow = 35f,
                sharpness = 10f,
                appliedPresetId = "smooth"
            )
        },

        FilterPreset(
            id = "pop",
            name = "Pop",
            category = "Snapseed Looks",
            description = "Snapseed's iconic punchy colors, vibrant contrast, and clarity"
        ) { state ->
            state.copy(
                exposure = 4f,
                contrast = 24f,
                highlights = -15f,
                shadows = 20f,
                ambiance = 28f,
                vibrance = 32f,
                saturation = 10f,
                clarity = 25f,
                sharpness = 30f,
                appliedPresetId = "pop"
            )
        },

        FilterPreset(
            id = "accentuate",
            name = "Accentuate",
            category = "Snapseed Looks",
            description = "Intense micro-contrast with crisp details and rich shadows"
        ) { state ->
            state.copy(
                exposure = 2f,
                contrast = 18f,
                highlights = -22f,
                shadows = 16f,
                ambiance = 20f,
                structure = 35f,
                clarity = 30f,
                sharpness = 45f,
                vibrance = 15f,
                appliedPresetId = "accentuate"
            )
        },

        FilterPreset(
            id = "faded_glow",
            name = "Faded Glow",
            category = "Snapseed Looks",
            description = "Atmospheric faded shadows with soft ethereal bloom"
        ) { state ->
            state.copy(
                exposure = 6f,
                contrast = -8f,
                shadows = 35f,
                blacks = 28f,
                temperature = 14f,
                tint = 8f,
                glamourGlow = 45f,
                vintage = 25f,
                vignette = 20f,
                appliedPresetId = "faded_glow"
            )
        },

        FilterPreset(
            id = "morning",
            name = "Morning",
            category = "Snapseed Looks",
            description = "Gentle morning sun with luminous pastel tones"
        ) { state ->
            state.copy(
                exposure = 8f,
                contrast = 10f,
                highlights = -12f,
                shadows = 18f,
                temperature = 22f,
                tint = 6f,
                vibrance = 25f,
                saturation = 8f,
                glamourGlow = 20f,
                appliedPresetId = "morning"
            )
        },

        FilterPreset(
            id = "bright",
            name = "Bright",
            category = "Snapseed Looks",
            description = "High-key crisp brightness with vivid highlights"
        ) { state ->
            state.copy(
                exposure = 22f,
                contrast = 8f,
                highlights = -10f,
                shadows = 30f,
                whites = 20f,
                vibrance = 18f,
                clarity = 12f,
                sharpness = 25f,
                appliedPresetId = "bright"
            )
        },

        FilterPreset(
            id = "fine_art",
            name = "Fine Art",
            category = "Snapseed Looks",
            description = "Museum grade dramatic black and white with silver gradation"
        ) { state ->
            state.copy(
                exposure = 4f,
                contrast = 38f,
                highlights = -15f,
                shadows = 20f,
                whites = 25f,
                blacks = -30f,
                clarity = 32f,
                sharpness = 40f,
                blackAndWhite = 100f,
                vignette = 30f,
                appliedPresetId = "fine_art"
            )
        },

        FilterPreset(
            id = "push",
            name = "Push",
            category = "Snapseed Looks",
            description = "High-ISO pushed film with gritty texture and deep blacks"
        ) { state ->
            state.copy(
                exposure = 2f,
                contrast = 30f,
                shadows = -10f,
                blacks = -20f,
                grain = 45f,
                clarity = 20f,
                sharpness = 35f,
                vintage = 20f,
                appliedPresetId = "push"
            )
        },

        FilterPreset(
            id = "structure",
            name = "Structure",
            category = "Snapseed Looks",
            description = "Emphasized textures, stone, architecture, and dramatic detail"
        ) { state ->
            state.copy(
                contrast = 15f,
                shadows = 25f,
                highlights = -30f,
                structure = 60f,
                clarity = 50f,
                sharpness = 60f,
                ambiance = 25f,
                appliedPresetId = "structure"
            )
        },

        FilterPreset(
            id = "silhouette",
            name = "Silhouette",
            category = "Snapseed Looks",
            description = "Crushed midnight shadows against vivid colorful sky"
        ) { state ->
            state.copy(
                exposure = -15f,
                contrast = 50f,
                highlights = 15f,
                shadows = -80f,
                blacks = -90f,
                vibrance = 45f,
                saturation = 25f,
                vignette = 40f,
                appliedPresetId = "silhouette"
            )
        },

        // Burhanuddin Crazy Features
        FilterPreset(
            id = "cyberpunk_tokyo",
            name = "Cyberpunk",
            category = "Burhanuddin Crazy",
            description = "Electric neon blues, hot magenta bloom, and futuristic sci-fi aesthetic"
        ) { state ->
            state.copy(
                exposure = 2f,
                contrast = 35f,
                shadows = -10f,
                temperature = -28f,
                tint = 45f,
                vibrance = 55f,
                saturation = 20f,
                cyberpunkNeon = 75f,
                clarity = 30f,
                vignette = 35f,
                appliedPresetId = "cyberpunk_tokyo"
            )
        },

        FilterPreset(
            id = "neon_noir",
            name = "Neon Noir",
            category = "Burhanuddin Crazy",
            description = "Ultra high-contrast moody dark film with glowing highlights"
        ) { state ->
            state.copy(
                exposure = -5f,
                contrast = 45f,
                shadows = -40f,
                noir = 70f,
                cyberpunkNeon = 40f,
                grain = 30f,
                clarity = 25f,
                vignette = 45f,
                appliedPresetId = "neon_noir"
            )
        },

        FilterPreset(
            id = "golden_mirage",
            name = "Golden Mirage",
            category = "Burhanuddin Crazy",
            description = "Radiant sun flare, rich amber highlights, and warm cinematic glow"
        ) { state ->
            state.copy(
                exposure = 8f,
                contrast = 18f,
                highlights = -10f,
                shadows = 30f,
                temperature = 42f,
                tint = 12f,
                vibrance = 35f,
                glamourGlow = 40f,
                vintage = 30f,
                appliedPresetId = "golden_mirage"
            )
        },

        FilterPreset(
            id = "emerald_matrix",
            name = "Emerald Matrix",
            category = "Burhanuddin Crazy",
            description = "Atmospheric sci-fi emerald green shadows and rich tonal separation"
        ) { state ->
            state.copy(
                exposure = 0f,
                contrast = 28f,
                shadows = 15f,
                temperature = -15f,
                tint = -40f,
                vibrance = 25f,
                clarity = 35f,
                vignette = 30f,
                appliedPresetId = "emerald_matrix"
            )
        }
    )
}
