package com.lumify.editor.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumify.editor.R
import com.lumify.editor.ui.theme.AccentCyan
import com.lumify.editor.ui.theme.TextPrimary

/**
 * Adobe Creative Cloud-style app tile logo for Lumify.
 * Features a solid matte charcoal squircle, bold chunky cyan border,
 * fat 'Lu' typography, and a mechanical camera aperture shutter.
 */
@Composable
fun LumifyLogoTile(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    val cornerRadius = size * 0.22f

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFF15171E))
            .border(
                width = 2.8.dp,
                color = AccentCyan,
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_lumify_logo),
            contentDescription = "Lumify Bold Matte Logo",
            modifier = Modifier.size(size * 0.85f)
        )
    }
}

@Composable
fun LumifyBrandHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LumifyLogoTile(size = 48.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Box {
            Text(
                text = "LUMIFY",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.5.sp
            )
        }
    }
}
