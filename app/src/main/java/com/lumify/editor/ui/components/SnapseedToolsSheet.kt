package com.lumify.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.ui.screens.SnapseedTool
import com.lumify.editor.ui.screens.ToolCategory
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
fun SnapseedToolsSheet(
    onDismiss: () -> Unit,
    onSelectTool: (SnapseedTool) -> Unit
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
                .fillMaxHeight(0.88f)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOOLS",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tap any tool to start editing",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Categorized Tool Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                ToolCategory.values().forEach { category ->
                    val toolsInCategory = SnapseedTool.values().filter { it.category == category }
                    if (toolsInCategory.isNotEmpty()) {
                        item(span = { GridItemSpan(4) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 18.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val catColor = when (category) {
                                    ToolCategory.ESSENTIALS -> AccentCyan
                                    ToolCategory.TRANSFORM -> AccentEmerald
                                    ToolCategory.CREATIVE -> TextPrimary
                                    ToolCategory.PHOTOSHOP_PRO -> Color(0xFF38BDF8) // Photoshop Pro Blue
                                    ToolCategory.BURHANUDDIN_CRAZY -> AccentPurple
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category.title.uppercase(),
                                    color = catColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }

                        items(toolsInCategory) { tool ->
                            SnapseedToolGridTile(
                                tool = tool,
                                onClick = {
                                    onSelectTool(tool)
                                }
                            )
                        }
                    }
                }

                item(span = { GridItemSpan(4) }) {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
fun SnapseedToolGridTile(
    tool: SnapseedTool,
    onClick: () -> Unit
) {
    val isCrazy = (tool.category == ToolCategory.BURHANUDDIN_CRAZY)
    val isPhotoshop = (tool.category == ToolCategory.PHOTOSHOP_PRO)
    val accent = when {
        isCrazy -> AccentPurple
        isPhotoshop -> Color(0xFF38BDF8)
        else -> AccentCyan
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCrazy -> AccentPurple.copy(alpha = 0.16f)
                        isPhotoshop -> Color(0xFF38BDF8).copy(alpha = 0.16f)
                        else -> DarkSurfaceElevated
                    }
                )
                .border(
                    1.dp,
                    if (isCrazy || isPhotoshop) accent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f),
                    CircleShape
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.title,
                tint = if (isCrazy || isPhotoshop) accent else TextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = tool.title,
            color = if (isCrazy || isPhotoshop) accent else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (isCrazy || isPhotoshop) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 14.sp
        )
    }
}
