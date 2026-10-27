package com.lumify.editor.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object ImageProcessor {

    /**
     * Applies full non-destructive editing pipeline to the source bitmap
     * returning a newly processed bitmap.
     */
    suspend fun processImage(
        source: Bitmap,
        state: EditState,
        isExport: Boolean = false
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!state.isModified && !isExport) {
            return@withContext source
        }

        // 1. Spatial Transformation (Crop, Rotate, Flip)
        val transformed = applySpatialTransforms(source, state)

        // 2. Color, Exposure, HSL, Tone & Detail Processing
        val processed = applyColorAndDetailPipeline(transformed, state)

        if (transformed != source && transformed != processed) {
            transformed.recycle()
        }

        processed
    }

    private fun applySpatialTransforms(source: Bitmap, state: EditState): Bitmap {
        var bmp = source
        val totalRotation = (state.rotationDegrees + state.fineRotation) % 360f

        if (totalRotation != 0f || state.flipHorizontal || state.flipVertical) {
            val matrix = Matrix()
            if (totalRotation != 0f) {
                matrix.postRotate(totalRotation)
            }
            val scaleX = if (state.flipHorizontal) -1f else 1f
            val scaleY = if (state.flipVertical) -1f else 1f
            if (scaleX != 1f || scaleY != 1f) {
                matrix.postScale(scaleX, scaleY)
            }

            val rotated = Bitmap.createBitmap(
                bmp, 0, 0, bmp.width, bmp.height, matrix, true
            )
            bmp = rotated
        }

        // Apply Crop if specified
        state.cropRect?.let { crop ->
            val left = (crop.left * bmp.width).toInt().coerceIn(0, bmp.width - 1)
            val top = (crop.top * bmp.height).toInt().coerceIn(0, bmp.height - 1)
            val width = ((crop.right - crop.left) * bmp.width).toInt().coerceIn(1, bmp.width - left)
            val height = ((crop.bottom - crop.top) * bmp.height).toInt().coerceIn(1, bmp.height - top)

            if (width > 0 && height > 0 && (width != bmp.width || height != bmp.height)) {
                val cropped = Bitmap.createBitmap(bmp, left, top, width, height)
                if (bmp != source) bmp.recycle()
                bmp = cropped
            }
        }

        return bmp
    }

    private fun applyColorAndDetailPipeline(source: Bitmap, state: EditState): Bitmap {
        val width = source.width
        val height = source.height

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Precompute parameter multipliers
        val exposureFactor = 2.0.pow((state.exposure / 50.0).toDouble()).toFloat()
        val contrastFactor = if (state.contrast == 0f) 1f else {
            val c = (state.contrast * 2.55f).coerceIn(-254f, 254f)
            (259f * (c + 255f)) / (255f * (259f - c))
        }

        val highlightsShift = state.highlights / 100f
        val shadowsShift = state.shadows / 100f
        val whitesShift = state.whites / 100f
        val blacksShift = state.blacks / 100f

        val tempShift = state.temperature / 100f  // + is warm (amber), - is cool (blue)
        val tintShift = state.tint / 100f        // + is magenta, - is green

        val satFactor = (state.saturation + 100f) / 100f
        val vibranceFactor = state.vibrance / 100f

        val vignetteAmount = state.vignette / 100f
        val centerX = width / 2.0
        val centerY = height / 2.0
        val maxDist = sqrt(centerX * centerX + centerY * centerY)

        val hasHsl = state.hslAdjustments.values.any { !it.isDefault }
        val hsv = FloatArray(3)

        // Process pixels
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p shr 24) and 0xFF
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF

            // 1. Exposure
            var rf = r * exposureFactor
            var gf = g * exposureFactor
            var bf = b * exposureFactor

            // 2. Temperature & Tint
            if (tempShift != 0f) {
                rf += tempShift * 28f
                bf -= tempShift * 28f
            }
            if (tintShift != 0f) {
                gf -= tintShift * 18f
                rf += tintShift * 12f
                bf += tintShift * 12f
            }

            // 3. Contrast (around midtone 128)
            if (contrastFactor != 1f) {
                rf = 128f + (rf - 128f) * contrastFactor
                gf = 128f + (gf - 128f) * contrastFactor
                bf = 128f + (bf - 128f) * contrastFactor
            }

            // 4. Shadows & Highlights recovery
            val lum = (0.299f * rf + 0.587f * gf + 0.114f * bf).coerceIn(0f, 255f)
            val normalizedLum = lum / 255f

            if (shadowsShift != 0f) {
                // Lift/crush shadows below midtone
                val shadowWeight = (1f - normalizedLum).coerceIn(0f, 1f).pow(1.8f)
                val shadowDelta = shadowsShift * shadowWeight * 60f
                rf += shadowDelta
                gf += shadowDelta
                bf += shadowDelta
            }

            if (highlightsShift != 0f) {
                // Recover/boost highlights above midtone
                val highlightWeight = normalizedLum.coerceIn(0f, 1f).pow(1.8f)
                val highlightDelta = highlightsShift * highlightWeight * 60f
                rf += highlightDelta
                gf += highlightDelta
                bf += highlightDelta
            }

            if (whitesShift != 0f && normalizedLum > 0.6f) {
                val weight = ((normalizedLum - 0.6f) / 0.4f).pow(1.5f)
                rf += whitesShift * weight * 40f
                gf += whitesShift * weight * 40f
                bf += whitesShift * weight * 40f
            }

            if (blacksShift != 0f && normalizedLum < 0.4f) {
                val weight = ((0.4f - normalizedLum) / 0.4f).pow(1.5f)
                rf += blacksShift * weight * 40f
                gf += blacksShift * weight * 40f
                bf += blacksShift * weight * 40f
            }

            // Clamp intermediate RGB
            var rClamped = rf.coerceIn(0f, 255f).toInt()
            var gClamped = gf.coerceIn(0f, 255f).toInt()
            var bClamped = bf.coerceIn(0f, 255f).toInt()

            // 5. Saturation & Vibrance & HSL Color Grading
            if (satFactor != 1f || vibranceFactor != 0f || hasHsl) {
                Color.RGBToHSV(rClamped, gClamped, bClamped, hsv)
                var hue = hsv[0]
                var sat = hsv[1]
                var value = hsv[2]

                // Global Saturation
                sat = (sat * satFactor).coerceIn(0f, 1f)

                // Vibrance (protects already saturated colors)
                if (vibranceFactor != 0f) {
                    val satComplement = 1f - sat
                    sat = (sat + vibranceFactor * satComplement * 0.5f).coerceIn(0f, 1f)
                }

                // 8-channel HSL Color Grading
                if (hasHsl) {
                    val targetChannel = getClosestHslChannel(hue)
                    val adjustment = state.hslAdjustments[targetChannel]
                    if (adjustment != null && !adjustment.isDefault) {
                        hue = (hue + adjustment.hueShift * 0.6f).mod(360f)
                        if (hue < 0) hue += 360f

                        sat = (sat * (1f + adjustment.saturation / 100f)).coerceIn(0f, 1f)
                        value = (value * (1f + adjustment.luminance / 100f)).coerceIn(0f, 1f)
                    }
                }

                hsv[0] = hue
                hsv[1] = sat
                hsv[2] = value

                val newColor = Color.HSVToColor(hsv)
                rClamped = (newColor shr 16) and 0xFF
                gClamped = (newColor shr 8) and 0xFF
                bClamped = newColor and 0xFF
            }

            // 6. Vignette
            if (vignetteAmount > 0f) {
                val px = (i % width) - centerX
                val py = (i / width) - centerY
                val dist = sqrt(px * px + py * py) / maxDist
                if (dist > 0.35) {
                    val vig = (1f - ((dist - 0.35) / 0.65).toFloat().pow(1.5f) * vignetteAmount).coerceIn(0f, 1f)
                    rClamped = (rClamped * vig).toInt()
                    gClamped = (gClamped * vig).toInt()
                    bClamped = (bClamped * vig).toInt()
                }
            }

            pixels[i] = (a shl 24) or (rClamped shl 16) or (gClamped shl 8) or bClamped
        }

        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        // 7. Sharpness / Clarity (Convolution if requested)
        return if (state.sharpness > 0f || state.clarity != 0f) {
            applySharpnessAndClarity(outBitmap, state.sharpness, state.clarity)
        } else {
            outBitmap
        }
    }

    private fun getClosestHslChannel(hue: Float): HslChannel {
        return when {
            hue >= 345f || hue < 15f -> HslChannel.RED
            hue in 15f..<45f -> HslChannel.ORANGE
            hue in 45f..<90f -> HslChannel.YELLOW
            hue in 90f..<150f -> HslChannel.GREEN
            hue in 150f..<210f -> HslChannel.CYAN
            hue in 210f..<260f -> HslChannel.BLUE
            hue in 260f..<300f -> HslChannel.PURPLE
            else -> HslChannel.MAGENTA
        }
    }

    /**
     * High-performance 3x3 unsharp mask filter for fine texture and edge crispness.
     */
    private fun applySharpnessAndClarity(bitmap: Bitmap, sharpness: Float, clarity: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        bitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val amount = (sharpness / 100f * 0.7f) + (clarity / 100f * 0.3f)
        val center = 1f + 4f * amount
        val edge = -amount

        for (y in 1 until height - 1) {
            val yOffset = y * width
            for (x in 1 until width - 1) {
                val idx = yOffset + x

                val top = srcPixels[idx - width]
                val bottom = srcPixels[idx + width]
                val left = srcPixels[idx - 1]
                val right = srcPixels[idx + 1]
                val mid = srcPixels[idx]

                val a = (mid shr 24) and 0xFF

                val rMid = (mid shr 16) and 0xFF
                val rNeighbors = ((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) +
                                 ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)
                val rFinal = (center * rMid + edge * rNeighbors).toInt().coerceIn(0, 255)

                val gMid = (mid shr 8) and 0xFF
                val gNeighbors = ((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) +
                                 ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)
                val gFinal = (center * gMid + edge * gNeighbors).toInt().coerceIn(0, 255)

                val bMid = mid and 0xFF
                val bNeighbors = (top and 0xFF) + (bottom and 0xFF) + (left and 0xFF) + (right and 0xFF)
                val bFinal = (center * bMid + edge * bNeighbors).toInt().coerceIn(0, 255)

                dstPixels[idx] = (a shl 24) or (rFinal shl 16) or (gFinal shl 8) or bFinal
            }
        }

        // Fill borders from source
        for (x in 0 until width) {
            dstPixels[x] = srcPixels[x]
            dstPixels[(height - 1) * width + x] = srcPixels[(height - 1) * width + x]
        }
        for (y in 0 until height) {
            dstPixels[y * width] = srcPixels[y * width]
            dstPixels[y * width + (width - 1)] = srcPixels[y * width + (width - 1)]
        }

        output.setPixels(dstPixels, 0, width, 0, 0, width, height)
        bitmap.recycle()
        return output
    }

    /**
     * Computes RGB + Luminance histograms for real-time visualization.
     */
    suspend fun computeHistogram(bitmap: Bitmap): HistogramData = withContext(Dispatchers.Default) {
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        val luminance = IntArray(256)

        // Downsample for fast 60fps histogram calculation
        val step = max(1, (bitmap.width * bitmap.height) / 50000)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var idx = 0
        while (idx < pixels.size) {
            val p = pixels[idx]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)

            red[r]++
            green[g]++
            blue[b]++
            luminance[lum]++

            idx += step
        }

        HistogramData(red, green, blue, luminance)
    }
}

data class HistogramData(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val luminance: IntArray
) {
    val maxCount: Int = max(
        luminance.maxOrNull() ?: 1,
        max(red.maxOrNull() ?: 1, max(green.maxOrNull() ?: 1, blue.maxOrNull() ?: 1))
    )
}
