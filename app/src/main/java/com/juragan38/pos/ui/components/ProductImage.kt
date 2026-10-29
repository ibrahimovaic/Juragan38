package com.juragan38.pos.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Menampilkan gambar produk dari:
 * - data URI base64 (hasil pilih galeri, ikut tersinkron ke HP lain)
 * - URL https
 * - kosong -> placeholder
 */
@Composable
fun ProductImage(
    image: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    corner: Dp = 10.dp
) {
    val shape = RoundedCornerShape(corner)
    when {
        image.startsWith("data:") -> {
            val bitmap = remember(image) { decodeBase64(image) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    modifier = modifier.clip(shape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Placeholder(modifier, shape)
            }
        }

        image.startsWith("http", ignoreCase = true) -> {
            AsyncImage(
                model = image,
                contentDescription = contentDescription,
                modifier = modifier.clip(shape),
                contentScale = ContentScale.Crop
            )
        }

        else -> Placeholder(modifier, shape)
    }
}

@Composable
private fun Placeholder(modifier: Modifier, shape: RoundedCornerShape) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingBag,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
    }
}

private fun decodeBase64(data: String): ImageBitmap? = try {
    val payload = data.substringAfter("base64,", data)
    val bytes = Base64.decode(payload, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
} catch (e: Exception) {
    null
}
