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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.ImageMetadata
import com.lumify.editor.ui.components.AmbientLiquidBackdrop
import com.lumify.editor.ui.components.LiquidGlassSurface
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.AccentEmerald
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
    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showTutorialDialog by remember { mutableStateOf(false) }
    var showCrazyLabDialog by remember { mutableStateOf(false) }

    // System Photo picker launcher (accepts any image, RAW, etc.)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    // Subtle breathing pulse animation for the iconic center plus button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    AmbientLiquidBackdrop(enableDrift = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { photoPickerLauncher.launch("image/*") }
        ) {
            // 1. Top Bar (Snapseed Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "OPEN" Button on the left
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(AccentCyan.copy(alpha = 0.15f))
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Open",
                        tint = AccentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OPEN",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Brand Subtitle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SNAPSEED",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp
                    )
                    Text(
                        text = "BY BURHANUDDIN",
                        color = AccentCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                }

                // Right action buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onLaunchCamera() }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = TextPrimary
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = TextPrimary
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(DarkSurfaceElevated)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Gestures & Stack Tutorial", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.School, null, tint = AccentCyan) },
                                onClick = {
                                    showMenu = false
                                    showTutorialDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Burhanuddin's Crazy Lab", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, null, tint = AccentPurple) },
                                onClick = {
                                    showMenu = false
                                    showCrazyLabDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About Snapseed Burhanuddin Edition", color = TextPrimary) },
                                leadingIcon = { Icon(Icons.Default.Info, null, tint = AccentEmerald) },
                                onClick = {
                                    showMenu = false
                                    showAboutDialog = true
                                }
                            )
                        }
                    }
                }
            }

            // 2. Central Iconic Snapseed "+" Button
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    DarkSurfaceElevated,
                                    DarkSurface
                                )
                            )
                        )
                        .border(2.dp, AccentCyan.copy(alpha = 0.5f), CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tap anywhere to open a photo",
                        tint = AccentCyan,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Tap anywhere to open a photo",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "RAW DNG, CR2, NEF, ARW, JPG, PNG",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentEmerald.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Crafted by Burhanuddin • Crazy Features Active",
                        color = AccentEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Bottom Bar: Quick Test Sample Photos Carousel
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "OR TRY A TEST PHOTO (1-TAP INSTANT EDIT)",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SampleChip(
                            title = "Sunset Mountain",
                            subtitle = "HDR DNG RAW",
                            accentColor = AccentCyan,
                            onClick = {
                                val (bmp, meta) = generateSampleProImage()
                                onSampleSelected(bmp, meta)
                            }
                        )
                    }

                    item {
                        SampleChip(
                            title = "Cyberpunk Tokyo",
                            subtitle = "Neon Crazy Lab",
                            accentColor = AccentPurple,
                            onClick = {
                                val (bmp, meta) = generateSampleCyberpunk()
                                onSampleSelected(bmp, meta)
                            }
                        )
                    }

                    item {
                        SampleChip(
                            title = "Studio Portrait",
                            subtitle = "Face Glow RAW",
                            accentColor = AccentEmerald,
                            onClick = {
                                val (bmp, meta) = generateSamplePortrait()
                                onSampleSelected(bmp, meta)
                            }
                        )
                    }

                    item {
                        SampleChip(
                            title = "Modern Architecture",
                            subtitle = "Structure & Detail",
                            accentColor = TextPrimary,
                            onClick = {
                                val (bmp, meta) = generateSampleArchitecture()
                                onSampleSelected(bmp, meta)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs: About, Tutorial, Crazy Lab
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    text = "Snapseed • Burhanuddin Edition",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Developed with ❤️ by Burhanuddin.\n\n" +
                               "Faithfully recreating Google Snapseed's legendary gesture-based photo editing engine while integrating cutting-edge pro features:\n\n" +
                               "• Pure vertical swipe to choose parameter\n" +
                               "• Smooth horizontal swipe to adjust values\n" +
                               "• Snapseed Stacks / View Edits history system\n" +
                               "• 20+ Iconic Tools: Tune Image, Details, Curves, White Balance, Glamour Glow, Drama, Vintage, Grainy Film, Retrolux, Noir, Portrait, Lens Blur\n" +
                               "• Burhanuddin Crazy Features: Cyberpunk Neon Matrix, Cinema 3-Way Color Wheels, Live Zebra Waveform, and Artist Signature Stamp.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Awesome", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    if (showTutorialDialog) {
        AlertDialog(
            onDismissRequest = { showTutorialDialog = false },
            title = {
                Text("How to Edit Like Snapseed", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "1. Swipe Up & Down:\n" +
                               "Selects the parameter (e.g. Brightness, Contrast, Saturation, Ambiance, Highlights, Shadows, Warmth).\n\n" +
                               "2. Swipe Left & Right:\n" +
                               "Adjusts the intensity of the active parameter (-100 to +100).\n\n" +
                               "3. Press & Hold Canvas:\n" +
                               "Instantly compare with your unedited original photo!\n\n" +
                               "4. Stacks (View Edits):\n" +
                               "Tap the layers icon at top to view past edit layers, adjust any tool retroactively, or revert.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTutorialDialog = false }) {
                    Text("Got it", color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    if (showCrazyLabDialog) {
        AlertDialog(
            onDismissRequest = { showCrazyLabDialog = false },
            title = {
                Text("Burhanuddin's Crazy Lab", fontWeight = FontWeight.Bold, color = AccentPurple)
            },
            text = {
                Column {
                    Text(
                        text = "Supercharge your photos with exclusive features beyond standard Snapseed:\n\n" +
                               "🔥 Cyberpunk Neon Bloom: Sci-fi electric blues and hot magenta highlights.\n" +
                               "🎬 Cinema 3-Way Color Wheels: Hollywood Lift, Gamma & Gain split toning.\n" +
                               "📊 False Color & Zebra Stripes: High-end camera exposure clipping diagnostics.\n" +
                               "✍️ Burhanuddin Artist Signature: Custom watermark overlay for your photography.\n" +
                               "🔍 8x Precision Loupe: Inspect individual pixel colors with live RGB readout.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCrazyLabDialog = false }) {
                    Text("Let's Create!", color = AccentPurple, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

@Composable
fun SampleChip(
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    LiquidGlassSurface(
        modifier = Modifier
            .clickable { onClick() }
            .width(155.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = DarkSurface.copy(alpha = 0.85f),
        borderWidth = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Generates an HDR Sunset Mountain sample bitmap with rich color information
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
                Color.rgb(20, 30, 85),
                Color.rgb(180, 50, 90),
                Color.rgb(255, 120, 40),
                Color.rgb(255, 210, 80)
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

    // Mountain Silhouette
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

    // Lake Reflection
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
        dateTime = "2026:10:28 08:30:00",
        isRaw = true
    )

    return Pair(bitmap, metadata)
}

/**
 * Generates Cyberpunk Tokyo night sample photo
 */
fun generateSampleCyberpunk(): Pair<Bitmap, ImageMetadata> {
    val width = 1200
    val height = 900
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Dark moody city night background
    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            Color.rgb(10, 8, 20),
            Color.rgb(5, 5, 12),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Neon Cyan glow
    val cyanGlow = Paint().apply {
        shader = RadialGradient(
            width * 0.3f, height * 0.4f, 300f,
            intArrayOf(Color.rgb(0, 245, 255), Color.argb(80, 0, 180, 220), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(width * 0.3f, height * 0.4f, 300f, cyanGlow)

    // Neon Magenta glow
    val magentaGlow = Paint().apply {
        shader = RadialGradient(
            width * 0.75f, height * 0.45f, 320f,
            intArrayOf(Color.rgb(255, 0, 128), Color.argb(90, 200, 0, 150), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(width * 0.75f, height * 0.45f, 320f, magentaGlow)

    // Wet street reflection
    val streetPaint = Paint().apply {
        shader = LinearGradient(
            0f, height * 0.65f, 0f, height.toFloat(),
            intArrayOf(Color.rgb(30, 10, 35), Color.rgb(8, 15, 25)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, height * 0.65f, width.toFloat(), height.toFloat(), streetPaint)

    val metadata = ImageMetadata(
        format = "ARW (Sony RAW)",
        width = width,
        height = height,
        cameraMake = "Sony",
        cameraModel = "Alpha 1",
        lensModel = "FE 50mm F1.2 GM",
        focalLength = "50 mm",
        aperture = "f/1.2",
        iso = "800",
        shutterSpeed = "1/125 s",
        dateTime = "2026:10:28 21:15:00",
        isRaw = true
    )

    return Pair(bitmap, metadata)
}

/**
 * Generates Studio Portrait sample
 */
fun generateSamplePortrait(): Pair<Bitmap, ImageMetadata> {
    val width = 900
    val height = 1200
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Soft warm background
    val bgPaint = Paint().apply {
        shader = RadialGradient(
            width * 0.5f, height * 0.4f, 600f,
            intArrayOf(Color.rgb(90, 65, 55), Color.rgb(25, 18, 15)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Warm Key Light (Beauty Dish)
    val keyLight = Paint().apply {
        shader = RadialGradient(
            width * 0.5f, height * 0.45f, 260f,
            intArrayOf(Color.rgb(255, 215, 185), Color.rgb(210, 150, 120), Color.TRANSPARENT),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(width * 0.5f, height * 0.45f, 260f, keyLight)

    val metadata = ImageMetadata(
        format = "CR3 (Canon RAW)",
        width = width,
        height = height,
        cameraMake = "Canon",
        cameraModel = "EOS R5",
        lensModel = "RF 85mm F1.2 L USM",
        focalLength = "85 mm",
        aperture = "f/1.4",
        iso = "100",
        shutterSpeed = "1/200 s",
        dateTime = "2026:10:28 14:00:00",
        isRaw = true
    )

    return Pair(bitmap, metadata)
}

/**
 * Generates High Structure Architecture sample
 */
fun generateSampleArchitecture(): Pair<Bitmap, ImageMetadata> {
    val width = 1200
    val height = 900
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Clear sky
    val skyPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 0f, height * 0.5f,
            Color.rgb(40, 80, 140),
            Color.rgb(140, 180, 220),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height * 0.5f, skyPaint)

    // Structural concrete pillars and diagonal lines
    val buildingPaint = Paint().apply {
        color = Color.rgb(210, 210, 215)
        isAntiAlias = true
    }
    for (i in 0..8) {
        val x = i * 150f
        canvas.drawRect(x, height * 0.35f, x + 85f, height.toFloat(), buildingPaint)
    }

    val metadata = ImageMetadata(
        format = "NEF (Nikon RAW)",
        width = width,
        height = height,
        cameraMake = "Nikon",
        cameraModel = "Z8",
        lensModel = "NIKKOR Z 14-24mm f/2.8 S",
        focalLength = "18 mm",
        aperture = "f/8.0",
        iso = "64",
        shutterSpeed = "1/500 s",
        dateTime = "2026:10:28 11:45:00",
        isRaw = true
    )

    return Pair(bitmap, metadata)
}
