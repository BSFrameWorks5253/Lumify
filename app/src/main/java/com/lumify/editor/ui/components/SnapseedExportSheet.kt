package com.lumify.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.AccentEmerald
import com.lumify.editor.ui.theme.AccentPurple
import com.lumify.editor.ui.theme.DarkBackground
import com.lumify.editor.ui.theme.DarkSurface
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnapseedExportSheet(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSaveCopy: () -> Unit,
    onOpenExportDialog: () -> Unit,
    onExportWithSignature: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 16.dp, horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EXPORT",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Options
            ExportOptionTile(
                icon = Icons.Default.Share,
                title = "Share",
                subtitle = "Share photo with apps or friends directly",
                accentColor = AccentCyan,
                onClick = {
                    onDismiss()
                    onShare()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ExportOptionTile(
                icon = Icons.Default.Save,
                title = "Save",
                subtitle = "Creates a high-quality copy of your photo in the gallery",
                accentColor = AccentEmerald,
                onClick = {
                    onDismiss()
                    onSaveCopy()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ExportOptionTile(
                icon = Icons.Default.Download,
                title = "Export (Custom Quality & Format)",
                subtitle = "Change JPEG / PNG / WEBP format, quality sizing",
                accentColor = AccentPurple,
                onClick = {
                    onDismiss()
                    onOpenExportDialog()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            ExportOptionTile(
                icon = Icons.Default.BrandingWatermark,
                title = "Export with Burhanuddin Signature",
                subtitle = "Applies elegant artist signature stamp",
                accentColor = AccentCyan,
                onClick = {
                    onDismiss()
                    onExportWithSignature()
                }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ExportOptionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}
