package com.lumify.editor.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.lumify.editor.core.EditState
import com.lumify.editor.presets.FilterPreset
import com.lumify.editor.presets.QrPresetManager
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary

@Composable
fun QrPresetShareDialog(
    presetName: String,
    editState: EditState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(presetName, editState) {
        qrBitmap = QrPresetManager.generateQrBitmap(presetName, editState, 450)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode2, null, tint = AccentCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Offline Recipe Share", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Scan with Snapseed by Burhanuddin or share the .lumify file to import this look offline.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // QR Code Container
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    qrBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Offline Recipe QR Code",
                            modifier = Modifier.size(196.dp)
                        )
                    } ?: Text("Generating QR...", color = Color.Black)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Recipe: $presetName",
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: Share .lumify file
                Button(
                    onClick = {
                        val file = QrPresetManager.exportLumifyFile(context, presetName, editState)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Snapseed Look: $presetName")
                            putExtra(Intent.EXTRA_TEXT, "Here is my custom '$presetName' preset for Snapseed by Burhanuddin!")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share .lumify preset"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share .lumify File", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextMuted)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}

@Composable
fun QrPresetImportDialog(
    onDismiss: () -> Unit,
    onImportSuccess: (FilterPreset) -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Picker for QR screenshot image
    val qrImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bmp = android.graphics.BitmapFactory.decodeStream(inputStream)
                val preset = bmp?.let { QrPresetManager.decodeQrBitmap(it) }
                if (preset != null) {
                    onImportSuccess(preset)
                } else {
                    errorMessage = "No valid recipe QR code detected in photo."
                }
            } catch (e: Exception) {
                errorMessage = "Could not read QR image: ${e.localizedMessage}"
            }
        }
    }

    // Picker for .lumify / JSON file
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val preset = stream?.let { QrPresetManager.importFromStream(it) }
                if (preset != null) {
                    onImportSuccess(preset)
                } else {
                    errorMessage = "Invalid .lumify recipe file."
                }
            } catch (e: Exception) {
                errorMessage = "Could not read preset file: ${e.localizedMessage}"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FileDownload, null, tint = AccentCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import Preset Offline", fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Import looks created by other photographers using a QR screenshot or .lumify file.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = { qrImagePicker.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCode2, null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan QR from Gallery Photo", color = TextPrimary)
                }

                OutlinedButton(
                    onClick = { filePicker.launch("*/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, null, tint = AccentCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open .lumify Preset File", color = TextPrimary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}
