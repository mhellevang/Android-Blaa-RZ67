package dev.hellevang.androidblaarz67.ui.theme

import android.graphics.Color.parseColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Color(parseColor("#f6bd60")),
    primaryVariant = Color(parseColor("#f7ede2")),
    secondary = Color(parseColor("#f5cac3")),
    background = Color(parseColor("#2a2c2e"))
)

@Composable
fun AndroidBlaaRZ67Theme(
    content: @Composable () -> Unit
) {
    val colors = DarkColorPalette

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}