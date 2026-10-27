package com.juragan38.pos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Blue200,
    secondary = Blue500,
    tertiary = Amber500,
    background = SurfaceLight,
    surface = androidx.compose.ui.graphics.Color.White,
    error = Red600
)

private val DarkScheme = darkColorScheme(
    primary = Blue200,
    onPrimary = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = Blue700,
    secondary = Blue500,
    tertiary = Amber500,
    background = DarkSurface,
    error = Red600
)

@Composable
fun Juragan38Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = JuraganTypography,
        content = content
    )
}
