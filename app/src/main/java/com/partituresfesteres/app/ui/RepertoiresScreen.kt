package com.partituresfesteres.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partituresfesteres.app.R
import com.partituresfesteres.app.model.LibraryPdf
import com.partituresfesteres.app.model.Repertoire
import com.partituresfesteres.app.ui.theme.AgedGold
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.Ink
import com.partituresfesteres.app.ui.theme.HeritageGreen
import com.partituresfesteres.app.ui.theme.MutedGold
import com.partituresfesteres.app.ui.theme.MutedInk
import com.partituresfesteres.app.ui.theme.Navy
import com.partituresfesteres.app.ui.theme.ParchmentCard

@Composable
fun RepertoiresScreen(
    repertoires: List<Repertoire>,
    selectedRepertoireId: String?,
    globalPdfs: List<LibraryPdf>,
    indexingLibrary: Boolean,
    onSelectRepertoire: (String) -> Unit,
    onCreateRepertoire: (String) -> Unit,
    onDeleteRepertoire: (String) -> Unit,
    onDuplicateRepertoire: (String) -> Unit,
    onRenameRepertoire: (String, String) -> Unit,
    onAddPdfs: (String, List<LibraryPdf>) -> Unit,
    onRemoveEntry: (String, Int) -> Unit,
    onMoveEntry: (String, Int, Int) -> Unit,
    onStartPerformance: (Repertoire, Int) -> Unit,
    onLibraryClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onToolsClick: () -> Unit,
    onAddContentClick: () -> Unit,
) {
    val selected = repertoires.firstOrNull { it.id == selectedRepertoireId } ?: repertoires.firstOrNull()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Repertoire?>(null) }
    var deleteTarget by remember { mutableStateOf<Repertoire?>(null) }

    AdaptiveNavigationScaffold(
        activeSection = AppSection.REPERTOIRES,
        onLibraryClick = onLibraryClick,
        onRepertoiresClick = {},
        onRecentsClick = onRecentsClick,
        onFavoritesClick = onFavoritesClick,
        onToolsClick = onToolsClick,
        onAddContentClick = onAddContentClick,
    ) {
        val compact = LocalAdaptiveWindowSize.current == AdaptiveWindowSize.COMPACT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 10.dp else 20.dp,
                    end = if (compact) 10.dp else 20.dp,
                    top = if (compact) 6.dp else 12.dp,
                    bottom = if (compact) 6.dp else 18.dp,
                ),
        ) {
            RepertoireHeader(compact)
            Spacer(Modifier.height(if (compact) 6.dp else 10.dp))

            if (compact) {
                CompactRepertoireSelector(
                    repertoires = repertoires,
                    selected = selected,
                    onSelect = onSelectRepertoire,
                    onCreate = { showCreateDialog = true },
                )
                Spacer(Modifier.height(7.dp))
                FestivePanel(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    cornerRadius = 18.dp,
                    backgroundColor = ParchmentCard,
                ) {
                    if (selected == null) {
                        EmptyRepertoireDetail(onCreate = { showCreateDialog = true })
                    } else {
                        key(selected.id, selected.entries.hashCode()) {
                            RepertoireDetail(
                                repertoire = selected,
                                availableUris = if (indexingLibrary) null else globalPdfs.map { it.uri.toString() }.toSet(),
                                onAdd = { showAddDialog = true },
                                onDelete = { deleteTarget = selected },
                                onDuplicate = { onDuplicateRepertoire(selected.id) },
                                onRename = { renameTarget = selected },
                                onRemoveEntry = { index -> onRemoveEntry(selected.id, index) },
                                onMoveEntry = { from, to -> onMoveEntry(selected.id, from, to) },
                                onStart = { onStartPerformance(selected, 0) },
                                onOpenEntry = { index -> onStartPerformance(selected, index) },
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FestivePanel(
                        modifier = Modifier.weight(0.34f).fillMaxHeight(),
                        cornerRadius = 20.dp,
                        backgroundColor = ParchmentCard,
                    ) {
                        Column(Modifier.fillMaxSize().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.repertoire_title),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Burgundy,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { showCreateDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_repertoire), tint = Burgundy)
                                }
                            }
                            Spacer(Modifier.height(4.dp))

                            if (repertoires.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Burgundy, modifier = Modifier.size(46.dp))
                                    Spacer(Modifier.height(10.dp))
                                    Text(stringResource(R.string.no_repertoires), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(stringResource(R.string.no_repertoires_hint), color = MutedInk, fontSize = 13.sp)
                                    Spacer(Modifier.height(14.dp))
                                    Button(
                                        onClick = { showCreateDialog = true },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.new_repertoire))
                                    }
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(9.dp),
                                    contentPadding = PaddingValues(bottom = 12.dp),
                                ) {
                                    items(repertoires, key = { it.id }) { repertoire ->
                                        RepertoireCard(
                                            repertoire = repertoire,
                                            selected = repertoire.id == selected?.id,
                                            onClick = { onSelectRepertoire(repertoire.id) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    FestivePanel(
                        modifier = Modifier.weight(0.66f).fillMaxHeight(),
                        cornerRadius = 20.dp,
                        backgroundColor = ParchmentCard,
                    ) {
                        if (selected == null) {
                            EmptyRepertoireDetail(onCreate = { showCreateDialog = true })
                        } else {
                            key(selected.id, selected.entries.hashCode()) {
                                RepertoireDetail(
                                    repertoire = selected,
                                    availableUris = if (indexingLibrary) null else globalPdfs.map { it.uri.toString() }.toSet(),
                                    onAdd = { showAddDialog = true },
                                    onDelete = { deleteTarget = selected },
                                    onDuplicate = { onDuplicateRepertoire(selected.id) },
                                    onRename = { renameTarget = selected },
                                    onRemoveEntry = { index -> onRemoveEntry(selected.id, index) },
                                    onMoveEntry = { from, to -> onMoveEntry(selected.id, from, to) },
                                    onStart = { onStartPerformance(selected, 0) },
                                    onOpenEntry = { index -> onStartPerformance(selected, index) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        EditRepertoireNameDialog(
            title = stringResource(R.string.new_repertoire),
            initialName = "",
            confirmLabel = stringResource(R.string.create),
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreateRepertoire(name)
                showCreateDialog = false
            },
        )
    }

    renameTarget?.let { repertoire ->
        EditRepertoireNameDialog(
            title = stringResource(R.string.rename_repertoire),
            initialName = repertoire.name,
            confirmLabel = stringResource(R.string.save),
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                onRenameRepertoire(repertoire.id, newName)
                renameTarget = null
            },
        )
    }

    if (showAddDialog && selected != null) {
        AddScoresDialog(
            repertoire = selected,
            pdfs = globalPdfs,
            loading = indexingLibrary,
            onDismiss = { showAddDialog = false },
            onConfirm = { pdfs ->
                onAddPdfs(selected.id, pdfs)
                showAddDialog = false
            },
        )
    }

    deleteTarget?.let { repertoire ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_repertoire)) },
            text = { Text(stringResource(R.string.delete_repertoire_confirm, repertoire.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRepertoire(repertoire.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete_repertoire), color = Burgundy) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun RepertoireHeader(compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CimoPlaceholder(size = if (compact) 40.dp else 64.dp)
        Spacer(Modifier.width(if (compact) 7.dp else 12.dp))
        Text(
            stringResource(R.string.partitures_festeres),
            fontSize = if (compact) 24.sp else 32.sp,
            color = Burgundy,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompactRepertoireSelector(
    repertoires: List<Repertoire>,
    selected: Repertoire?,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FestiveButton(
            onClick = onCreate,
            backgroundColor = Burgundy,
            contentColor = Color.White,
            horizontalPadding = 12.dp,
            verticalPadding = 8.dp,
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_repertoire), tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.new_repertoire), color = Color.White, fontSize = 12.sp)
        }
        repertoires.forEach { repertoire ->
            FestiveChoice(
                selected = repertoire.id == selected?.id,
                onClick = { onSelect(repertoire.id) },
                selectedColor = Navy,
                contentColor = Navy,
                cornerRadius = 12.dp,
            ) {
                Text(
                    repertoire.name,
                    color = if (repertoire.id == selected?.id) Color.White else Navy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun RepertoireSidebar(
    onLibraryClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onToolsClick: () -> Unit,
    onAddContentClick: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier
                .width(if (compact) 92.dp else 236.dp)
                .fillMaxHeight()
                .background(Color(0xAAF7ECD8))
                .border(1.dp, AgedGold.copy(alpha = 0.45f))
                .padding(horizontal = if (compact) 9.dp else 18.dp, vertical = if (compact) 18.dp else 30.dp),
        ) {
            if (!compact) {
                Text(stringResource(R.string.sidebar_good_morning), fontStyle = FontStyle.Italic, fontSize = 19.sp, color = Ink)
                Text(stringResource(R.string.sidebar_motto), fontStyle = FontStyle.Italic, fontSize = 13.sp, color = MutedInk)
                Spacer(Modifier.height(34.dp))
            }
            RepertoireSidebarItem(stringResource(R.string.sidebar_library), Icons.Default.Folder, Burgundy, false, compact, onLibraryClick)
            RepertoireSidebarItem(stringResource(R.string.sidebar_repertoires), Icons.Default.LibraryMusic, Navy, true, compact)
            RepertoireSidebarItem(stringResource(R.string.sidebar_recents), Icons.Default.History, HeritageGreen, false, compact, onRecentsClick)
            RepertoireSidebarItem(stringResource(R.string.sidebar_favorites), Icons.Default.Favorite, MutedGold, false, compact, onFavoritesClick)
            RepertoireSidebarItem(stringResource(R.string.sidebar_tools), Icons.Default.Build, Color(0xFF76507C), false, compact, onToolsClick)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(if (compact) 62.dp else 76.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Burgundy, CircleShape)
                    .border(2.dp, AgedGold, CircleShape)
                    .clickable(onClick = onAddContentClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (compact) 30.dp else 36.dp))
            }
            if (!compact) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.sidebar_add_content), modifier = Modifier.align(Alignment.CenterHorizontally), color = Ink)
                Spacer(Modifier.height(26.dp))
                Text(stringResource(R.string.sidebar_identity), letterSpacing = 3.sp, fontSize = 11.sp, color = MutedInk)
            }
        }
    }
}

@Composable
private fun RepertoireSidebarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    sectionColor: Color,
    selected: Boolean,
    compact: Boolean,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 4.dp else 7.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(if (selected) ParchmentCard else Color.Transparent, RoundedCornerShape(16.dp))
            .padding(if (compact) 7.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 48.dp else 46.dp)
                .background(sectionColor, CircleShape)
                .border(1.dp, AgedGold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        if (!compact) {
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 20.sp, color = if (selected) sectionColor else Navy, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun RepertoireCard(repertoire: Repertoire, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val background = if (selected) ParchmentCard.copy(alpha = 0.98f) else ParchmentCard.copy(alpha = 0.72f)
    val outline = if (selected) AgedGold.copy(alpha = 0.72f) else AgedGold.copy(alpha = 0.22f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, outline, shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(if (selected) Burgundy else Navy, CircleShape)
                .border(1.dp, AgedGold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(
                repertoire.name,
                color = if (selected) Burgundy else Navy,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stringResource(R.string.score_count_format, repertoire.entries.size), color = MutedInk, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyRepertoireDetail(onCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = Burgundy, modifier = Modifier.size(62.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.first_repertoire), fontSize = 22.sp, color = Burgundy, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.first_repertoire_hint), color = MutedInk)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onCreate,
            colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.new_repertoire))
        }
    }
}

@Composable
private fun RepertoireDetail(
    repertoire: Repertoire,
    availableUris: Set<String>?,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onRename: () -> Unit,
    onRemoveEntry: (Int) -> Unit,
    onMoveEntry: (Int, Int) -> Unit,
    onStart: () -> Unit,
    onOpenEntry: (Int) -> Unit,
) {
    var search by remember(repertoire.id) { mutableStateOf("") }
    val hasAvailableEntries = availableUris == null || repertoire.entries.any { it.uriString in availableUris }
    // No se memoriza la lista: el repertorio es pequeño y recalcularla garantiza
    // que los cambios rápidos de contenido se reflejen inmediatamente en pantalla.
    val indexedEntries = repertoire.entries.mapIndexed { index, entry -> index to entry }
    val filteredEntries = smartSearch(search, indexedEntries) { (_, entry) ->
        listOf(entry.displayName, entry.fileName, entry.relativePath)
    }

    val compact = LocalAdaptiveWindowSize.current == AdaptiveWindowSize.COMPACT
    Column(Modifier.fillMaxSize().padding(if (compact) 9.dp else 16.dp)) {
        // Títol en una línia pròpia: evita que es trenque en pantalles estretes.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    repertoire.name,
                    color = Burgundy,
                    fontSize = if (compact) 19.sp else 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(stringResource(R.string.score_count_format, repertoire.entries.size), color = MutedInk, fontSize = 13.sp)
            }
            Button(
                onClick = onStart,
                enabled = repertoire.entries.isNotEmpty() && hasAvailableEntries,
                colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.start_performance))
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (compact) Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRename) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(3.dp))
                Text(stringResource(R.string.rename), color = Navy, fontSize = 13.sp)
            }
            TextButton(onClick = onDuplicate) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Navy, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(3.dp))
                Text(stringResource(R.string.duplicate), color = Navy, fontSize = 13.sp)
            }
            TextButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Burgundy, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(3.dp))
                Text(stringResource(R.string.delete_repertoire), color = Burgundy, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(6.dp))
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (search.isNotBlank()) {{ IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear)) } }} else null,
                    placeholder = { Text(stringResource(R.string.search_repertoire), fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = festiveTextFieldColors(),
                )
                Button(
                    onClick = onAdd,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_scores), modifier = Modifier.size(20.dp))
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (search.isNotBlank()) {{ IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear)) } }} else null,
                    placeholder = { Text(stringResource(R.string.search_repertoire)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = festiveTextFieldColors(),
                )
                Button(
                    onClick = onAdd,
                    modifier = Modifier.widthIn(min = 158.dp, max = 180.dp).heightIn(min = 56.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.elevation(defaultElevation = 1.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_scores), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (repertoire.entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.repertoire_empty), color = Ink, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.repertoire_empty_hint), color = MutedInk)
            }
        } else if (filteredEntries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.no_score_found), color = Ink, fontSize = 17.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.try_another_name), color = MutedInk)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(
                    items = filteredEntries,
                    key = { (originalIndex, entry) -> "${repertoire.id}:$originalIndex:${entry.uriString}" },
                ) { (originalIndex, entry) ->
                    RepertoireEntryRow(
                        index = originalIndex,
                        available = availableUris?.contains(entry.uriString) ?: true,
                        entryName = entry.displayName,
                        relativePath = entry.relativePath,
                        canReorder = search.isBlank(),
                        itemCount = repertoire.entries.size,
                        onMove = onMoveEntry,
                        onOpen = { onOpenEntry(originalIndex) },
                        onRemove = { onRemoveEntry(originalIndex) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RepertoireEntryRow(
    index: Int,
    available: Boolean,
    entryName: String,
    relativePath: String,
    canReorder: Boolean,
    itemCount: Int,
    onMove: (Int, Int) -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val background = if (available) ParchmentCard.copy(alpha = 0.88f) else Color(0xFFFFEEE9).copy(alpha = 0.92f)
    val outline = if (available) AgedGold.copy(alpha = 0.20f) else Burgundy.copy(alpha = 0.22f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, outline, shape)
            .then(if (available) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${index + 1}", color = AgedGold, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
        Column(Modifier.weight(1f)) {
            Text(entryName, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!available) {
                Text(stringResource(R.string.score_unavailable), color = Burgundy, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            } else if (relativePath.isNotBlank()) {
                Text(relativePath, color = MutedInk, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(
            onClick = { onMove(index, index - 1) },
            enabled = canReorder && index > 0,
            modifier = Modifier.size(38.dp),
        ) {
            Icon(
                Icons.Default.ArrowUpward,
                contentDescription = stringResource(R.string.move_score_up),
                tint = if (canReorder && index > 0) Navy else MutedInk.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = { onMove(index, index + 1) },
            enabled = canReorder && index < itemCount - 1,
            modifier = Modifier.size(38.dp),
        ) {
            Icon(
                Icons.Default.ArrowDownward,
                contentDescription = stringResource(R.string.move_score_down),
                tint = if (canReorder && index < itemCount - 1) Navy else MutedInk.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(38.dp)) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove_from_repertoire), tint = Burgundy, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun EditRepertoireNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = festiveTextFieldColors(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text(confirmLabel, color = Burgundy) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun AddScoresDialog(
    repertoire: Repertoire,
    pdfs: List<LibraryPdf>,
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<LibraryPdf>) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    var selectedUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    val existing = remember(repertoire.id, repertoire.entries) { repertoire.entries.map { it.uriString }.toSet() }
    val filtered = remember(pdfs, search) {
        smartSearch(search, pdfs) {
            listOf(it.displayName, it.fileName, it.relativePath)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_scores_to_repertoire, repertoire.name)) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (search.isNotBlank()) {
                        {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear))
                            }
                        }
                    } else null,
                    placeholder = { Text(stringResource(R.string.search_global)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = festiveTextFieldColors(),
                )
                Spacer(Modifier.height(10.dp))
                if (loading) {
                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Burgundy)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filtered, key = { it.uri.toString() }) { pdf ->
                            val uri = pdf.uri.toString()
                            val alreadyAdded = uri in existing
                            val checked = alreadyAdded || uri in selectedUris
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !alreadyAdded) {
                                        selectedUris = if (uri in selectedUris) selectedUris - uri else selectedUris + uri
                                    }
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = if (alreadyAdded) null else { value ->
                                        selectedUris = if (value) selectedUris + uri else selectedUris - uri
                                    },
                                    enabled = !alreadyAdded,
                                    colors = CheckboxDefaults.colors(checkedColor = Burgundy),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(pdf.displayName, color = if (alreadyAdded) MutedInk else Navy, fontWeight = FontWeight.Medium)
                                    if (pdf.relativePath.isNotBlank()) {
                                        Text(pdf.relativePath, color = MutedInk, fontSize = 11.sp)
                                    }
                                }
                                if (alreadyAdded) {
                                    Text(stringResource(R.string.already_added), color = MutedInk, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(pdfs.filter { it.uri.toString() in selectedUris }) },
                enabled = selectedUris.isNotEmpty(),
            ) { Text(stringResource(R.string.add_selected, selectedUris.size), color = Burgundy) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
