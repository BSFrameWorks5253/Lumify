package com.lumify.editor.core

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

object ImageProcessor {

    suspend fun processImage(
        source: Bitmap,
        state: EditState,
        secondaryBitmap: Bitmap? = null,
        isExport: Boolean = false
    ): Bitmap = withContext(Dispatchers.Default) {
        if (!state.isModified && !isExport && secondaryBitmap == null) {
            return@withContext source
        }

        // 1. Spatial Transforms (Crop, Rotate, Flip, Lens Distortion)
        val transformed = applySpatialTransforms(source, state)

        // 2. Healing / Spot Removal (Applied first to raw pixels before color grading)
        val healed = if (state.healingStrokes.isNotEmpty()) {
            applyHealing(transformed, state.healingStrokes)
        } else {
            transformed
        }

        // 3. Color, Tone Curves, HSL, Masking & Detail Pipeline
        val processed = applyColorAndDetailPipeline(healed, state)

        if (healed != source && healed != transformed && healed != processed) {
            healed.recycle()
        }
        if (transformed != source && transformed != processed) {
            transformed.recycle()
        }

        // 4. Snapseed Selective U-Point Control Points
        val selectiveApplied = if (state.selectivePoints.any { !it.isDefault }) {
            applySelectiveUPoint(processed, state.selectivePoints)
        } else {
            processed
        }

        // 5. Double Exposure
        val doubleExposureApplied = if (!state.doubleExposure.isDefault && secondaryBitmap != null) {
            applyDoubleExposure(selectiveApplied, secondaryBitmap, state.doubleExposure)
        } else {
            selectiveApplied
        }

        // 6. Photographer Watermarking (Text or Graphic)
        val finalResult = if (state.watermark.enabled) {
            applyWatermark(doubleExposureApplied, state.watermark)
        } else {
            doubleExposureApplied
        }

        finalResult
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

        // Lens Distortion (Barrel / Pincushion warp)
        if (state.lensDistortion != 0f) {
            bmp = applyLensDistortion(bmp, state.lensDistortion / 100f)
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

    private fun applyLensDistortion(source: Bitmap, distortion: Float): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val srcPixels = IntArray(width * height)
        val dstPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val cx = width / 2f
        val cy = height / 2f
        val maxR = sqrt(cx * cx + cy * cy)
        val k = distortion * 0.35f

        for (y in 0 until height) {
            val dy = (y - cy) / maxR
            for (x in 0 until width) {
                val dx = (x - cx) / maxR
                val r2 = dx * dx + dy * dy
                val f = 1f + k * r2

                val srcX = (cx + dx * f * maxR).toInt()
                val srcY = (cy + dy * f * maxR).toInt()

                if (srcX in 0 until width && srcY in 0 until height) {
                    dstPixels[y * width + x] = srcPixels[srcY * width + srcX]
                } else {
                    dstPixels[y * width + x] = Color.BLACK
                }
            }
        }

        output.setPixels(dstPixels, 0, width, 0, 0, width, height)
        if (source != output) source.recycle()
        return output
    }

    private fun applyColorAndDetailPipeline(source: Bitmap, state: EditState): Bitmap {
        val width = source.width
        val height = source.height

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // 1. Build Tone Curve LUTs (O(1) instant lookup tables)
        val rgbLut = buildCurveLut(state.toneCurves.rgbPoints)
        val redLut = buildCurveLut(state.toneCurves.redPoints)
        val greenLut = buildCurveLut(state.toneCurves.greenPoints)
        val blueLut = buildCurveLut(state.toneCurves.bluePoints)
        val hasCurves = !state.toneCurves.isDefault

        // Multipliers
        val exposureFactor = 2.0.pow((state.exposure / 50.0).toDouble()).toFloat()
        val contrastFactor = if (state.contrast == 0f) 1f else {
            val c = (state.contrast * 2.55f).coerceIn(-254f, 254f)
            (259f * (c + 255f)) / (255f * (259f - c))
        }

        val highlightsShift = state.highlights / 100f
        val shadowsShift = state.shadows / 100f
        val whitesShift = state.whites / 100f
        val blacksShift = state.blacks / 100f

        val tempShift = state.temperature / 100f
        val tintShift = state.tint / 100f

        val satFactor = (state.saturation + 100f) / 100f
        val vibranceFactor = state.vibrance / 100f
        val defringeAmount = state.defringeAmount / 100f

        val vignetteAmount = state.vignette / 100f
        val centerX = width / 2.0
        val centerY = height / 2.0
        val maxDist = sqrt(centerX * centerX + centerY * centerY)

        val hasHsl = state.hslAdjustments.values.any { !it.isDefault }
        val mask = state.activeMask?.takeIf { it.isModified }
        val hsv = FloatArray(3)

        // Process pixels
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p shr 24) and 0xFF
            var r = (p shr 16) and 0xFF
            var g = (p shr 8) and 0xFF
            var b = p and 0xFF

            // Exposure
            var rf = r * exposureFactor
            var gf = g * exposureFactor
            var bf = b * exposureFactor

            // White Balance (Temperature & Tint)
            if (tempShift != 0f) {
                rf += tempShift * 28f
                bf -= tempShift * 28f
            }
            if (tintShift != 0f) {
                gf -= tintShift * 18f
                rf += tintShift * 12f
                bf += tintShift * 12f
            }

            // Contrast
            if (contrastFactor != 1f) {
                rf = 128f + (rf - 128f) * contrastFactor
                gf = 128f + (gf - 128f) * contrastFactor
                bf = 128f + (bf - 128f) * contrastFactor
            }

            // Shadows & Highlights
            val lum = (0.299f * rf + 0.587f * gf + 0.114f * bf).coerceIn(0f, 255f)
            val normalizedLum = lum / 255f

            if (shadowsShift != 0f) {
                val shadowWeight = (1f - normalizedLum).coerceIn(0f, 1f).pow(1.8f)
                val shadowDelta = shadowsShift * shadowWeight * 60f
                rf += shadowDelta
                gf += shadowDelta
                bf += shadowDelta
            }

            if (highlightsShift != 0f) {
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

            // Snapseed Ambiance
            if (state.ambiance != 0f) {
                val amb = state.ambiance / 100f
                val shadowBoost = (1f - normalizedLum) * amb * 32f
                val midBoost = (1f - abs(normalizedLum - 0.5f) * 2f).coerceIn(0f, 1f) * amb * 16f
                rf += shadowBoost + midBoost
                gf += shadowBoost + midBoost
                bf += shadowBoost + midBoost
            }

            // Snapseed Glamour Glow
            if (state.glamourGlow > 0f) {
                val gg = state.glamourGlow / 100f
                val glow = normalizedLum.pow(1.6f) * gg * 40f
                rf += glow * 1.05f
                gf += glow * 0.98f
                bf += glow * 0.90f
            }

            // Snapseed Drama
            if (state.drama != 0f) {
                val dr = state.drama / 100f
                val delta = (normalizedLum - 0.5f) * dr * 50f
                rf += delta
                gf += delta
                bf += delta
            }

            // Snapseed Vintage
            if (state.vintage > 0f) {
                val vt = state.vintage / 100f
                rf = max(rf, vt * 28f) + vt * 14f
                gf = max(gf, vt * 20f) + vt * 4f
                bf = max(bf, vt * 12f) - vt * 8f
            }

            // Snapseed Grainy Film
            if (state.grain > 0f) {
                val gr = state.grain / 100f
                val px = i % width
                val py = i / width
                val noise = (((px * 13 + py * 79) % 101) / 50f - 1f) * gr * 22f
                rf += noise
                gf += noise
                bf += noise
            }

            // Burhanuddin Crazy Feature: Cyberpunk Neon Matrix
            if (state.cyberpunkNeon > 0f) {
                val cb = state.cyberpunkNeon / 100f
                if (normalizedLum < 0.5f) {
                    val darkWeight = (0.5f - normalizedLum) * 2f
                    bf += cb * darkWeight * 55f
                    gf += cb * darkWeight * 30f
                    rf -= cb * darkWeight * 20f
                } else {
                    val lightWeight = (normalizedLum - 0.5f) * 2f
                    rf += cb * lightWeight * 60f
                    bf += cb * lightWeight * 45f
                    gf -= cb * lightWeight * 15f
                }
            }

            // Photoshop PC Pro: Color Balance (Cyan/Red, Magenta/Green, Yellow/Blue)
            if (state.colorBalanceCyanRed != 0f || state.colorBalanceMagentaGreen != 0f || state.colorBalanceYellowBlue != 0f) {
                rf += state.colorBalanceCyanRed * 0.35f
                gf += state.colorBalanceMagentaGreen * 0.35f
                bf += state.colorBalanceYellowBlue * 0.35f
            }

            // Photoshop PC Pro: Input Levels & Midtone Gamma Stretch
            if (state.levelsBlack > 0f || state.levelsWhite > 0f || state.levelsGamma != 0f) {
                val bIn = state.levelsBlack * 0.7f
                val wIn = 255f - state.levelsWhite * 0.7f
                val invR = if (wIn > bIn) 255f / (wIn - bIn) else 1f
                val gamma = 2.0.pow((-state.levelsGamma / 60.0).toDouble()).toFloat()

                val applyLvl = { v: Float ->
                    val norm = ((v - bIn) * invR).coerceIn(0f, 255f) / 255f
                    norm.pow(gamma) * 255f
                }
                rf = applyLvl(rf)
                gf = applyLvl(gf)
                bf = applyLvl(bf)
            }

            var rClamped = rf.coerceIn(0f, 255f).toInt()
            var gClamped = gf.coerceIn(0f, 255f).toInt()
            var bClamped = bf.coerceIn(0f, 255f).toInt()

            // Tone Curves Evaluation (Master RGB -> Individual Channel)
            if (hasCurves) {
                rClamped = redLut[rgbLut[rClamped]]
                gClamped = greenLut[rgbLut[gClamped]]
                bClamped = blueLut[rgbLut[bClamped]]
            }

            // Defringe (Chromatic Aberration Removal along high-contrast borders)
            if (defringeAmount > 0f) {
                val maxC = max(rClamped, max(gClamped, bClamped))
                val minC = min(rClamped, min(gClamped, bClamped))
                val chroma = maxC - minC
                // Purple fringe: R & B high, G low
                if (rClamped > gClamped + 30 && bClamped > gClamped + 20) {
                    val target = (rClamped + gClamped + bClamped) / 3
                    rClamped = (rClamped + (target - rClamped) * defringeAmount).toInt()
                    bClamped = (bClamped + (target - bClamped) * defringeAmount).toInt()
                }
                // Green fringe: G high, R & B low
                else if (gClamped > rClamped + 35 && gClamped > bClamped + 35) {
                    val target = (rClamped + gClamped + bClamped) / 3
                    gClamped = (gClamped + (target - gClamped) * defringeAmount).toInt()
                }
            }

            // Selective Masking (Linear or Radial gradient weight)
            if (mask != null) {
                val pxNorm = (i % width).toFloat() / width
                val pyNorm = (i / width).toFloat() / height
                val maskWeight = calculateMaskWeight(mask, pxNorm, pyNorm)

                if (maskWeight > 0f) {
                    if (mask.exposure != 0f) {
                        val mExp = 2.0.pow((mask.exposure / 50.0).toDouble()).toFloat()
                        rClamped = (rClamped + (rClamped * mExp - rClamped) * maskWeight).toInt().coerceIn(0, 255)
                        gClamped = (gClamped + (gClamped * mExp - gClamped) * maskWeight).toInt().coerceIn(0, 255)
                        bClamped = (bClamped + (bClamped * mExp - bClamped) * maskWeight).toInt().coerceIn(0, 255)
                    }
                }
            }

            // Saturation & Vibrance & HSL Color Grading
            if (satFactor != 1f || vibranceFactor != 0f || hasHsl) {
                Color.RGBToHSV(rClamped, gClamped, bClamped, hsv)
                var hue = hsv[0]
                var sat = hsv[1]
                var value = hsv[2]

                sat = (sat * satFactor).coerceIn(0f, 1f)
                if (vibranceFactor != 0f) {
                    sat = (sat + vibranceFactor * (1f - sat) * 0.5f).coerceIn(0f, 1f)
                }

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

            // Vignette
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

            // 3-Way Hollywood Color Grading (Cinema Split Toning)
            if (!state.colorGrading.isDefault) {
                val (cgR, cgG, cgB) = applyColorGrading(rClamped, gClamped, bClamped, state.colorGrading)
                rClamped = cgR
                gClamped = cgG
                bClamped = cgB
            }

            // 3D LUT Evaluation
            if (state.customLut != null) {
                val (lutR, lutG, lutB) = applyLut3D(state.customLut, rClamped, gClamped, bClamped, state.lutIntensity)
                rClamped = lutR
                gClamped = lutG
                bClamped = lutB
            }

            // Noir & Black and White
            if (state.noir > 0f || state.blackAndWhite > 0f) {
                val bwAmount = max(state.noir / 100f, state.blackAndWhite / 100f)
                var grey = 0.299f * rClamped + 0.587f * gClamped + 0.114f * bClamped
                if (state.noir > 0f) {
                    val nr = state.noir / 100f
                    grey = if (grey < 70f) grey + (70f - grey) * nr * 0.35f else (grey - 128f) * (1f + nr * 0.4f) + 128f
                }
                rClamped = (rClamped * (1f - bwAmount) + grey * bwAmount).toInt().coerceIn(0, 255)
                gClamped = (gClamped * (1f - bwAmount) + grey * bwAmount).toInt().coerceIn(0, 255)
                bClamped = (bClamped * (1f - bwAmount) + grey * bwAmount).toInt().coerceIn(0, 255)
            }

            // Portrait Face Spotlight
            if (state.portraitSpotlight > 0f) {
                val ps = state.portraitSpotlight / 100f
                val px = (i % width) - centerX
                val py = (i / width) - centerY
                val dist = sqrt(px * px + py * py) / maxDist
                val spot = (1f - dist.toFloat()).coerceIn(0f, 1f).pow(1.5f) * ps * 45f
                rClamped = (rClamped + spot).toInt().coerceIn(0, 255)
                gClamped = (gClamped + spot).toInt().coerceIn(0, 255)
                bClamped = (bClamped + spot).toInt().coerceIn(0, 255)
            }

            pixels[i] = (a shl 24) or (rClamped shl 16) or (gClamped shl 8) or bClamped
        }

        outBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return if (state.sharpness > 0f || state.clarity != 0f) {
            applySharpnessAndClarity(outBitmap, state.sharpness, state.clarity)
        } else {
            outBitmap
        }
    }

    private fun calculateMaskWeight(mask: SelectiveMask, px: Float, py: Float): Float {
        return when (mask.type) {
            MaskType.LINEAR_GRADIENT -> {
                val dx = mask.endX - mask.startX
                val dy = mask.endY - mask.startY
                val len2 = dx * dx + dy * dy
                if (len2 == 0f) 0f else {
                    val t = (((px - mask.startX) * dx + (py - mask.startY) * dy) / len2).coerceIn(0f, 1f)
                    1f - t
                }
            }
            MaskType.RADIAL_GRADIENT -> {
                val rx = (px - mask.centerX) / mask.radiusX
                val ry = (py - mask.centerY) / mask.radiusY
                val dist = sqrt(rx * rx + ry * ry)
                (1f - dist).coerceIn(0f, 1f).pow(1.5f)
            }
        }
    }

    private fun applyColorGrading(
        r: Int, g: Int, b: Int,
        grading: ColorGradingState
    ): Triple<Int, Int, Int> {
        val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        val pivot = (0.5f + grading.balance / 200f).coerceIn(0.1f, 0.9f)
        val blend = (grading.blending / 100f).coerceIn(0.1f, 1f)

        val sWeight = ((pivot + blend * 0.5f - lum) / blend).coerceIn(0f, 1f)
        val hWeight = ((lum - (pivot - blend * 0.5f)) / blend).coerceIn(0f, 1f)
        val mWeight = (1f - abs(lum - pivot) * 2f).coerceIn(0f, 1f)

        var rf = r.toFloat()
        var gf = g.toFloat()
        var bf = b.toFloat()

        if (!grading.shadows.isDefault) {
            val d = com.lumify.editor.gpu.GpuShaderSource.hslToRgbDelta(grading.shadows.hue, grading.shadows.saturation)
            rf += (d[0] * 80f * sWeight) + (grading.shadows.luminance * 0.4f * sWeight)
            gf += (d[1] * 80f * sWeight) + (grading.shadows.luminance * 0.4f * sWeight)
            bf += (d[2] * 80f * sWeight) + (grading.shadows.luminance * 0.4f * sWeight)
        }

        if (!grading.midtones.isDefault) {
            val d = com.lumify.editor.gpu.GpuShaderSource.hslToRgbDelta(grading.midtones.hue, grading.midtones.saturation)
            rf += (d[0] * 80f * mWeight) + (grading.midtones.luminance * 0.4f * mWeight)
            gf += (d[1] * 80f * mWeight) + (grading.midtones.luminance * 0.4f * mWeight)
            bf += (d[2] * 80f * mWeight) + (grading.midtones.luminance * 0.4f * mWeight)
        }

        if (!grading.highlights.isDefault) {
            val d = com.lumify.editor.gpu.GpuShaderSource.hslToRgbDelta(grading.highlights.hue, grading.highlights.saturation)
            rf += (d[0] * 80f * hWeight) + (grading.highlights.luminance * 0.4f * hWeight)
            gf += (d[1] * 80f * hWeight) + (grading.highlights.luminance * 0.4f * hWeight)
            bf += (d[2] * 80f * hWeight) + (grading.highlights.luminance * 0.4f * hWeight)
        }

        return Triple(rf.toInt().coerceIn(0, 255), gf.toInt().coerceIn(0, 255), bf.toInt().coerceIn(0, 255))
    }

    /**
     * Applies customizable photographer watermark (Text or Graphic logo) onto exported bitmap.
     */
    fun applyWatermark(
        source: Bitmap,
        config: WatermarkConfig,
        graphicBitmap: Bitmap? = null
    ): Bitmap {
        if (!config.enabled) return source
        val outBmp = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = android.graphics.Canvas(outBmp)
        val w = outBmp.width.toFloat()
        val h = outBmp.height.toFloat()

        val margin = (config.marginPercent / 100f) * min(w, h)
        val alphaInt = (config.opacity.coerceIn(0.1f, 1f) * 255).toInt()

        if (config.type == WatermarkType.TEXT) {
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                alpha = alphaInt
                textSize = (config.sizePercent / 100f) * (w * 0.25f).coerceAtLeast(24f)
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                setShadowLayer(5f, 2f, 2f, android.graphics.Color.BLACK)
            }

            val textBounds = android.graphics.Rect()
            paint.getTextBounds(config.text, 0, config.text.length, textBounds)
            val textW = textBounds.width().toFloat()
            val textH = textBounds.height().toFloat()

            val (x, y) = when (config.position) {
                WatermarkPosition.BOTTOM_RIGHT -> Pair(w - textW - margin, h - margin)
                WatermarkPosition.BOTTOM_LEFT -> Pair(margin, h - margin)
                WatermarkPosition.TOP_RIGHT -> Pair(w - textW - margin, margin + textH)
                WatermarkPosition.TOP_LEFT -> Pair(margin, margin + textH)
                WatermarkPosition.CENTER -> Pair((w - textW) / 2f, (h + textH) / 2f)
            }
            canvas.drawText(config.text, x, y, paint)
        } else if (config.type == WatermarkType.GRAPHIC && graphicBitmap != null) {
            val targetW = (config.sizePercent / 100f * w).toInt().coerceAtLeast(30)
            val targetH = (targetW.toFloat() / graphicBitmap.width * graphicBitmap.height).toInt().coerceAtLeast(30)
            val scaledGraphic = Bitmap.createScaledBitmap(graphicBitmap, targetW, targetH, true)

            val (gx, gy) = when (config.position) {
                WatermarkPosition.BOTTOM_RIGHT -> Pair(w - targetW - margin, h - targetH - margin)
                WatermarkPosition.BOTTOM_LEFT -> Pair(margin, h - targetH - margin)
                WatermarkPosition.TOP_RIGHT -> Pair(w - targetW - margin, margin)
                WatermarkPosition.TOP_LEFT -> Pair(margin, margin)
                WatermarkPosition.CENTER -> Pair((w - targetW) / 2f, (h - targetH) / 2f)
            }

            val gPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                alpha = alphaInt
            }
            canvas.drawBitmap(scaledGraphic, gx, gy, gPaint)
            scaledGraphic.recycle()
        }

