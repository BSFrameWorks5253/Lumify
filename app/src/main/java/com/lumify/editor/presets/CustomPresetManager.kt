package com.lumify.editor.presets

import android.content.Context
import com.lumify.editor.core.EditState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object CustomPresetManager {

    private const val PRESETS_FILENAME = "custom_offline_presets.json"

    fun loadCustomPresets(context: Context): List<FilterPreset> {
        val file = File(context.filesDir, PRESETS_FILENAME)
        if (!file.exists()) return emptyList()

        return try {
            val content = file.readText()
            val array = JSONArray(content)
            val presets = mutableListOf<FilterPreset>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val name = obj.optString("name", "Custom Look")
                val category = obj.optString("category", "My Presets")
                val stateObj = obj.getJSONObject("state")
                val state = deserializeEditState(stateObj)

                presets.add(
                    FilterPreset(
                        id = id,
                        name = name,
                        category = category,
                        description = "Custom Offline Preset by Burhanuddin"
                    ) { _ ->
                        state.copy(appliedPresetId = id)
                    }
                )
            }
            presets
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePreset(context: Context, name: String, category: String, state: EditState): FilterPreset {
        val id = "custom_${System.currentTimeMillis()}"
        val file = File(context.filesDir, PRESETS_FILENAME)

        val array = if (file.exists()) {
            try { JSONArray(file.readText()) } catch (e: Exception) { JSONArray() }
        } else {
            JSONArray()
        }

        val obj = JSONObject().apply {
            put("id", id)
            put("name", name)
            put("category", category)
            put("createdAt", System.currentTimeMillis())
            put("state", serializeEditState(state))
        }

        array.put(obj)
        file.writeText(array.toString())

        return FilterPreset(
            id = id,
            name = name,
            category = category,
            description = "Custom Offline Preset"
        ) { _ ->
            state.copy(appliedPresetId = id)
        }
    }

    fun deletePreset(context: Context, id: String): Boolean {
        val file = File(context.filesDir, PRESETS_FILENAME)
        if (!file.exists()) return false

        return try {
            val array = JSONArray(file.readText())
            val newArray = JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                if (obj.optString("id") != id) {
                    newArray.put(obj)
                }
            }
            file.writeText(newArray.toString())
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun serializeEditState(state: EditState): JSONObject {
        return JSONObject().apply {
            put("exposure", state.exposure.toDouble())
            put("contrast", state.contrast.toDouble())
            put("highlights", state.highlights.toDouble())
            put("shadows", state.shadows.toDouble())
            put("whites", state.whites.toDouble())
            put("blacks", state.blacks.toDouble())
            put("temperature", state.temperature.toDouble())
            put("tint", state.tint.toDouble())
            put("vibrance", state.vibrance.toDouble())
            put("saturation", state.saturation.toDouble())
            put("clarity", state.clarity.toDouble())
            put("sharpness", state.sharpness.toDouble())
            put("vignette", state.vignette.toDouble())
            put("ambiance", state.ambiance.toDouble())
            put("structure", state.structure.toDouble())
            put("glamourGlow", state.glamourGlow.toDouble())
            put("drama", state.drama.toDouble())
            put("vintage", state.vintage.toDouble())
            put("grain", state.grain.toDouble())
            put("retrolux", state.retrolux.toDouble())
            put("blackAndWhite", state.blackAndWhite.toDouble())
            put("noir", state.noir.toDouble())
            put("portraitSmooth", state.portraitSmooth.toDouble())
            put("portraitSpotlight", state.portraitSpotlight.toDouble())
            put("lensBlur", state.lensBlur.toDouble())
            put("cyberpunkNeon", state.cyberpunkNeon.toDouble())
            put("levelsBlack", state.levelsBlack.toDouble())
            put("levelsWhite", state.levelsWhite.toDouble())
            put("levelsGamma", state.levelsGamma.toDouble())
            put("colorBalanceCyanRed", state.colorBalanceCyanRed.toDouble())
            put("colorBalanceMagentaGreen", state.colorBalanceMagentaGreen.toDouble())
            put("colorBalanceYellowBlue", state.colorBalanceYellowBlue.toDouble())
        }
    }

    private fun deserializeEditState(obj: JSONObject): EditState {
        return EditState(
            exposure = obj.optDouble("exposure", 0.0).toFloat(),
            contrast = obj.optDouble("contrast", 0.0).toFloat(),
            highlights = obj.optDouble("highlights", 0.0).toFloat(),
            shadows = obj.optDouble("shadows", 0.0).toFloat(),
            whites = obj.optDouble("whites", 0.0).toFloat(),
            blacks = obj.optDouble("blacks", 0.0).toFloat(),
            temperature = obj.optDouble("temperature", 0.0).toFloat(),
            tint = obj.optDouble("tint", 0.0).toFloat(),
            vibrance = obj.optDouble("vibrance", 0.0).toFloat(),
            saturation = obj.optDouble("saturation", 0.0).toFloat(),
            clarity = obj.optDouble("clarity", 0.0).toFloat(),
            sharpness = obj.optDouble("sharpness", 0.0).toFloat(),
            vignette = obj.optDouble("vignette", 0.0).toFloat(),
            ambiance = obj.optDouble("ambiance", 0.0).toFloat(),
            structure = obj.optDouble("structure", 0.0).toFloat(),
            glamourGlow = obj.optDouble("glamourGlow", 0.0).toFloat(),
            drama = obj.optDouble("drama", 0.0).toFloat(),
            vintage = obj.optDouble("vintage", 0.0).toFloat(),
            grain = obj.optDouble("grain", 0.0).toFloat(),
            retrolux = obj.optDouble("retrolux", 0.0).toFloat(),
            blackAndWhite = obj.optDouble("blackAndWhite", 0.0).toFloat(),
            noir = obj.optDouble("noir", 0.0).toFloat(),
            portraitSmooth = obj.optDouble("portraitSmooth", 0.0).toFloat(),
            portraitSpotlight = obj.optDouble("portraitSpotlight", 0.0).toFloat(),
            lensBlur = obj.optDouble("lensBlur", 0.0).toFloat(),
            cyberpunkNeon = obj.optDouble("cyberpunkNeon", 0.0).toFloat(),
            levelsBlack = obj.optDouble("levelsBlack", 0.0).toFloat(),
            levelsWhite = obj.optDouble("levelsWhite", 255.0).toFloat(),
            levelsGamma = obj.optDouble("levelsGamma", 1.0).toFloat(),
            colorBalanceCyanRed = obj.optDouble("colorBalanceCyanRed", 0.0).toFloat(),
            colorBalanceMagentaGreen = obj.optDouble("colorBalanceMagentaGreen", 0.0).toFloat(),
            colorBalanceYellowBlue = obj.optDouble("colorBalanceYellowBlue", 0.0).toFloat()
        )
    }
}
