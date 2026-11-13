package com.wifisecurity.analyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF07111F)
private val Panel = Color(0xFF101F32)
private val Cyan = Color(0xFF22D3EE)
private val Blue = Color(0xFF2563EB)

@Composable
fun AnalyzerTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Cyan,
        secondary = Blue,
        background = Navy,
        surface = Panel,
        onPrimary = Color(0xFF00131A),
        onBackground = Color(0xFFE7F2FF),
        onSurface = Color(0xFFE7F2FF)
    )
    MaterialTheme(colorScheme = colors, content = content)
}
