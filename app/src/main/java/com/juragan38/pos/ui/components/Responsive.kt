package com.juragan38.pos.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Lebar layar >= 840dp dianggap tablet / landscape lebar. */
@Composable
fun isWideScreen(minWidthDp: Int = 840): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp >= minWidthDp
}

/** Lebar layar >= 600dp (tablet kecil). */
@Composable
fun isMediumScreen(minWidthDp: Int = 600): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp >= minWidthDp
}

/**
 * Membatasi lebar konten supaya di tablet tidak melebar penuh
 * (form dan daftar jadi lebih enak dibaca).
 */
@Composable
fun CenteredContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 1000.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