        if (source != outBmp) source.recycle()
        return outBmp
    }

    /**
     * Applies a 3D LUT using fast trilinear interpolation and intensity blending.
     */
    fun applyLut3D(lut: Lut3D, r: Int, g: Int, b: Int, intensity: Float): Triple<Int, Int, Int> {
        val size = lut.size
        val table = lut.table
        val maxIndex = size - 1

        val normR = (r / 255f) * maxIndex
        val normG = (g / 255f) * maxIndex
        val normB = (b / 255f) * maxIndex

        val r0 = normR.toInt().coerceIn(0, maxIndex)
        val r1 = min(maxIndex, r0 + 1)
        val dr = normR - r0

        val g0 = normG.toInt().coerceIn(0, maxIndex)
        val g1 = min(maxIndex, g0 + 1)
        val dg = normG - g0

        val b0 = normB.toInt().coerceIn(0, maxIndex)
        val b1 = min(maxIndex, b0 + 1)
        val db = normB - b0

        fun sample(ri: Int, gi: Int, bi: Int, c: Int): Float {
            val idx = ((bi * size + gi) * size + ri) * 3 + c
            return if (idx < table.size) table[idx] else 0f
        }

        fun trilinear(c: Int): Float {
            val c000 = sample(r0, g0, b0, c)
            val c100 = sample(r1, g0, b0, c)
            val c010 = sample(r0, g1, b0, c)
            val c110 = sample(r1, g1, b0, c)
            val c001 = sample(r0, g0, b1, c)
            val c101 = sample(r1, g0, b1, c)
            val c011 = sample(r0, g1, b1, c)
            val c111 = sample(r1, g1, b1, c)

            val c00 = c000 * (1f - dr) + c100 * dr
            val c10 = c010 * (1f - dr) + c110 * dr
            val c01 = c001 * (1f - dr) + c101 * dr
            val c11 = c011 * (1f - dr) + c111 * dr

            val c0 = c00 * (1f - dg) + c10 * dg
            val c1 = c01 * (1f - dg) + c11 * dg

            return c0 * (1f - db) + c1 * db
        }

        val outR = (trilinear(0) * 255f).coerceIn(0f, 255f)
        val outG = (trilinear(1) * 255f).coerceIn(0f, 255f)
        val outB = (trilinear(2) * 255f).coerceIn(0f, 255f)

        val factor = (intensity / 100f).coerceIn(0f, 1f)
        val finalR = (r * (1f - factor) + outR * factor).toInt()
        val finalG = (g * (1f - factor) + outG * factor).toInt()
        val finalB = (b * (1f - factor) + outB * factor).toInt()

        return Triple(finalR, finalG, finalB)
    }

    /**
     * Parses standard Adobe/DaVinci .cube format LUTs.
     */
    fun parseCubeLut(input: java.io.InputStream, title: String = "Custom LUT"): Lut3D? {
        return try {
            val reader = input.bufferedReader()
            var size = 0
            val entries = mutableListOf<Float>()
            reader.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
                if (trimmed.startsWith("LUT_3D_SIZE")) {
                    size = trimmed.substringAfter("LUT_3D_SIZE").trim().toIntOrNull() ?: 0
                } else if (size > 0) {
                    val parts = trimmed.split("\\s+".toRegex())
                    if (parts.size >= 3) {
                        val r = parts[0].toFloatOrNull()
                        val g = parts[1].toFloatOrNull()
                        val b = parts[2].toFloatOrNull()
                        if (r != null && g != null && b != null) {
                            entries.add(r)
                            entries.add(g)
                            entries.add(b)
                        }
                    }
                }
            }
            if (size <= 1 || entries.size < size * size * size * 3) null
            else Lut3D(title, size, entries.toFloatArray())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds a 256-entry monotonic cubic spline lookup table from control points.
     */
    fun buildCurveLut(points: List<ToneCurvePoint>): IntArray {
        val lut = IntArray(256)
        if (points.isEmpty()) {
            for (i in 0..255) lut[i] = i
            return lut
        }

        val sorted = points.sortedBy { it.x }
        for (i in 0..255) {
            val t = i / 255f
            val y = interpolateSpline(sorted, t)
            lut[i] = (y * 255f).toInt().coerceIn(0, 255)
        }
        return lut
    }

    private fun interpolateSpline(points: List<ToneCurvePoint>, x: Float): Float {
        if (x <= points.first().x) return points.first().y
        if (x >= points.last().x) return points.last().y

        var idx = 0
        while (idx < points.size - 1 && points[idx + 1].x < x) {
            idx++
        }

        val p0 = points[max(0, idx - 1)]
        val p1 = points[idx]
        val p2 = points[idx + 1]
        val p3 = points[min(points.size - 1, idx + 2)]

        val t = (x - p1.x) / (p2.x - p1.x).coerceAtLeast(0.0001f)
        val t2 = t * t
        val t3 = t2 * t

        // Catmull-Rom Spline
        val v = 0.5f * (
            (2f * p1.y) +
            (-p0.y + p2.y) * t +
            (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
            (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
        )
        return v.coerceIn(0f, 1f)
    }

    /**
     * White Balance Eyedropper: Samples a 9x9 pixel patch at the given coordinate
     * and calculates the temperature and tint offsets required to neutralize the color cast.
     */
    fun sampleNeutralWhiteBalance(bitmap: Bitmap, normX: Float, normY: Float): Pair<Float, Float> {
        val cx = (normX * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val cy = (normY * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)

        val radius = 4
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0

        for (dy in -radius..radius) {
            val y = (cy + dy).coerceIn(0, bitmap.height - 1)
            for (dx in -radius..radius) {
                val x = (cx + dx).coerceIn(0, bitmap.width - 1)
                val pixel = bitmap.getPixel(x, y)
                sumR += (pixel shr 16) and 0xFF
                sumG += (pixel shr 8) and 0xFF
                sumB += pixel and 0xFF
                count++
            }
        }

        if (count == 0) return Pair(0f, 0f)

        val avgR = sumR.toFloat() / count
        val avgG = sumG.toFloat() / count
        val avgB = sumB.toFloat() / count

        // Neutral target: Average luminance
        val lum = (avgR + avgG + avgB) / 3f

        // Temperature offset: (Blue - Red) difference normalized
        val deltaTemp = ((avgB - avgR) / 128f * 100f).coerceIn(-100f, 100f)

        // Tint offset: (Green - (Red+Blue)/2) normalized
        val avgRB = (avgR + avgB) / 2f
        val deltaTint = ((avgRB - avgG) / 128f * 100f).coerceIn(-100f, 100f)

        return Pair(deltaTemp, deltaTint)
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

    private fun applyHealing(source: Bitmap, strokes: List<HealingStroke>): Bitmap {
        if (strokes.isEmpty()) return source
        val width = source.width
        val height = source.height
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        val inpaintMask = BooleanArray(width * height)

        // Mark inpaint mask for all strokes
        for (stroke in strokes) {
            val rPx = (stroke.radius * width).toInt().coerceAtLeast(4)
            for (pt in stroke.points) {
                val cx = (pt.x * width).toInt()
                val cy = (pt.y * height).toInt()
                val x0 = max(0, cx - rPx)
                val x1 = min(width - 1, cx + rPx)
                val y0 = max(0, cy - rPx)
                val y1 = min(height - 1, cy + rPx)

                for (y in y0..y1) {
                    val dy = y - cy
                    val yOff = y * width
                    for (x in x0..x1) {
                        val dx = x - cx
                        if (dx * dx + dy * dy <= rPx * rPx) {
                            inpaintMask[yOff + x] = true
                        }
                    }
                }
            }
        }

        // For masked pixels, diffuse / inpaint from nearest unmasked perimeter neighbors
        val tempPixels = pixels.clone()
        val searchR = (width * 0.05f).toInt().coerceIn(6, 40)

        for (y in 0 until height) {
            val yOff = y * width
            for (x in 0 until width) {
                val idx = yOff + x
                if (!inpaintMask[idx]) continue

                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var totalW = 0f

                for (sy in max(0, y - searchR)..min(height - 1, y + searchR) step 2) {
                    val sYOff = sy * width
                    val dy = (sy - y).toFloat()
                    for (sx in max(0, x - searchR)..min(width - 1, x + searchR) step 2) {
                        val sIdx = sYOff + sx
                        if (!inpaintMask[sIdx]) {
                            val dx = (sx - x).toFloat()
                            val d2 = dx * dx + dy * dy
                            if (d2 > 0f) {
                                val w = 1f / d2
                                val p = tempPixels[sIdx]
                                sumR += ((p shr 16) and 0xFF) * w
                                sumG += ((p shr 8) and 0xFF) * w
                                sumB += (p and 0xFF) * w
                                totalW += w
                            }
                        }
                    }
                }

                if (totalW > 0f) {
                    val nr = (sumR / totalW).toInt().coerceIn(0, 255)
                    val ng = (sumG / totalW).toInt().coerceIn(0, 255)
                    val nb = (sumB / totalW).toInt().coerceIn(0, 255)
                    pixels[idx] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
                }
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applySelectiveUPoint(source: Bitmap, points: List<SelectivePoint>): Bitmap {
        val activePoints = points.filter { !it.isDefault }
        if (activePoints.isEmpty()) return source

        val width = source.width
        val height = source.height
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        for (pt in activePoints) {
            val cx = (pt.normX * width).toInt().coerceIn(0, width - 1)
            val cy = (pt.normY * height).toInt().coerceIn(0, height - 1)
            val centerPx = pixels[cy * width + cx]
            val tr = (centerPx shr 16) and 0xFF
            val tg = (centerPx shr 8) and 0xFF
            val tb = centerPx and 0xFF

            val radiusPx = (pt.radius * max(width, height)).coerceAtLeast(10f)
            val radiusSq = radiusPx * radiusPx

            val minX = max(0, (cx - radiusPx).toInt())
            val maxX = min(width - 1, (cx + radiusPx).toInt())
            val minY = max(0, (cy - radiusPx).toInt())
            val maxY = min(height - 1, (cy + radiusPx).toInt())

            val bAmount = pt.brightness * 1.2f
            val cAmount = pt.contrast / 100f
            val sAmount = pt.saturation / 100f

            for (y in minY..maxY) {
                val dy = (y - cy).toFloat()
                val dySq = dy * dy
                val yOff = y * width
                for (x in minX..maxX) {
                    val dx = (x - cx).toFloat()
                    val distSq = dx * dx + dySq
                    if (distSq > radiusSq) continue

                    val idx = yOff + x
                    val px = pixels[idx]
                    val r = (px shr 16) and 0xFF
                    val g = (px shr 8) and 0xFF
                    val b = px and 0xFF

                    // Spatial Gaussian falloff
                    val spatial = (1f - sqrt(distSq) / radiusPx).coerceIn(0f, 1f)

                    // Color similarity (Euclidean distance in RGB space)
                    val dR = (r - tr).toFloat()
                    val dG = (g - tg).toFloat()
                    val dB = (b - tb).toFloat()
                    val colorDist = sqrt(dR * dR + dG * dG + dB * dB)
                    val colorSim = (1f - colorDist / 125f).coerceIn(0f, 1f).pow(1.5f)

                    val weight = spatial * colorSim
                    if (weight <= 0.005f) continue

                    var rf = r.toFloat()
                    var gf = g.toFloat()
                    var bf = b.toFloat()

                    // Brightness
                    if (bAmount != 0f) {
                        rf += bAmount * weight
                        gf += bAmount * weight
                        bf += bAmount * weight
                    }

                    // Contrast
                    if (cAmount != 0f) {
                        val factor = 1f + cAmount * weight
                        rf = (rf - 128f) * factor + 128f
                        gf = (gf - 128f) * factor + 128f
                        bf = (bf - 128f) * factor + 128f
                    }

                    // Saturation
                    if (sAmount != 0f) {
                        val lum = 0.299f * rf + 0.587f * gf + 0.114f * bf
                        val factor = 1f + sAmount * weight
                        rf = lum + (rf - lum) * factor
                        gf = lum + (gf - lum) * factor
                        bf = lum + (bf - lum) * factor
                    }

                    val nr = rf.toInt().coerceIn(0, 255)
                    val ng = gf.toInt().coerceIn(0, 255)
                    val nb = bf.toInt().coerceIn(0, 255)
                    pixels[idx] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
                }
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    fun applyDoubleExposure(
        base: Bitmap,
        secondary: Bitmap?,
        state: DoubleExposureState
    ): Bitmap {
        if (secondary == null || state.opacity <= 0f) return base

        val width = base.width
        val height = base.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Scale secondary to fit base dimensions
        val scaledSecondary = if (secondary.width != width || secondary.height != height) {
            Bitmap.createScaledBitmap(secondary, width, height, true)
        } else {
            secondary
        }

        val basePixels = IntArray(width * height)
        val secPixels = IntArray(width * height)
        val outPixels = IntArray(width * height)

        base.getPixels(basePixels, 0, width, 0, 0, width, height)
        scaledSecondary.getPixels(secPixels, 0, width, 0, 0, width, height)

        val alpha = state.opacity.coerceIn(0f, 1f)
        val invAlpha = 1f - alpha

        for (i in basePixels.indices) {
            val bp = basePixels[i]
            val sp = secPixels[i]

            val bR = ((bp shr 16) and 0xFF) / 255f
            val bG = ((bp shr 8) and 0xFF) / 255f
            val bB = (bp and 0xFF) / 255f

            val sR = ((sp shr 16) and 0xFF) / 255f
            val sG = ((sp shr 8) and 0xFF) / 255f
            val sB = (sp and 0xFF) / 255f

            var blendR: Float
            var blendG: Float
            var blendB: Float

            when (state.blendMode) {
                DoubleExposureBlendMode.NORMAL -> {
                    blendR = sR
                    blendG = sG
                    blendB = sB
                }
                DoubleExposureBlendMode.LIGHTEN -> {
                    blendR = max(bR, sR)
                    blendG = max(bG, sG)
                    blendB = max(bB, sB)
                }
                DoubleExposureBlendMode.DARKEN -> {
                    blendR = min(bR, sR)
                    blendG = min(bG, sG)
                    blendB = min(bB, sB)
                }
                DoubleExposureBlendMode.ADD -> {
                    blendR = min(1f, bR + sR)
                    blendG = min(1f, bG + sG)
                    blendB = min(1f, bB + sB)
                }
                DoubleExposureBlendMode.SUBTRACT -> {
                    blendR = max(0f, bR - sR)
                    blendG = max(0f, bG - sG)
                    blendB = max(0f, bB - sB)
                }
                DoubleExposureBlendMode.SCREEN -> {
                    blendR = 1f - (1f - bR) * (1f - sR)
                    blendG = 1f - (1f - bG) * (1f - sG)
                    blendB = 1f - (1f - bB) * (1f - sB)
                }
                DoubleExposureBlendMode.MULTIPLY -> {
                    blendR = bR * sR
                    blendG = bG * sG
                    blendB = bB * sB
                }
                DoubleExposureBlendMode.OVERLAY -> {
                    blendR = if (bR < 0.5f) 2f * bR * sR else 1f - 2f * (1f - bR) * (1f - sR)
                    blendG = if (bG < 0.5f) 2f * bG * sR else 1f - 2f * (1f - bG) * (1f - sG)
                    blendB = if (bB < 0.5f) 2f * bB * sR else 1f - 2f * (1f - bB) * (1f - sB)
                }
            }

            val finalR = ((bR * invAlpha + blendR * alpha) * 255f).toInt().coerceIn(0, 255)
            val finalG = ((bG * invAlpha + blendG * alpha) * 255f).toInt().coerceIn(0, 255)
            val finalB = ((bB * invAlpha + blendB * alpha) * 255f).toInt().coerceIn(0, 255)

            outPixels[i] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
        }

        if (scaledSecondary != secondary) {
            scaledSecondary.recycle()
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        return output
    }

    suspend fun computeHistogram(bitmap: Bitmap): HistogramData = withContext(Dispatchers.Default) {
        val red = IntArray(256)
        val green = IntArray(256)
        val blue = IntArray(256)
        val luminance = IntArray(256)

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
