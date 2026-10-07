package com.partituresfesteres.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.material.LocalContentColor
import androidx.compose.material.TextFieldColors
import androidx.compose.material.TextFieldDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.partituresfesteres.app.ui.theme.AgedGold
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.Ink
import com.partituresfesteres.app.ui.theme.MutedInk
import com.partituresfesteres.app.ui.theme.ParchmentCard

/**
 * Components compartits per a evitar la barreja de Card/Button/Surface de Material
 * que, en alguns launchers/configuracions d'accessibilitat, afegia un segon
 * rectangle clar darrere del contingut. Aquests components dibuixen una sola
 * superfície opaca i un únic contorn, mantenint l'estètica de pergamí.
 */
@Composable
internal fun FestivePanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color = ParchmentCard,
    borderColor: Color = AgedGold.copy(alpha = 0.28f),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor, shape)
            .border(1.dp, borderColor, shape),
    ) {
        content()
    }
}

@Composable
internal fun FestiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    cornerRadius: Dp = 12.dp,
    minHeight: Dp = 44.dp,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 9.dp,
    borderColor: Color = if (backgroundColor.luminance() > 0.55f) AgedGold.copy(alpha = 0.38f) else Color.Transparent,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(shape)
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.45f), shape)
            .border(1.dp, borderColor, shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
internal fun FestiveChoice(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color,
    selectedContentColor: Color = Color.White,
    contentColor: Color,
    cornerRadius: Dp = 10.dp,
    content: @Composable RowScope.() -> Unit,
) {
    FestiveButton(
        onClick = onClick,
        modifier = modifier,
        backgroundColor = if (selected) selectedColor else ParchmentCard,
        contentColor = if (selected) selectedContentColor else contentColor,
        cornerRadius = cornerRadius,
        minHeight = 42.dp,
        horizontalPadding = 10.dp,
        verticalPadding = 8.dp,
        borderColor = if (selected) Color.Transparent else AgedGold.copy(alpha = 0.34f),
        content = content,
    )
}

@Composable
internal fun festiveTextFieldColors(): TextFieldColors = TextFieldDefaults.outlinedTextFieldColors(
    textColor = Ink,
    backgroundColor = ParchmentCard,
    cursorColor = Burgundy,
    focusedBorderColor = Burgundy,
    unfocusedBorderColor = AgedGold.copy(alpha = 0.55f),
    focusedLabelColor = Burgundy,
    unfocusedLabelColor = MutedInk,
    placeholderColor = MutedInk.copy(alpha = 0.86f),
)

private fun Color.luminance(): Float {
    fun channel(v: Float): Float = if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    return 0.2126f * channel(red) + 0.7152f * channel(green) + 0.0722f * channel(blue)
}
