package com.partituresfesteres.app.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable

private val AppColors = lightColors(
    primary = Burgundy,
    primaryVariant = BurgundyDark,
    secondary = Navy,
    background = Parchment,
    surface = ParchmentCard,
    onPrimary = ParchmentCard,
    onSecondary = ParchmentCard,
    onBackground = Ink,
    onSurface = Ink,
)

@Composable
fun PartituresFesteresTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = AppColors,
        content = content,
    )
}
