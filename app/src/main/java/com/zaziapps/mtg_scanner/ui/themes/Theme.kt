package com.zaziapps.mtg_scanner.ui.themes

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Standard dark color palette scheme configuration token mappings.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

/**
 * Standard light color palette scheme configuration token mappings.
 */
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

/**
 * Main application theme wrapper mapping cohesive material design properties onto the layout hierarchy context.
 * Dynamically resolves system brightness parameters and manages material palette overrides based on hardware versioning specs.
 *
 * @param darkTheme Operational toggle determining whether to supply dark or light baseline color schemes.
 * @param dynamicColor Conditional flag allowing system-backed Material You wallpaper color derivation on supported Android S+ devices.
 * @param content Nested user interface Composable layout components to frame inside the customized theme.
 */
@Composable
fun MTGScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Enforce fallback evaluations on matching hardware targets to deploy system dynamic theme engines.
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
