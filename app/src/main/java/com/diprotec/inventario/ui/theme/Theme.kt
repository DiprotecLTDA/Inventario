package com.diprotec.inventario.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    secondary = BrandAccent,
    tertiary = BrandSurfaceTint,
    background = Background,
    surface = Surface,
    onPrimary = White,
    onSecondary = White,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = StatusError
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    secondary = BrandAccent,
    tertiary = BrandSurfaceTint,
    background = Background,
    surface = Surface,
    onPrimary = White,
    onSecondary = White,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = StatusError
)

@Composable
fun InventarioTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
