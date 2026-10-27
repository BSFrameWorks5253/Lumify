package com.lumify.editor.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.R
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Cinematic Aperture Iris Dilate opening animation:
 * 1. 6 mechanical camera aperture iris blades rotate and dilate open from center (f/22 -> f/1.4).
 * 2. Anamorphic glass optical focal glow flares through the opening pupil.
 * 3. Lumify emblem settles into tack-sharp focus with spring physics.
 * 4. Premium typography emerges cleanly before transitioning into the editor.
 */
@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit
) {
    // Animation drivers
    val irisDilateProgress = remember { Animatable(0f) }      // 0f (closed/f22) -> 1f (wide open/f1.4)
    val irisRotation = remember { Animatable(-50f) }          // Mechanical blade rotation
    val opticalFocalGlow = remember { Animatable(0f) }        // Light flare through pupil
    val emblemScale = remember { Animatable(0.78f) }          // Logo focus scale
    val emblemAlpha = remember { Animatable(0f) }             // Logo reveal
    val emblemBlur = remember { Animatable(12f) }             // Depth-of-field rack focus (px)
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        // Step 1: Aperture blades mechanical iris dilation & rotation (550ms)
        launch {
            irisRotation.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
        launch {
            irisDilateProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }

        // Step 2: Optical flare bursts as iris opens, then settles
        launch {
            opticalFocalGlow.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing)
            )
            opticalFocalGlow.animateTo(
                targetValue = 0.25f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        }

        // Step 3: Lumify logo rack-focus into tack-sharp sharpness
        delay(180)
        launch {
            emblemAlpha.animateTo(1f, tween(300))
        }
        launch {
            emblemBlur.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        }
        emblemScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )

        // Step 4: Typography reveal
        launch {
            textAlpha.animateTo(1f, tween(300))
        }
        textOffsetY.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 320f))

        // Brief hold to absorb the cinematic visual, then proceed
        delay(650)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val stageSize = 160.dp

            Box(
                modifier = Modifier
                    .size(stageSize),
                contentAlignment = Alignment.Center
            ) {
                // Optical focal glow background canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val glowRadius = size.minDimension * 0.7f * opticalFocalGlow.value

                    if (glowRadius > 0f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AccentCyan.copy(alpha = 0.45f * opticalFocalGlow.value),
                                    AccentCyan.copy(alpha = 0.15f * opticalFocalGlow.value),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = glowRadius
                            ),
                            radius = glowRadius,
                            center = center
                        )
                    }
                }

                // 6-Blade Mechanical Camera Aperture Iris Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerRadius = size.minDimension / 2f - 4.dp.toPx()
                    val bladeCount = 6

                    // Outer Lens Barrel Bezel Ring
                    drawCircle(
                        color = Color(0xFF1E222D),
                        radius = outerRadius + 2.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    drawCircle(
                        color = AccentCyan.copy(alpha = 0.6f * irisDilateProgress.value),
                        radius = outerRadius + 4.dp.toPx(),
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Draw 6 mechanical aperture blades rotating and dilating
                    rotate(degrees = irisRotation.value, pivot = center) {
                        val progress = irisDilateProgress.value
                        val innerPupilRadius = outerRadius * (0.12f + 0.72f * progress)

                        for (i in 0 until bladeCount) {
                            val baseAngle = (i * 360f / bladeCount) * (PI.toFloat() / 180f)
                            val nextAngle = ((i + 1) * 360f / bladeCount) * (PI.toFloat() / 180f)

                            val p1 = Offset(
                                center.x + outerRadius * cos(baseAngle),
                                center.y + outerRadius * sin(baseAngle)
                            )
                            val p2 = Offset(
                                center.x + outerRadius * cos(nextAngle),
                                center.y + outerRadius * sin(nextAngle)
                            )

                            // Inner vertex sliding out to dilate aperture pupil
                            val irisTangentAngle = baseAngle + (0.55f + 0.35f * (1f - progress))
                            val pIris = Offset(
                                center.x + innerPupilRadius * cos(irisTangentAngle),
                                center.y + innerPupilRadius * sin(irisTangentAngle)
                            )

                            val bladePath = Path().apply {
                                moveTo(center.x, center.y)
                                lineTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                lineTo(pIris.x, pIris.y)
                                close()
                            }

                            // Matte graphite metallic blade with subtle bevel edge
                            val bladeColor = if (i % 2 == 0) Color(0xFF111319) else Color(0xFF181B24)
                            drawPath(
                                path = bladePath,
                                color = bladeColor.copy(alpha = (1f - progress * 0.85f).coerceIn(0f, 1f)),
                                style = Fill
                            )

                            // Blade edge line
                            drawPath(
                                path = bladePath,
                                color = Color(0xFF282E3E).copy(alpha = (1f - progress * 0.7f).coerceIn(0f, 1f)),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }

                // Inner Tack-Sharp Lumify Emblem (Revealed through open aperture)
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(emblemScale.value)
                        .alpha(emblemAlpha.value)
                        .blur(emblemBlur.value.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(26.dp),
                            spotColor = AccentCyan.copy(alpha = 0.4f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_lumify_logo),
                        contentDescription = "Lumify Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(textAlpha.value)
            ) {
                Text(
                    text = "LUMIFY",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Master RAW & Photo Editor",
                    color = AccentCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
            }
        }
    }
}
