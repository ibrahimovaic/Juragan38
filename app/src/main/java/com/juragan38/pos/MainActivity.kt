package com.juragan38.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.juragan38.pos.data.ThemeMode
import com.juragan38.pos.ui.AppViewModel
import com.juragan38.pos.ui.components.AppWatermark
import com.juragan38.pos.ui.navigation.JuraganNavHost
import com.juragan38.pos.ui.theme.Juragan38Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: AppViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val settings by viewModel.settings.collectAsState()
            val session by viewModel.session.collectAsState()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }

            Juragan38Theme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        JuraganNavHost(viewModel = viewModel)

                        // Watermark hanya muncul setelah login, menimpa seluruh layar.
                        if (session != null) {
                            AppWatermark(
                                enabled = settings.watermarkEnabled,
                                text = settings.watermarkText,
                                opacity = settings.watermarkOpacity
                            )
                        }
                    }
                }
            }
        }
    }
}
