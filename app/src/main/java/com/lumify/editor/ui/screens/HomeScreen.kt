package com.lumify.editor.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.ImageMetadata
import com.lumify.editor.ui.components.LiquidGlassButton
import com.lumify.editor.ui.components.LiquidGlassSurface
import com.lumify.editor.ui.components.LumifyLogoTile
import com.lumify.editor.ui.theme.AccentAmber
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.AccentPurple
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.DarkSurface
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onImageSelected: (Uri) -> Unit,
    onSampleSelected: (Bitmap, ImageMetadata) -> Unit,
    onLaunchCamera: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Photo picker launcher (accepts any image, RAW, etc.)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Adobe-style Lumify App Tile Logo with Aperture Iris
            LumifyLogoTile(size = 84.dp)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "LUMIFY",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )

            Text(
                text = "Master RAW & Photo Editor",
                color = AccentCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Main Action Card: Open from Gallery / Files
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { photoPickerLauncher.launch("*/*") },
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkSurface.copy(alpha = 0.85f),
                borderWidth = 1.5.dp
            ) {
                Row(
                    modifier = Modifier.padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(AccentCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Open Photo",
                            tint = AccentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Open Photo / RAW",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Select any DNG, CR2, NEF, ARW, JPG, or PNG",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Action: Test with Sample RAW Landscape
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val (sampleBmp, sampleMeta) = generateSampleProImage()
                        onSampleSelected(sampleBmp, sampleMeta)
                    },
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkSurfaceElevated.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AccentAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Sample Photo",
                            tint = AccentAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Load Sample Pro RAW",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Test instant HDR landscape with live HSL color grading",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tertiary Action: Capture Photo with Camera
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLaunchCamera() },
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkSurfaceElevated.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AccentPurple.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = AccentPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Capture with CameraX",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Shoot photos directly into the editing studio",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Feature Highlights
            Text(
                text = "ENGINE CAPABILITIES",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureItem(
                icon = Icons.Default.Tune,
                title = "32-Bit Floating Point Color Pipeline",
                description = "Non-destructive exposure, shadows/highlights recovery, and Kelvin white balance."
            )

            FeatureItem(
                icon = Icons.Default.Diamond,
                title = "8-Channel HSL Color Grading",
                description = "Independent control of Hue, Saturation, and Luminance for Red, Orange, Yellow, Green, Cyan, Blue, Purple, Magenta."
            )

            FeatureItem(
                icon = Icons.Default.AutoAwesome,
                title = "Liquid Glass Interface",
                description = "Hardware-accelerated frosted glass panels with specular rim lighting and tactile haptic controls."
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Generates an HDR Sunset Mountain sample bitmap with rich color information
 * so the user can immediately test all editing controls without needing local files.
 */
fun generateSampleProImage(): Pair<Bitmap, ImageMetadata> {
    val width = 1200
    val height = 900
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Sky Gradient (Sunset Golden Hour)
    val skyPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, height * 0.65f,
            intArrayOf(
                Color.rgb(20, 30, 85),   // Deep Indigo Night Sky
                Color.rgb(180, 50, 90),  // Magenta Sunset
                Color.rgb(255, 120, 40), // Vivid Orange
                Color.rgb(255, 210, 80)  // Golden Sun Horizon
            ),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height * 0.65f, skyPaint)

    // Glowing Sun
    val sunPaint = Paint().apply {
        shader = RadialGradient(
            width * 0.55f, height * 0.45f, 160f,
            intArrayOf(Color.WHITE, Color.argb(200, 255, 200, 80), Color.TRANSPARENT),
            floatArrayOf(0f, 0.4f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(width * 0.55f, height * 0.45f, 160f, sunPaint)

    // Distant Mountain Silhouette
    val mountainPaint = Paint().apply {
        shader = LinearGradient(
            0f, height * 0.4f, 0f, height.toFloat(),
            Color.rgb(45, 25, 60),
            Color.rgb(15, 12, 25),
            Shader.TileMode.CLAMP
        )
        isAntiAlias = true
    }
    val mountainPath = android.graphics.Path().apply {
        moveTo(0f, height * 0.65f)
        lineTo(width * 0.2f, height * 0.42f)
        lineTo(width * 0.45f, height * 0.52f)
        lineTo(width * 0.68f, height * 0.36f)
        lineTo(width * 0.88f, height * 0.50f)
        lineTo(width.toFloat(), height * 0.40f)
        lineTo(width.toFloat(), height.toFloat())
        lineTo(0f, height.toFloat())
        close()
    }
    canvas.drawPath(mountainPath, mountainPaint)

    // Foreground Lake Reflection
    val lakePaint = Paint().apply {
        shader = LinearGradient(
            0f, height * 0.65f, 0f, height.toFloat(),
            intArrayOf(
                Color.rgb(120, 60, 50),
                Color.rgb(30, 20, 40),
                Color.rgb(10, 8, 18)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, height * 0.65f, width.toFloat(), height.toFloat(), lakePaint)

    val metadata = ImageMetadata(
        format = "DNG (RAW)",
        width = width,
        height = height,
        cameraMake = "Sony",
        cameraModel = "ILCE-7RM5",
        lensModel = "FE 24-70mm F2.8 GM II",
        focalLength = "35 mm",
        aperture = "f/2.8",
        iso = "100",
        shutterSpeed = "1/250 s",
        dateTime = "2026:10:27 08:30:00",
        isRaw = true
    )

    return Pair(bitmap, metadata)
}
