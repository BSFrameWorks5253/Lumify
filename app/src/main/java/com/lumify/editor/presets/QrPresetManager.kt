package com.lumify.editor.presets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.lumify.editor.core.EditState
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

object QrPresetManager {

    /**
     * Generates a 100% offline QR code Bitmap containing all recipe adjustments.
     */
    fun generateQrBitmap(presetName: String, state: EditState, size: Int = 512): Bitmap? {
        return try {
            val json = serializeCompact(presetName, state)
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(json, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val color = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                    bmp.setPixel(x, y, color)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Exports a portable offline .lumify recipe file.
     */
    fun exportLumifyFile(context: Context, presetName: String, state: EditState): File {
        val sanitized = presetName.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val file = File(context.cacheDir, "$sanitized.lumify")
        val json = serializeCompact(presetName, state)
        file.writeText(json)
        return file
    }

    /**
     * Decodes a QR code from a camera frame or photo in the gallery.
     */
    fun decodeQrBitmap(bitmap: Bitmap): FilterPreset? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = QRCodeReader().decode(binaryBitmap)

            importPresetJson(result.text)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Imports a recipe from raw JSON or a .lumify file stream.
     */
    fun importFromStream(inputStream: InputStream): FilterPreset? {
        return try {
            val content = inputStream.bufferedReader().use { it.readText() }
            importPresetJson(content)
        } catch (e: Exception) {
            null
        }
    }

    fun importPresetJson(jsonString: String): FilterPreset? {
        return try {
            val obj = JSONObject(jsonString)
            val name = obj.optString("name", "Imported Look")
            val id = "imported_${UUID.randomUUID().toString().take(8)}"
            val state = deserializeCompact(obj)

            FilterPreset(
                id = id,
                name = name,
                category = "Imported",
                description = "Imported via QR / .lumify recipe"
            ) { _ ->
                state.copy(appliedPresetId = id)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeCompact(name: String, state: EditState): String {
        return JSONObject().apply {
            put("app", "Lumify")
            put("version", "1.0")
            put("name", name)
            put("exp", state.exposure.toDouble())
            put("con", state.contrast.toDouble())
            put("sat", state.saturation.toDouble())
            put("amb", state.ambiance.toDouble())
            put("hl", state.highlights.toDouble())
            put("sh", state.shadows.toDouble())
            put("wh", state.whites.toDouble())
            put("bl", state.blacks.toDouble())
            put("tmp", state.temperature.toDouble())
            put("tnt", state.tint.toDouble())
            put("str", state.structure.toDouble())
            put("shp", state.sharpness.toDouble())
            put("vig", state.vignette.toDouble())
            put("glw", state.glamourGlow.toDouble())
            put("drm", state.drama.toDouble())
            put("vnt", state.vintage.toDouble())
            put("grn", state.grain.toDouble())
            put("neon", state.cyberpunkNeon.toDouble())
            put("lblk", state.levelsBlack.toDouble())
            put("lwht", state.levelsWhite.toDouble())
            put("lgam", state.levelsGamma.toDouble())
            put("cbr", state.colorBalanceCyanRed.toDouble())
            put("cbm", state.colorBalanceMagentaGreen.toDouble())
            put("cby", state.colorBalanceYellowBlue.toDouble())
        }.toString()
    }

    private fun deserializeCompact(obj: JSONObject): EditState {
        return EditState(
            exposure = obj.optDouble("exp", 0.0).toFloat(),
            contrast = obj.optDouble("con", 0.0).toFloat(),
            saturation = obj.optDouble("sat", 0.0).toFloat(),
            ambiance = obj.optDouble("amb", 0.0).toFloat(),
            highlights = obj.optDouble("hl", 0.0).toFloat(),
            shadows = obj.optDouble("sh", 0.0).toFloat(),
            whites = obj.optDouble("wh", 0.0).toFloat(),
            blacks = obj.optDouble("bl", 0.0).toFloat(),
            temperature = obj.optDouble("tmp", 0.0).toFloat(),
            tint = obj.optDouble("tnt", 0.0).toFloat(),
            structure = obj.optDouble("str", 0.0).toFloat(),
            sharpness = obj.optDouble("shp", 0.0).toFloat(),
            vignette = obj.optDouble("vig", 0.0).toFloat(),
            glamourGlow = obj.optDouble("glw", 0.0).toFloat(),
            drama = obj.optDouble("drm", 0.0).toFloat(),
            vintage = obj.optDouble("vnt", 0.0).toFloat(),
            grain = obj.optDouble("grn", 0.0).toFloat(),
            cyberpunkNeon = obj.optDouble("neon", 0.0).toFloat(),
            levelsBlack = obj.optDouble("lblk", 0.0).toFloat(),
            levelsWhite = obj.optDouble("lwht", 0.0).toFloat(),
            levelsGamma = obj.optDouble("lgam", 0.0).toFloat(),
            colorBalanceCyanRed = obj.optDouble("cbr", 0.0).toFloat(),
            colorBalanceMagentaGreen = obj.optDouble("cbm", 0.0).toFloat(),
            colorBalanceYellowBlue = obj.optDouble("cby", 0.0).toFloat()
        )
    }
}
