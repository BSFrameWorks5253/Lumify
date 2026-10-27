package com.lumify.editor.gpu

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import com.lumify.editor.core.EditState

/**
 * Hardware-accelerated GPU render manager.
 * On Android 13+ (API 33+), leverages AGSL RuntimeShader to run Exposure,
 * White Balance, Contrast, Shadows/Highlights, and 3-Way Color Wheels directly
 * on the GPU at 120Hz with zero CPU overhead.
 */
object GpuRenderEngine {

    val isGpuAccelerationSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private var runtimeShaderInstance: RuntimeShader? = null

    /**
     * Builds or updates the AGSL RenderEffect for real-time viewport scrubbing.
     */
    fun createGpuRenderEffect(state: EditState, width: Float, height: Float): RenderEffect? {
        if (!isGpuAccelerationSupported) return null
        if (!state.isModified) return null

        return try {
            val shader = getOrCreateShader()
            updateShaderUniforms(shader, state, width, height)
            RenderEffect.createRuntimeShaderEffect(shader, "image")
        } catch (_: Exception) {
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getOrCreateShader(): RuntimeShader {
        var s = runtimeShaderInstance
        if (s == null) {
            s = RuntimeShader(GpuShaderSource.AGSL_STUDIO_SHADER)
            runtimeShaderInstance = s
        }
        return s
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun updateShaderUniforms(shader: RuntimeShader, state: EditState, width: Float, height: Float) {
        shader.setFloatUniform("uResolution", width.coerceAtLeast(1f), height.coerceAtLeast(1f))
        shader.setFloatUniform("uExposure", state.exposure)
        shader.setFloatUniform("uContrast", state.contrast)
        shader.setFloatUniform("uTemperature", state.temperature)
        shader.setFloatUniform("uTint", state.tint)
        shader.setFloatUniform("uHighlights", state.highlights)
        shader.setFloatUniform("uShadows", state.shadows)
        shader.setFloatUniform("uVignette", state.vignette)

        // 3-Way Color Wheels
        val sDelta = GpuShaderSource.hslToRgbDelta(state.colorGrading.shadows.hue, state.colorGrading.shadows.saturation)
        shader.setFloatUniform("uShadowColor", sDelta[0], sDelta[1], sDelta[2])
        shader.setFloatUniform("uShadowLum", state.colorGrading.shadows.luminance)

        val mDelta = GpuShaderSource.hslToRgbDelta(state.colorGrading.midtones.hue, state.colorGrading.midtones.saturation)
        shader.setFloatUniform("uMidtoneColor", mDelta[0], mDelta[1], mDelta[2])
        shader.setFloatUniform("uMidtoneLum", state.colorGrading.midtones.luminance)

        val hDelta = GpuShaderSource.hslToRgbDelta(state.colorGrading.highlights.hue, state.colorGrading.highlights.saturation)
        shader.setFloatUniform("uHighlightColor", hDelta[0], hDelta[1], hDelta[2])
        shader.setFloatUniform("uHighlightLum", state.colorGrading.highlights.luminance)

        shader.setFloatUniform("uBalance", state.colorGrading.balance)
        shader.setFloatUniform("uBlending", state.colorGrading.blending)
    }
}
