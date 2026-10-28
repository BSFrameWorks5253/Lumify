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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.core.EditStackItem
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.AccentRed
import com.lumify.editor.ui.theme.DarkSurface
import com.lumify.editor.ui.theme.DarkSurfaceElevated
import com.lumify.editor.ui.theme.TextMuted
import com.lumify.editor.ui.theme.TextPrimary
import com.lumify.editor.ui.theme.TextSecondary

@Composable
fun SnapseedViewEditsDialog(
    editStack: List<EditStackItem>,
    onDismiss: () -> Unit,
    onDeleteLayer: (Int) -> Unit,
    onRevertAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "View Edits (Stacks)",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                }

                if (editStack.isNotEmpty()) {
                    TextButton(onClick = {
                        onRevertAll()
                        onDismiss()
                    }) {
                        Text("Revert All", color = AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Snapseed non-destructive layer stack. Review, delete, or inspect any past adjustment.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Base Original Layer
                    item {
                        StackLayerTile(
                            title = "Original Photo",
                            summary = "Baseline unedited state",
                            isBase = true,
                            onDelete = null
                        )
                    }

                    // Stack Items in order
                    itemsIndexed(editStack) { index, item ->
                        StackLayerTile(
                            title = item.toolName,
                            summary = item.summary.ifEmpty { "Step ${index + 1}" },
                            isBase = false,
                            onDelete = { onDeleteLayer(index) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurfaceElevated
    )
}

@Composable
fun StackLayerTile(
    title: String,
    summary: String,
    isBase: Boolean,
    onDelete: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isBase) DarkSurface else DarkSurface.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isBase) AccentCyan else TextMuted)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = summary,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Layer",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
