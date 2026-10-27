package com.lumify.editor.gpu

import android.graphics.Color
import com.lumify.editor.core.ColorGradingState
import com.lumify.editor.core.EditState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance AGSL (Android Graphics Shading Language) & GLSL shader sources.
 * Pushes Exposure, Contrast, White Balance, Shadows/Highlights, 3-Way Color Wheels,
 * and Vignette directly to the Adreno / Mali GPU.
 */
object GpuShaderSource {

    const val AGSL_STUDIO_SHADER = """
        uniform shader image;
        uniform float2 uResolution;
        uniform float uExposure;
        uniform float uContrast;
        uniform float uTemperature;
        uniform float uTint;
        uniform float uHighlights;
        uniform float uShadows;
        uniform float uVignette;
        
        // 3-Way Color Grading
        uniform vec3 uShadowColor;
        uniform float uShadowLum;
        uniform vec3 uMidtoneColor;
        uniform float uMidtoneLum;
        uniform vec3 uHighlightColor;
        uniform float uHighlightLum;
        uniform float uBalance;
        uniform float uBlending;

        vec3 hsv2rgb(vec3 c) {
            vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
            vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
            return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
        }

        vec4 main(vec2 coord) {
            vec4 color = image.eval(coord);
            if (color.a == 0.0) return color;

            vec3 rgb = color.rgb;

            // 1. Exposure (EV scale)
            float exposureMult = exp2(uExposure / 50.0);
            rgb *= exposureMult;

            // 2. White Balance (Temperature & Tint)
            rgb.r += (uTemperature * 0.15) + (uTint * 0.08);
            rgb.g -= (uTint * 0.12);
            rgb.b -= (uTemperature * 0.15) - (uTint * 0.08);

            // 3. Contrast (S-Curve pivot around 0.5)
            if (uContrast != 0.0) {
                float c = uContrast / 100.0;
                rgb = (rgb - 0.5) * (1.0 + c) + 0.5;
            }

            // 4. Shadows & Highlights (Luminance weighting)
            float lum = dot(rgb, vec3(0.299, 0.587, 0.114));
            
            // Shadows weight: concentrated in darks
            float shadowWeight = pow(clamp(1.0 - lum, 0.0, 1.0), 1.8);
            rgb += uShadows * 0.003 * shadowWeight;

            // Highlights weight: concentrated in brights
            float highlightWeight = pow(clamp(lum, 0.0, 1.0), 1.8);
            rgb += uHighlights * 0.003 * highlightWeight;

            // 5. 3-Way Cinema Color Grading (Shadows, Midtones, Highlights)
            float pivot = clamp(0.5 + (uBalance / 200.0), 0.1, 0.9);
            float blendRange = clamp(uBlending / 100.0, 0.1, 1.0);

            float sWeight = smoothstep(pivot + blendRange * 0.5, pivot - blendRange * 0.5, lum);
            float hWeight = smoothstep(pivot - blendRange * 0.5, pivot + blendRange * 0.5, lum);
            float mWeight = 1.0 - abs(lum - pivot) * 2.0;
            mWeight = clamp(mWeight, 0.0, 1.0);

            // Apply tint & luminance shifts per tone range
            rgb += uShadowColor * sWeight * 0.35 + vec3(uShadowLum * 0.002 * sWeight);
            rgb += uMidtoneColor * mWeight * 0.35 + vec3(uMidtoneLum * 0.002 * mWeight);
            rgb += uHighlightColor * hWeight * 0.35 + vec3(uHighlightLum * 0.002 * hWeight);

            // 6. Vignette
            if (uVignette > 0.0) {
                vec2 normCoord = coord / uResolution - 0.5;
                float dist = length(normCoord) * 1.414;
                if (dist > 0.35) {
                    float vig = clamp(1.0 - pow((dist - 0.35) / 0.65, 1.5) * (uVignette / 100.0), 0.0, 1.0);
                    rgb *= vig;
                }
            }

            return vec4(clamp(rgb, 0.0, 1.0), color.a);
        }
    """

    fun hslToRgbDelta(hue: Float, saturation: Float): FloatArray {
        if (saturation == 0f) return floatArrayOf(0f, 0f, 0f)
        val hsv = floatArrayOf(hue, saturation / 100f, 1f)
        val colorInt = Color.HSVToColor(hsv)
        val r = (Color.red(colorInt) / 255f) - 0.5f
        val g = (Color.green(colorInt) / 255f) - 0.5f
        val b = (Color.blue(colorInt) / 255f) - 0.5f
        return floatArrayOf(r, g, b)
    }
}
