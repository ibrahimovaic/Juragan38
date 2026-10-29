package com.juragan38.pos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Watermark transparan yang menimpa seluruh layar aplikasi.
 * Tidak menangkap sentuhan karena hanya berisi teks.
 */
@Composable
fun AppWatermark(
    enabled: Boolean,
    text: String,
    opacity: Float
) {
    if (!enabled) return
    val label = text.ifBlank { "JURAGAN 38" }
    val safeOpacity = opacity.coerceIn(0.02f, 0.25f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .scale(1.9f)
            .rotate(-24f)
            .alpha(safeOpacity)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(12) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(3) {
                        Text(
                            text = label,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
