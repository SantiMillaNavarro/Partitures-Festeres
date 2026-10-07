package com.partituresfesteres.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partituresfesteres.app.R
import com.partituresfesteres.app.ui.theme.AgedGold
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.HeritageGreen
import com.partituresfesteres.app.ui.theme.MutedGold
import com.partituresfesteres.app.ui.theme.Navy
import com.partituresfesteres.app.ui.theme.ParchmentCard

enum class AdaptiveWindowSize { COMPACT, MEDIUM, EXPANDED }

val LocalAdaptiveWindowSize = staticCompositionLocalOf { AdaptiveWindowSize.EXPANDED }

@Composable
fun AdaptiveWindowProvider(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // A phone in landscape can have an "expanded" width but very little height.
        // Treat short windows as compact so navigation never becomes unreachable.
        val windowSize = when {
            maxWidth < 600.dp || maxHeight < 480.dp -> AdaptiveWindowSize.COMPACT
            maxWidth < 840.dp || maxHeight < 600.dp -> AdaptiveWindowSize.MEDIUM
            else -> AdaptiveWindowSize.EXPANDED
        }
        CompositionLocalProvider(LocalAdaptiveWindowSize provides windowSize) {
            content()
        }
    }
}

@Composable
internal fun AdaptiveNavigationScaffold(
    activeSection: AppSection,
    onLibraryClick: () -> Unit,
    onRepertoiresClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onToolsClick: () -> Unit,
    onAddContentClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val windowSize = LocalAdaptiveWindowSize.current

    if (windowSize == AdaptiveWindowSize.COMPACT) {
        Box(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 68.dp),
                content = content,
            )

            MobileBottomNavigation(
                activeSection = activeSection,
                onLibraryClick = onLibraryClick,
                onRepertoiresClick = onRepertoiresClick,
                onRecentsClick = onRecentsClick,
                onFavoritesClick = onFavoritesClick,
                onToolsClick = onToolsClick,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            if (activeSection != AppSection.ADD_CONTENT) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 12.dp, bottom = 76.dp)
                        .size(52.dp)
                        .background(Burgundy, CircleShape)
                        .border(2.dp, AgedGold, CircleShape)
                        .clickable(onClick = onAddContentClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.sidebar_add_content),
                        tint = Color.White,
                        modifier = Modifier.size(27.dp),
                    )
                }
            }
        }
    } else {
        Row(Modifier.fillMaxSize()) {
            Sidebar(
                activeSection = activeSection,
                onLibraryClick = onLibraryClick,
                onRepertoiresClick = onRepertoiresClick,
                onRecentsClick = onRecentsClick,
                onFavoritesClick = onFavoritesClick,
                onAddContentClick = onAddContentClick,
                onToolsClick = onToolsClick,
            )
            Box(Modifier.weight(1f).fillMaxHeight(), content = content)
        }
    }
}

private data class MobileNavItem(
    val section: AppSection,
    val labelRes: Int,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
)

@Composable
private fun MobileBottomNavigation(
    activeSection: AppSection,
    onLibraryClick: () -> Unit,
    onRepertoiresClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onToolsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        MobileNavItem(AppSection.LIBRARY, R.string.sidebar_library, Icons.Default.Folder, Burgundy, onLibraryClick),
        MobileNavItem(AppSection.REPERTOIRES, R.string.sidebar_repertoires, Icons.Default.LibraryMusic, Navy, onRepertoiresClick),
        MobileNavItem(AppSection.RECENTS, R.string.sidebar_recents, Icons.Default.History, HeritageGreen, onRecentsClick),
        MobileNavItem(AppSection.FAVORITES, R.string.sidebar_favorites, Icons.Default.Favorite, MutedGold, onFavoritesClick),
        MobileNavItem(AppSection.TOOLS, R.string.sidebar_tools, Icons.Default.Build, Color(0xFF76507C), onToolsClick),
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xFFF7ECD8),
        elevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                val selected = activeSection == item.section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 2.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (selected) item.color else Color.Transparent,
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = stringResource(item.labelRes),
                            tint = if (selected) Color.White else item.color,
                            modifier = Modifier.size(23.dp),
                        )
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(
                        stringResource(item.labelRes),
                        color = if (selected) item.color else Navy,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
