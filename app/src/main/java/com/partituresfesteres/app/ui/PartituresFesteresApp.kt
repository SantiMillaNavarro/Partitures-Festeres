package com.partituresfesteres.app.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partituresfesteres.app.R
import com.partituresfesteres.app.data.LibraryRepository
import com.partituresfesteres.app.data.LibraryStateStore
import com.partituresfesteres.app.data.AnnotationStore
import com.partituresfesteres.app.data.AppSettings
import com.partituresfesteres.app.data.BackupManager
import com.partituresfesteres.app.data.PdfThumbnailRepository
import com.partituresfesteres.app.data.PdfPageAdjustmentStore
import com.partituresfesteres.app.data.PdfViewerRepository
import com.partituresfesteres.app.data.RootFolderStore
import com.partituresfesteres.app.data.RepertoireStore
import com.partituresfesteres.app.data.ViewerStateStore
import com.partituresfesteres.app.data.SettingsStore
import com.partituresfesteres.app.model.LibraryCrumb
import com.partituresfesteres.app.model.LibraryDirectory
import com.partituresfesteres.app.model.LibraryFolder
import com.partituresfesteres.app.model.LibraryPdf
import com.partituresfesteres.app.model.Repertoire
import com.partituresfesteres.app.model.RepertoireEntry
import com.partituresfesteres.app.ui.theme.AgedGold
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.Ink
import com.partituresfesteres.app.ui.theme.HeritageGreen
import com.partituresfesteres.app.ui.theme.MutedGold
import com.partituresfesteres.app.ui.theme.MutedInk
import com.partituresfesteres.app.ui.theme.Navy
import com.partituresfesteres.app.ui.theme.ParchmentCard
import kotlinx.coroutines.launch
import java.util.UUID

internal enum class AppSection { LIBRARY, REPERTOIRES, RECENTS, FAVORITES, TOOLS, ADD_CONTENT, SETTINGS }

private data class ViewerSession(
    val pdfs: List<LibraryPdf>,
    val index: Int,
    val startMode: ViewerStartMode,
    val loopForward: Boolean = false,
    val revision: Int = 0,
) {
    val currentPdf: LibraryPdf get() = pdfs[index]
}

@Composable
fun PartituresFesteresApp(
    incomingPdfUri: Uri? = null,
    onIncomingPdfConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember { RootFolderStore(context) }
    val repository = remember { LibraryRepository(context) }
    val thumbnailRepository = remember { PdfThumbnailRepository(context) }
    val viewerRepository = remember { PdfViewerRepository(context) }
    val viewerStateStore = remember { ViewerStateStore(context) }
    val repertoireStore = remember { RepertoireStore(context) }
    val libraryStateStore = remember { LibraryStateStore(context) }
    val annotationStore = remember { AnnotationStore(context) }
    val pageAdjustmentStore = remember { PdfPageAdjustmentStore(context) }
    val settingsStore = remember { SettingsStore(context) }
    val backupManager = remember { BackupManager(context) }

    var viewerSession by remember { mutableStateOf<ViewerSession?>(null) }
    var activeSection by remember { mutableStateOf(AppSection.LIBRARY) }
    var repertoires by remember { mutableStateOf(repertoireStore.load()) }
    var selectedRepertoireId by remember { mutableStateOf(repertoires.firstOrNull()?.id) }
    var favorites by remember { mutableStateOf(libraryStateStore.loadFavorites()) }
    var recents by remember { mutableStateOf(libraryStateStore.loadRecents()) }
    var rootUri by remember { mutableStateOf(store.load()) }
    var breadcrumbs by remember { mutableStateOf<List<LibraryCrumb>>(emptyList()) }
    var directory by remember { mutableStateOf(LibraryDirectory(emptyList(), emptyList())) }
    var globalPdfs by remember { mutableStateOf<List<LibraryPdf>>(emptyList()) }
    var globalFolders by remember { mutableStateOf<List<LibraryFolder>>(emptyList()) }
    var indexingLibrary by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var appSettings by remember { mutableStateOf(settingsStore.load()) }
    var backupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(incomingPdfUri, rootUri) {
        if (incomingPdfUri != null && rootUri != null) {
            search = ""
            activeSection = AppSection.ADD_CONTENT
        }
    }

    // Manté la pantalla activa durant tota l'aplicació (no sols al visor).
    // En eixir de l'app Android recupera el comportament normal automàticament.
    val activity = context as? Activity
    DisposableEffect(activity, appSettings.keepScreenOn) {
        if (appSettings.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    fun persistRepertoires(updated: List<Repertoire>) {
        repertoires = updated
        repertoireStore.save(updated)
        if (selectedRepertoireId != null && updated.none { it.id == selectedRepertoireId }) {
            selectedRepertoireId = updated.firstOrNull()?.id
        }
    }

    fun createRepertoire(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val created = Repertoire(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            entries = emptyList(),
        )
        persistRepertoires(repertoires + created)
        selectedRepertoireId = created.id
    }

    fun deleteRepertoire(id: String) {
        persistRepertoires(repertoires.filterNot { it.id == id })
    }

    fun duplicateRepertoire(id: String) {
        val source = repertoires.firstOrNull { it.id == id } ?: return
        var candidate = context.getString(R.string.copy_suffix, source.name)
        var suffix = 2
        val existingNames = repertoires.map { it.name.lowercase() }.toSet()
        while (candidate.lowercase() in existingNames) {
            candidate = context.getString(R.string.copy_suffix_number, source.name, suffix)
            suffix += 1
        }
        val duplicate = source.copy(
            id = UUID.randomUUID().toString(),
            name = candidate,
            entries = source.entries.toList(),
        )
        persistRepertoires(repertoires + duplicate)
        selectedRepertoireId = duplicate.id
    }

    fun renameRepertoire(id: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        persistRepertoires(
            repertoires.map { repertoire ->
                if (repertoire.id == id) repertoire.copy(name = trimmed) else repertoire
            }
        )
    }

    fun addPdfsToRepertoire(id: String, pdfs: List<LibraryPdf>) {
        if (pdfs.isEmpty()) return
        val updated = repertoires.map { repertoire ->
            if (repertoire.id != id) repertoire else {
                val existing = repertoire.entries.map { it.uriString }.toSet()
                val newEntries = pdfs
                    .filterNot { it.uri.toString() in existing }
                    .map(RepertoireEntry::fromPdf)
                repertoire.copy(entries = repertoire.entries + newEntries)
            }
        }
        persistRepertoires(updated)
    }

    fun removeRepertoireEntry(id: String, index: Int) {
        val updated = repertoires.map { repertoire ->
            if (repertoire.id != id || index !in repertoire.entries.indices) repertoire
            else repertoire.copy(entries = repertoire.entries.toMutableList().also { it.removeAt(index) })
        }
        persistRepertoires(updated)
    }

    fun moveRepertoireEntry(id: String, from: Int, to: Int) {
        val updated = repertoires.map { repertoire ->
            if (repertoire.id != id || from !in repertoire.entries.indices || to !in repertoire.entries.indices) {
                repertoire
            } else {
                val entries = repertoire.entries.toMutableList()
                val item = entries.removeAt(from)
                entries.add(to, item)
                repertoire.copy(entries = entries)
            }
        }
        persistRepertoires(updated)
    }

    fun toggleFavorite(pdf: LibraryPdf) {
        val uri = pdf.uri.toString()
        favorites = if (favorites.any { it.uri.toString() == uri }) {
            favorites.filterNot { it.uri.toString() == uri }
        } else {
            listOf(pdf) + favorites
        }
        libraryStateStore.saveFavorites(favorites)
    }

    fun recordRecent(pdf: LibraryPdf) {
        recents = libraryStateStore.recordRecent(pdf)
    }

    fun replacePdfReferences(oldPdf: LibraryPdf, newPdf: LibraryPdf) {
        if (oldPdf.uri != newPdf.uri) {
            annotationStore.migrateUri(oldPdf.uri, newPdf.uri)
            pageAdjustmentStore.migrateUri(oldPdf.uri, newPdf.uri)
            viewerStateStore.migrateUri(oldPdf.uri, newPdf.uri)
        }

        val oldUri = oldPdf.uri.toString()
        favorites = favorites.map { if (it.uri.toString() == oldUri) newPdf else it }
            .distinctBy { it.uri.toString() }
        libraryStateStore.saveFavorites(favorites)

        recents = recents.map { if (it.uri.toString() == oldUri) newPdf else it }
            .distinctBy { it.uri.toString() }
            .take(50)
        libraryStateStore.saveRecents(recents)

        val updatedRepertoires = repertoires.map { repertoire ->
            repertoire.copy(entries = repertoire.entries.map { entry ->
                if (entry.uriString == oldUri) RepertoireEntry.fromPdf(newPdf) else entry
            })
        }
        persistRepertoires(updatedRepertoires)
    }

    fun removeDeletedPdfReferences(pdf: LibraryPdf) {
        val uriString = pdf.uri.toString()
        favorites = favorites.filterNot { it.uri.toString() == uriString }
        libraryStateStore.saveFavorites(favorites)
        recents = recents.filterNot { it.uri.toString() == uriString }
        libraryStateStore.saveRecents(recents)
        annotationStore.clearAll(pdf.uri)
        pageAdjustmentStore.clearAll(pdf.uri)
        viewerStateStore.clear(pdf.uri)
        persistRepertoires(
            repertoires.map { repertoire ->
                repertoire.copy(entries = repertoire.entries.filterNot { it.uriString == uriString })
            }
        )
    }

    fun reconcileReferences(index: List<LibraryPdf>) {
        if (index.isEmpty()) return
        val byUri = index.associateBy { it.uri.toString() }
        val byFileName = index.groupBy { it.fileName.lowercase() }

        fun resolve(pdf: LibraryPdf): LibraryPdf? {
            byUri[pdf.uri.toString()]?.let { return it }
            val candidates = byFileName[pdf.fileName.lowercase()].orEmpty()
            val match = candidates.singleOrNull()
            if (match != null && match.uri != pdf.uri) {
                annotationStore.migrateUri(pdf.uri, match.uri)
                pageAdjustmentStore.migrateUri(pdf.uri, match.uri)
                viewerStateStore.migrateUri(pdf.uri, match.uri)
            }
            return match
        }

        val repairedFavorites = favorites.map { resolve(it) ?: it }.distinctBy { it.uri.toString() }
        if (repairedFavorites != favorites) {
            favorites = repairedFavorites
            libraryStateStore.saveFavorites(repairedFavorites)
        }

        val repairedRecents = recents.mapNotNull(::resolve).distinctBy { it.uri.toString() }.take(50)
        if (repairedRecents != recents) {
            recents = repairedRecents
            libraryStateStore.saveRecents(repairedRecents)
        }

        val repairedRepertoires = repertoires.map { repertoire ->
            val repairedEntries = repertoire.entries.map { entry ->
                if (entry.uriString in byUri) entry else {
                    val match = byFileName[entry.fileName.lowercase()].orEmpty().singleOrNull()
                    if (match != null) {
                        val oldUri = Uri.parse(entry.uriString)
                        annotationStore.migrateUri(oldUri, match.uri)
                        pageAdjustmentStore.migrateUri(oldUri, match.uri)
                        viewerStateStore.migrateUri(oldUri, match.uri)
                        RepertoireEntry.fromPdf(match)
                    } else entry
                }
            }
            repertoire.copy(entries = repairedEntries)
        }
        if (repairedRepertoires != repertoires) persistRepertoires(repairedRepertoires)
    }

    val currentUri = breadcrumbs.lastOrNull()?.uri ?: rootUri

    suspend fun reload(uri: Uri) {
        loading = true
        error = null
        repository.readDirectory(uri)
            .onSuccess { directory = it }
            .onFailure { error = it.message ?: context.getString(R.string.library_read_error) }
        loading = false
    }

    suspend fun rebuildGlobalIndex(uri: Uri) {
        indexingLibrary = true
        repository.readAllPdfs(uri)
            .onSuccess { indexed ->
                globalPdfs = indexed
                reconcileReferences(indexed)
            }
            .onFailure { /* La navegació local continua disponible encara que falle l'índex. */ }
        repository.readAllFolders(uri)
            .onSuccess { globalFolders = it }
            .onFailure { globalFolders = emptyList() }
        indexingLibrary = false
    }

    fun navigateToFolder(folder: LibraryFolder) {
        breadcrumbs = breadcrumbs + LibraryCrumb(folder.name, folder.uri)
        search = ""
    }

    fun navigateBack() {
        if (breadcrumbs.isNotEmpty()) {
            breadcrumbs = breadcrumbs.dropLast(1)
            search = ""
        }
    }

    fun returnToLibraryRoot() {
        breadcrumbs = emptyList()
        search = ""
    }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }.recoverCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            store.save(uri)
            breadcrumbs = emptyList()
            search = ""
            rootUri = uri
            activeSection = AppSection.LIBRARY
        }
    }


    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            backupManager.exportTo(uri)
                .onSuccess { backupMessage = context.getString(R.string.backup_created) }
                .onFailure { backupMessage = it.message ?: context.getString(R.string.backup_create_error) }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            backupManager.restoreFrom(uri)
                .onSuccess {
                    repertoires = repertoireStore.load()
                    selectedRepertoireId = repertoires.firstOrNull()?.id
                    favorites = libraryStateStore.loadFavorites()
                    recents = libraryStateStore.loadRecents()
                    appSettings = settingsStore.load()
                    backupMessage = context.getString(R.string.backup_restored)
                }
                .onFailure { backupMessage = it.message ?: context.getString(R.string.backup_restore_error) }
        }
    }

    LaunchedEffect(currentUri) {
        currentUri?.let { reload(it) }
    }

    LaunchedEffect(rootUri) {
        rootUri?.let { rebuildGlobalIndex(it) }
    }

    BackHandler(enabled = viewerSession == null && activeSection != AppSection.LIBRARY) {
        activeSection = AppSection.LIBRARY
        returnToLibraryRoot()
    }

    BackHandler(enabled = viewerSession == null && activeSection == AppSection.LIBRARY && breadcrumbs.isNotEmpty()) {
        navigateBack()
    }

    val activeViewer = viewerSession
    LaunchedEffect(activeViewer?.currentPdf?.uri) {
        activeViewer?.currentPdf?.let(::recordRecent)
    }
    if (activeViewer != null) {
        key(activeViewer.currentPdf.uri, activeViewer.revision) {
            PdfViewerScreen(
                pdf = activeViewer.currentPdf,
                repository = viewerRepository,
                stateStore = viewerStateStore,
                annotationStore = annotationStore,
                pageAdjustmentStore = pageAdjustmentStore,
                settings = appSettings,
                tunerReferenceHz = appSettings.tunerReferenceHz,
                onTunerReferenceChange = { hz ->
                    val updated = appSettings.copy(tunerReferenceHz = hz.coerceIn(430, 450))
                    appSettings = updated
                    settingsStore.save(updated)
                },
                startMode = activeViewer.startMode,
                scorePosition = activeViewer.index + 1,
                totalScores = activeViewer.pdfs.size,
                scoreList = activeViewer.pdfs,
                onSelectScore = { targetIndex ->
                    if (targetIndex in activeViewer.pdfs.indices) {
                        viewerSession = activeViewer.copy(
                            index = targetIndex,
                            startMode = ViewerStartMode.FIRST,
                        )
                    }
                },
                isFavorite = favorites.any { it.uri == activeViewer.currentPdf.uri },
                onToggleFavorite = { toggleFavorite(activeViewer.currentPdf) },
                onClose = { viewerSession = null },
                onPreviousScore = {
                    if (activeViewer.index > 0) {
                        viewerSession = activeViewer.copy(
                            index = activeViewer.index - 1,
                            startMode = ViewerStartMode.LAST,
                        )
                    }
                },
                onNextScore = {
                    when {
                        activeViewer.index < activeViewer.pdfs.lastIndex -> {
                            viewerSession = activeViewer.copy(
                                index = activeViewer.index + 1,
                                startMode = ViewerStartMode.FIRST,
                            )
                        }
                        activeViewer.loopForward && activeViewer.pdfs.isNotEmpty() -> {
                            viewerSession = activeViewer.copy(
                                index = 0,
                                startMode = ViewerStartMode.FIRST,
                                revision = activeViewer.revision + 1,
                            )
                        }
                    }
                },
            )
        }
    } else {
        AppBackground {
            if (rootUri == null) {
                FirstRunScreen(onSelectRoot = { folderPicker.launch(null) })
            } else {
                when (activeSection) {
                    AppSection.LIBRARY -> {
                        val isGlobalSearch = search.isNotBlank()
                        val currentRelativePath = breadcrumbs.joinToString(" › ") { it.name }
                        val localPdfs = directory.pdfs.map { it.copy(relativePath = currentRelativePath) }
                        val filteredFolders = if (isGlobalSearch) emptyList() else directory.folders
                        val filteredPdfs = if (isGlobalSearch) {
                            smartSearch(search, globalPdfs) {
                                listOf(it.displayName, it.fileName, it.relativePath)
                            }
                        } else {
                            localPdfs
                        }

                        LibraryShell(
                            folders = filteredFolders,
                            pdfs = filteredPdfs,
                            breadcrumbs = breadcrumbs,
                            search = search,
                            onSearchChange = { search = it },
                            onClearSearch = { search = "" },
                            onLibraryRoot = ::returnToLibraryRoot,
                            onOpenRepertoires = {
                                search = ""
                                activeSection = AppSection.REPERTOIRES
                            },
                            onOpenRecents = {
                                search = ""
                                activeSection = AppSection.RECENTS
                            },
                            onOpenFavorites = {
                                search = ""
                                activeSection = AppSection.FAVORITES
                            },
                            onOpenTools = {
                                search = ""
                                activeSection = AppSection.TOOLS
                            },
                            onAddContent = {
                                search = ""
                                activeSection = AppSection.ADD_CONTENT
                            },
                            loading = loading || (isGlobalSearch && indexingLibrary),
                            isGlobalSearch = isGlobalSearch,
                            error = error,
                            onRefresh = {
                                currentUri?.let { uri -> scope.launch { reload(uri) } }
                                rootUri?.let { uri -> scope.launch { rebuildGlobalIndex(uri) } }
                            },
                            onChangeRoot = { folderPicker.launch(rootUri) },
                            onOpenSettings = {
                                search = ""
                                activeSection = AppSection.SETTINGS
                            },
                            onOpenFolder = ::navigateToFolder,
                            onOpenPdf = { pdf ->
                                val sourcePdfs = if (isGlobalSearch) filteredPdfs else localPdfs
                                val index = sourcePdfs.indexOfFirst { it.uri == pdf.uri }.coerceAtLeast(0)
                                viewerSession = ViewerSession(
                                    pdfs = sourcePdfs,
                                    index = index,
                                    startMode = ViewerStartMode.RESUME,
                                )
                            },
                            onBack = ::navigateBack,
                            thumbnailRepository = thumbnailRepository,
                            showPdfPath = isGlobalSearch,
                            favoriteUris = favorites.map { it.uri.toString() }.toSet(),
                            onToggleFavorite = ::toggleFavorite,
                            rootUri = rootUri!!,
                            allFolders = globalFolders,
                            repository = repository,
                            repertoireUseCount = { pdf ->
                                repertoires.count { repertoire -> repertoire.entries.any { it.uriString == pdf.uri.toString() } }
                            },
                            onPdfReplaced = { oldPdf, newPdf ->
                                replacePdfReferences(oldPdf, newPdf)
                                currentUri?.let { uri -> scope.launch { reload(uri) } }
                                rootUri?.let { uri -> scope.launch { rebuildGlobalIndex(uri) } }
                            },
                            onPdfDeleted = { pdf ->
                                removeDeletedPdfReferences(pdf)
                                currentUri?.let { uri -> scope.launch { reload(uri) } }
                                rootUri?.let { uri -> scope.launch { rebuildGlobalIndex(uri) } }
                            },
                        )
                    }
                    AppSection.REPERTOIRES -> {
                        RepertoiresScreen(
                            repertoires = repertoires,
                            selectedRepertoireId = selectedRepertoireId,
                            globalPdfs = globalPdfs,
                            indexingLibrary = indexingLibrary,
                            onSelectRepertoire = { selectedRepertoireId = it },
                            onCreateRepertoire = ::createRepertoire,
                            onDeleteRepertoire = ::deleteRepertoire,
                            onDuplicateRepertoire = ::duplicateRepertoire,
                            onRenameRepertoire = ::renameRepertoire,
                            onAddPdfs = ::addPdfsToRepertoire,
                            onRemoveEntry = ::removeRepertoireEntry,
                            onMoveEntry = ::moveRepertoireEntry,
                            onStartPerformance = { repertoire, startIndex ->
                                val availableByUri = globalPdfs.associateBy { it.uri.toString() }
                                val ordered = repertoire.entries.mapIndexedNotNull { originalIndex, entry ->
                                    availableByUri[entry.uriString]?.let { originalIndex to it }
                                }
                                if (ordered.isNotEmpty()) {
                                    val requestedPosition = ordered.indexOfFirst { (originalIndex, _) -> originalIndex >= startIndex }
                                        .let { if (it >= 0) it else 0 }
                                    viewerSession = ViewerSession(
                                        pdfs = ordered.map { it.second },
                                        index = requestedPosition.coerceIn(0, ordered.lastIndex),
                                        startMode = ViewerStartMode.FIRST,
                                        loopForward = true,
                                    )
                                }
                            },
                            onLibraryClick = {
                                activeSection = AppSection.LIBRARY
                                returnToLibraryRoot()
                            },
                            onRecentsClick = { activeSection = AppSection.RECENTS },
                            onFavoritesClick = { activeSection = AppSection.FAVORITES },
                            onToolsClick = { activeSection = AppSection.TOOLS },
                            onAddContentClick = { activeSection = AppSection.ADD_CONTENT },
                        )
                    }
                    AppSection.RECENTS -> {
                        SpecialCollectionShell(
                            section = AppSection.RECENTS,
                            title = stringResource(R.string.sidebar_recents),
                            subtitle = stringResource(R.string.recent_subtitle),
                            pdfs = recents,
                            thumbnailRepository = thumbnailRepository,
                            favoriteUris = favorites.map { it.uri.toString() }.toSet(),
                            onToggleFavorite = ::toggleFavorite,
                            onOpenPdf = { pdf ->
                                val index = recents.indexOfFirst { it.uri == pdf.uri }.coerceAtLeast(0)
                                viewerSession = ViewerSession(recents, index, ViewerStartMode.RESUME)
                            },
                            onLibraryClick = {
                                activeSection = AppSection.LIBRARY
                                returnToLibraryRoot()
                            },
                            onRepertoiresClick = { activeSection = AppSection.REPERTOIRES },
                            onRecentsClick = { activeSection = AppSection.RECENTS },
                            onFavoritesClick = { activeSection = AppSection.FAVORITES },
                            onToolsClick = { activeSection = AppSection.TOOLS },
                            onAddContentClick = { activeSection = AppSection.ADD_CONTENT },
                        )
                    }
                    AppSection.FAVORITES -> {
                        SpecialCollectionShell(
                            section = AppSection.FAVORITES,
                            title = stringResource(R.string.sidebar_favorites),
                            subtitle = stringResource(R.string.favorites_subtitle),
                            pdfs = favorites,
                            thumbnailRepository = thumbnailRepository,
                            favoriteUris = favorites.map { it.uri.toString() }.toSet(),
                            onToggleFavorite = ::toggleFavorite,
                            onOpenPdf = { pdf ->
                                val index = favorites.indexOfFirst { it.uri == pdf.uri }.coerceAtLeast(0)
                                viewerSession = ViewerSession(favorites, index, ViewerStartMode.RESUME)
                            },
                            onLibraryClick = {
                                activeSection = AppSection.LIBRARY
                                returnToLibraryRoot()
                            },
                            onRepertoiresClick = { activeSection = AppSection.REPERTOIRES },
                            onRecentsClick = { activeSection = AppSection.RECENTS },
                            onFavoritesClick = { activeSection = AppSection.FAVORITES },
                            onToolsClick = { activeSection = AppSection.TOOLS },
                            onAddContentClick = { activeSection = AppSection.ADD_CONTENT },
                        )
                    }
                    AppSection.SETTINGS -> {
                        SettingsScreen(
                            settings = appSettings,
                            onSettingsChange = { updated ->
                                appSettings = updated
                                settingsStore.save(updated)
                            },
                            onLanguageChange = { language ->
                                val updated = appSettings.copy(appLanguage = language)
                                settingsStore.save(updated)
                                appSettings = updated
                                activity?.recreate()
                            },
                            onSave = { backupMessage = context.getString(R.string.settings_saved) },
                            onSelectRoot = { folderPicker.launch(rootUri) },
                            onCreateBackup = { createBackupLauncher.launch("PartituresFesteres_backup.json") },
                            onRestoreBackup = { restoreBackupLauncher.launch(arrayOf("application/json", "text/plain")) },
                            backupMessage = backupMessage,
                            onLibraryClick = {
                                activeSection = AppSection.LIBRARY
                                returnToLibraryRoot()
                            },
                            onRepertoiresClick = { activeSection = AppSection.REPERTOIRES },
                            onRecentsClick = { activeSection = AppSection.RECENTS },
                            onFavoritesClick = { activeSection = AppSection.FAVORITES },
                            onToolsClick = { activeSection = AppSection.TOOLS },
                            onAddContentClick = { activeSection = AppSection.ADD_CONTENT },
                        )
                    }
                    AppSection.TOOLS -> {
                        ToolsScreen(
                            tunerReferenceHz = appSettings.tunerReferenceHz,
                            onTunerReferenceChange = { hz ->
                                val updated = appSettings.copy(tunerReferenceHz = hz.coerceIn(430, 450))
                                appSettings = updated
                                settingsStore.save(updated)
                            },
                            onLibraryClick = {
                                activeSection = AppSection.LIBRARY
                                returnToLibraryRoot()
                            },
                            onRepertoiresClick = { activeSection = AppSection.REPERTOIRES },
                            onRecentsClick = { activeSection = AppSection.RECENTS },
                            onFavoritesClick = { activeSection = AppSection.FAVORITES },
                            onToolsClick = { activeSection = AppSection.TOOLS },
                            onAddContentClick = { activeSection = AppSection.ADD_CONTENT },
                        )
                    }
                    AppSection.ADD_CONTENT -> {
                        AddContentScreen(
                            rootUri = rootUri!!,
                            repository = repository,
                            thumbnailRepository = thumbnailRepository,
                            repertoires = repertoires,
                            initialImportUri = incomingPdfUri,
                            onSaved = { pdf, repertoireId ->
                                if (repertoireId != null) addPdfsToRepertoire(repertoireId, listOf(pdf))
                                scope.launch {
                                    rootUri?.let { uri -> rebuildGlobalIndex(uri) }
                                    rootUri?.let { uri -> reload(uri) }
                                }
                                activeSection = AppSection.LIBRARY
                                returnToLibraryRoot()
                                onIncomingPdfConsumed()
                            },
                            onLibraryClick = {
                                activeSection = AppSection.LIBRARY
                                returnToLibraryRoot()
                            },
                            onRepertoiresClick = { activeSection = AppSection.REPERTOIRES },
                            onRecentsClick = { activeSection = AppSection.RECENTS },
                            onFavoritesClick = { activeSection = AppSection.FAVORITES },
                            onToolsClick = { activeSection = AppSection.TOOLS },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_parchment),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.06f)))
        content()
    }
}

@Composable
private fun FirstRunScreen(onSelectRoot: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(52.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CimoPlaceholder()
        Spacer(Modifier.height(8.dp))
        AppTitle()
        Spacer(Modifier.height(22.dp))
        Card(
            shape = RoundedCornerShape(22.dp),
            backgroundColor = ParchmentCard.copy(alpha = 0.94f),
            elevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 44.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.welcome_title), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.welcome_body),
                    color = MutedInk,
                    fontSize = 17.sp,
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onSelectRoot,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.settings_select_root), fontSize = 17.sp)
                }
            }
        }
    }
}

@Composable
private fun LibraryShell(
    folders: List<LibraryFolder>,
    pdfs: List<LibraryPdf>,
    breadcrumbs: List<LibraryCrumb>,
    search: String,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onLibraryRoot: () -> Unit,
    onOpenRepertoires: () -> Unit,
    onOpenRecents: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenTools: () -> Unit,
    onAddContent: () -> Unit,
    loading: Boolean,
    isGlobalSearch: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onChangeRoot: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFolder: (LibraryFolder) -> Unit,
    onOpenPdf: (LibraryPdf) -> Unit,
    onBack: () -> Unit,
    thumbnailRepository: PdfThumbnailRepository,
    showPdfPath: Boolean,
    favoriteUris: Set<String>,
    onToggleFavorite: (LibraryPdf) -> Unit,
    rootUri: Uri,
    allFolders: List<LibraryFolder>,
    repository: LibraryRepository,
    repertoireUseCount: (LibraryPdf) -> Int,
    onPdfReplaced: (LibraryPdf, LibraryPdf) -> Unit,
    onPdfDeleted: (LibraryPdf) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var managedPdf by remember { mutableStateOf<LibraryPdf?>(null) }

    managedPdf?.let { pdf ->
        PdfFileActionsDialog(
            pdf = pdf,
            rootUri = rootUri,
            folders = allFolders,
            repository = repository,
            repertoireUseCount = repertoireUseCount(pdf),
            onDismiss = { managedPdf = null },
            onPdfReplaced = { newPdf ->
                onPdfReplaced(pdf, newPdf)
                managedPdf = null
            },
            onPdfDeleted = {
                onPdfDeleted(pdf)
                managedPdf = null
            },
        )
    }

    Row(Modifier.fillMaxSize()) {
        Sidebar(
            activeSection = AppSection.LIBRARY,
            onLibraryClick = onLibraryRoot,
            onRepertoiresClick = onOpenRepertoires,
            onRecentsClick = onOpenRecents,
            onFavoritesClick = onOpenFavorites,
            onAddContentClick = onAddContent,
            onToolsClick = onOpenTools,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 26.dp, end = 38.dp, top = 18.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CimoPlaceholder()
                    AppTitle()
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title), tint = Ink, modifier = Modifier.size(30.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            BreadcrumbRow(breadcrumbs = breadcrumbs, onBack = onBack)
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = onSearchChange,
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = {
                        Text(stringResource(R.string.search_global))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    trailingIcon = if (search.isNotBlank()) {
                        {
                            IconButton(onClick = {
                                onClearSearch()
                                keyboardController?.hide()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear), tint = Burgundy)
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.search_refresh_library), tint = Burgundy)
                }
            }

            Spacer(Modifier.height(16.dp))

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Burgundy)
                }
                error != null -> ErrorPanel(error, onChangeRoot)
                folders.isEmpty() && pdfs.isEmpty() -> EmptyDirectoryPanel(breadcrumbs.isEmpty())
                else -> LibraryGrid(
                    folders = folders,
                    pdfs = pdfs,
                    onOpenFolder = onOpenFolder,
                    onOpenPdf = onOpenPdf,
                    thumbnailRepository = thumbnailRepository,
                    showPdfPath = showPdfPath,
                    favoriteUris = favoriteUris,
                    onToggleFavorite = onToggleFavorite,
                    onManagePdf = { managedPdf = it },
                )
            }
        }
    }
}

private enum class PdfActionStage { MENU, RENAME, MOVE, MOVE_CONFLICT, DELETE }

@Composable
private fun PdfFileActionsDialog(
    pdf: LibraryPdf,
    rootUri: Uri,
    folders: List<LibraryFolder>,
    repository: LibraryRepository,
    repertoireUseCount: Int,
    onDismiss: () -> Unit,
    onPdfReplaced: (LibraryPdf) -> Unit,
    onPdfDeleted: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val rootLabel = stringResource(R.string.library_root)
    var stage by remember(pdf.uri) { mutableStateOf(PdfActionStage.MENU) }
    var renameText by remember(pdf.uri) { mutableStateOf(pdf.displayName) }
    var selectedDestination by remember(pdf.uri) { mutableStateOf<LibraryFolder?>(null) }
    var message by remember(pdf.uri) { mutableStateOf<String?>(null) }
    var working by remember(pdf.uri) { mutableStateOf(false) }

    val destinations = remember(rootUri, folders, rootLabel) {
        listOf(
            LibraryFolder(
                name = rootLabel,
                uri = rootUri,
                directPdfCount = 0,
                relativePath = "",
            )
        ) + folders
    }

    fun runMove(destination: LibraryFolder, replaceExisting: Boolean, keepBoth: Boolean) {
        if (destination.relativePath == pdf.relativePath) {
            message = context.getString(R.string.score_already_folder)
            stage = PdfActionStage.MOVE
            return
        }
        working = true
        message = null
        scope.launch {
            repository.movePdf(
                pdf = pdf,
                destinationUri = destination.uri,
                destinationRelativePath = destination.relativePath,
                replaceExisting = replaceExisting,
                keepBoth = keepBoth,
            ).onSuccess { moved ->
                onPdfReplaced(moved)
            }.onFailure { throwable ->
                message = throwable.message ?: context.getString(R.string.move_score_error)
                stage = PdfActionStage.MOVE
            }
            working = false
        }
    }

    when (stage) {
        PdfActionStage.MENU -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(pdf.displayName, color = Burgundy, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pdf.relativePath.isNotBlank()) {
                        Text(pdf.relativePath, color = MutedInk, fontSize = 13.sp)
                    }
                    Button(
                        onClick = { message = null; stage = PdfActionStage.MOVE },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Navy, contentColor = Color.White),
                    ) { Text(stringResource(R.string.move_to_folder)) }
                    Button(
                        onClick = { message = null; stage = PdfActionStage.RENAME },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = AgedGold, contentColor = Ink),
                    ) { Text(stringResource(R.string.rename)) }
                    Button(
                        onClick = { message = null; stage = PdfActionStage.DELETE },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                    ) { Text(stringResource(R.string.delete_score)) }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = ParchmentCard, contentColor = Ink),
                    ) { Text(stringResource(R.string.cancel)) }
                }
            },
            confirmButton = {},
            shape = RoundedCornerShape(20.dp),
            backgroundColor = ParchmentCard,
        )

        PdfActionStage.RENAME -> AlertDialog(
            onDismissRequest = { if (!working) stage = PdfActionStage.MENU },
            title = { Text(stringResource(R.string.rename_score), color = Burgundy, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text(stringResource(R.string.score_name)) },
                        singleLine = true,
                        enabled = !working,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Burgundy, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        working = true
                        message = null
                        scope.launch {
                            repository.renamePdf(pdf, renameText)
                                .onSuccess(onPdfReplaced)
                                .onFailure { message = it.message ?: context.getString(R.string.rename_score_error) }
                            working = false
                        }
                    },
                    enabled = !working && renameText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                ) { Text(if (working) stringResource(R.string.saving) else stringResource(R.string.save)) }
            },
            dismissButton = {
                Button(
                    onClick = { stage = PdfActionStage.MENU },
                    enabled = !working,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ParchmentCard, contentColor = Ink),
                ) { Text(stringResource(R.string.back)) }
            },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = ParchmentCard,
        )

        PdfActionStage.MOVE -> AlertDialog(
            onDismissRequest = { if (!working) stage = PdfActionStage.MENU },
            title = { Text(stringResource(R.string.move_score), color = Burgundy, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(stringResource(R.string.select_destination), color = Ink)
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .height(330.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        destinations.forEach { destination ->
                            Button(
                                onClick = {
                                    if (destination.relativePath == pdf.relativePath) {
                                        message = context.getString(R.string.score_already_folder)
                                    } else {
                                        working = true
                                        message = null
                                        scope.launch {
                                            val exists = repository.pdfExists(destination.uri, pdf.fileName)
                                            working = false
                                            if (exists) {
                                                selectedDestination = destination
                                                stage = PdfActionStage.MOVE_CONFLICT
                                            } else {
                                                runMove(destination, replaceExisting = false, keepBoth = false)
                                            }
                                        }
                                    }
                                },
                                enabled = !working,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = if (destination.relativePath == pdf.relativePath) Color.LightGray else Color.White,
                                    contentColor = Navy,
                                ),
                            ) {
                                Text(
                                    if (destination.relativePath.isBlank()) rootLabel else destination.relativePath,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Burgundy, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(
                    onClick = { stage = PdfActionStage.MENU },
                    enabled = !working,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ParchmentCard, contentColor = Ink),
                ) { Text(stringResource(R.string.back)) }
            },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = ParchmentCard,
        )

        PdfActionStage.MOVE_CONFLICT -> {
            val destination = selectedDestination
            AlertDialog(
                onDismissRequest = { if (!working) stage = PdfActionStage.MOVE },
                title = { Text(stringResource(R.string.duplicate_score_title), color = Burgundy, fontWeight = FontWeight.SemiBold) },
                text = {
                    Column {
                        Text(stringResource(R.string.duplicate_destination, destination?.relativePath?.ifBlank { rootLabel } ?: rootLabel, pdf.fileName))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.duplicate_options), color = MutedInk)
                        message?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = Burgundy, fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { destination?.let { runMove(it, replaceExisting = false, keepBoth = true) } },
                        enabled = !working && destination != null,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Navy, contentColor = Color.White),
                    ) { Text(stringResource(R.string.keep_both)) }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { destination?.let { runMove(it, replaceExisting = true, keepBoth = false) } },
                            enabled = !working && destination != null,
                            colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                        ) { Text(stringResource(R.string.replace)) }
                        Button(
                            onClick = { stage = PdfActionStage.MOVE },
                            enabled = !working,
                            colors = ButtonDefaults.buttonColors(backgroundColor = ParchmentCard, contentColor = Ink),
                        ) { Text(stringResource(R.string.cancel)) }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                backgroundColor = ParchmentCard,
            )
        }

        PdfActionStage.DELETE -> AlertDialog(
            onDismissRequest = { if (!working) stage = PdfActionStage.MENU },
            title = { Text(stringResource(R.string.delete_score_question), color = Burgundy, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_score_physical, pdf.fileName))
                    if (repertoireUseCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.delete_score_repertoires, repertoireUseCount, if (repertoireUseCount == 1) "" else "s"),
                            color = Burgundy,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.delete_score_metadata), color = MutedInk)
                    message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = Burgundy, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        working = true
                        message = null
                        scope.launch {
                            repository.deletePdf(pdf)
                                .onSuccess { onPdfDeleted() }
                                .onFailure { message = it.message ?: context.getString(R.string.delete_score_error) }
                            working = false
                        }
                    },
                    enabled = !working,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                ) { Text(if (working) stringResource(R.string.deleting) else stringResource(R.string.delete_score)) }
            },
            dismissButton = {
                Button(
                    onClick = { stage = PdfActionStage.MENU },
                    enabled = !working,
                    colors = ButtonDefaults.buttonColors(backgroundColor = ParchmentCard, contentColor = Ink),
                ) { Text(stringResource(R.string.cancel)) }
            },
            shape = RoundedCornerShape(20.dp),
            backgroundColor = ParchmentCard,
        )
    }
}

@Composable
private fun BreadcrumbRow(breadcrumbs: List<LibraryCrumb>, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (breadcrumbs.isNotEmpty()) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back), tint = Burgundy)
            }
            Spacer(Modifier.width(2.dp))
        }
        Text(stringResource(R.string.sidebar_library), fontSize = 26.sp, color = Burgundy, fontWeight = FontWeight.SemiBold)
        breadcrumbs.forEach { crumb ->
            Text("  ›  ", color = AgedGold, fontSize = 22.sp)
            Text(
                crumb.name,
                color = Navy,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun Sidebar(
    activeSection: AppSection,
    onLibraryClick: () -> Unit,
    onRepertoiresClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onAddContentClick: () -> Unit,
    onToolsClick: () -> Unit = {},
) {
    // Use the real layout constraints instead of LocalConfiguration.screenWidthDp.
    // A locale-specific Configuration can be created before the Activity has its final
    // landscape window metrics, which made the navigation bar incorrectly switch to
    // compact mode after restarting the app following a language change.
    BoxWithConstraints(modifier = Modifier.fillMaxHeight()) {
        val compact = maxWidth < 700.dp
        val sidebarWidth = if (compact) 92.dp else 236.dp

        Column(
            modifier = Modifier
                .width(sidebarWidth)
                .fillMaxHeight()
                .background(Color(0xAAF7ECD8))
                .border(1.dp, AgedGold.copy(alpha = 0.45f))
                .padding(horizontal = if (compact) 9.dp else 18.dp, vertical = if (compact) 18.dp else 30.dp),
        ) {
            if (!compact) {
                Text(stringResource(R.string.sidebar_good_morning), fontStyle = FontStyle.Italic, fontSize = 19.sp, color = Ink)
                Text(stringResource(R.string.sidebar_motto), fontStyle = FontStyle.Italic, fontSize = 13.sp, color = MutedInk)
                Spacer(Modifier.height(34.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }

            SidebarItem(stringResource(R.string.sidebar_library), Icons.Default.Folder, Burgundy, activeSection == AppSection.LIBRARY, compact, onLibraryClick)
            SidebarItem(stringResource(R.string.sidebar_repertoires), Icons.Default.LibraryMusic, Navy, activeSection == AppSection.REPERTOIRES, compact, onRepertoiresClick)
            SidebarItem(stringResource(R.string.sidebar_recents), Icons.Default.History, HeritageGreen, activeSection == AppSection.RECENTS, compact, onRecentsClick)
            SidebarItem(stringResource(R.string.sidebar_favorites), Icons.Default.Favorite, MutedGold, activeSection == AppSection.FAVORITES, compact, onFavoritesClick)
            SidebarItem(stringResource(R.string.sidebar_tools), Icons.Default.Build, Color(0xFF76507C), activeSection == AppSection.TOOLS, compact, onToolsClick)

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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.sidebar_add_content), tint = Color.White, modifier = Modifier.size(if (compact) 30.dp else 36.dp))
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
private fun SidebarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    sectionColor: Color,
    selected: Boolean = false,
    compact: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 4.dp else 7.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .background(if (selected) Color.White.copy(alpha = 0.56f) else Color.Transparent, RoundedCornerShape(16.dp))
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
private fun SpecialCollectionShell(
    section: AppSection,
    title: String,
    subtitle: String,
    pdfs: List<LibraryPdf>,
    thumbnailRepository: PdfThumbnailRepository,
    favoriteUris: Set<String>,
    onToggleFavorite: (LibraryPdf) -> Unit,
    onOpenPdf: (LibraryPdf) -> Unit,
    onLibraryClick: () -> Unit,
    onRepertoiresClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onAddContentClick: () -> Unit,
    onToolsClick: () -> Unit = {},
) {
    var filter by remember(section) { mutableStateOf("") }
    val filtered = remember(pdfs, filter) {
        smartSearch(filter, pdfs) {
            listOf(it.displayName, it.fileName, it.relativePath)
        }
    }

    Row(Modifier.fillMaxSize()) {
        Sidebar(
            activeSection = section,
            onLibraryClick = onLibraryClick,
            onRepertoiresClick = onRepertoiresClick,
            onRecentsClick = onRecentsClick,
            onFavoritesClick = onFavoritesClick,
            onAddContentClick = onAddContentClick,
            onToolsClick = onToolsClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 26.dp, end = 38.dp, top = 18.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CimoPlaceholder()
                Spacer(Modifier.width(12.dp))
                Column {
                    AppTitle()
                    Text(title, fontSize = 26.sp, color = Burgundy, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, fontSize = 13.sp, color = MutedInk)
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (filter.isNotBlank()) {
                    {
                        IconButton(onClick = { filter = "" }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.search_clear), tint = Burgundy)
                        }
                    }
                } else null,
                placeholder = { Text(stringResource(R.string.search_scores)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.height(16.dp))
            if (filtered.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    backgroundColor = ParchmentCard.copy(alpha = 0.92f),
                ) {
                    Column(Modifier.padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (section == AppSection.FAVORITES) Icons.Default.Favorite else Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = if (section == AppSection.FAVORITES) MutedGold else HeritageGreen,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (section == AppSection.FAVORITES) stringResource(R.string.favorites_empty)
                            else stringResource(R.string.recents_empty),
                            color = Ink,
                            fontSize = 18.sp,
                        )
                    }
                }
            } else {
                LibraryGrid(
                    folders = emptyList(),
                    pdfs = filtered,
                    onOpenFolder = {},
                    onOpenPdf = onOpenPdf,
                    thumbnailRepository = thumbnailRepository,
                    showPdfPath = true,
                    favoriteUris = favoriteUris,
                    onToggleFavorite = onToggleFavorite,
                )
            }
        }
    }
}

@Composable
private fun LibraryGrid(
    folders: List<LibraryFolder>,
    pdfs: List<LibraryPdf>,
    onOpenFolder: (LibraryFolder) -> Unit,
    onOpenPdf: (LibraryPdf) -> Unit,
    thumbnailRepository: PdfThumbnailRepository,
    showPdfPath: Boolean,
    favoriteUris: Set<String>,
    onToggleFavorite: (LibraryPdf) -> Unit,
    onManagePdf: ((LibraryPdf) -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 210.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 20.dp),
    ) {
        if (folders.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionLabel(stringResource(R.string.library_folders))
            }
            items(folders, key = { "folder:${it.uri}" }) { folder ->
                FolderCard(folder = folder, onClick = { onOpenFolder(folder) })
            }
        }

        if (pdfs.isNotEmpty()) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SectionLabel(stringResource(R.string.library_scores))
            }
            items(pdfs, key = { "pdf:${it.uri}" }) { pdf ->
                PdfCard(
                    pdf = pdf,
                    thumbnailRepository = thumbnailRepository,
                    onClick = { onOpenPdf(pdf) },
                    showPath = showPdfPath,
                    isFavorite = pdf.uri.toString() in favoriteUris,
                    onToggleFavorite = { onToggleFavorite(pdf) },
                    onManage = onManagePdf?.let { callback -> { callback(pdf) } },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
        fontSize = 18.sp,
        color = Burgundy,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun FolderCard(folder: LibraryFolder, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(172.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = ParchmentCard.copy(alpha = 0.93f),
        elevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Burgundy, CircleShape)
                    .border(2.dp, AgedGold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(35.dp))
            }
            Spacer(Modifier.height(9.dp))
            Text(
                folder.name,
                fontSize = 19.sp,
                color = Navy,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(stringResource(R.string.score_count_format, folder.directPdfCount), fontSize = 13.sp, color = MutedInk)
        }
    }
}

@Composable
private fun PdfCard(
    pdf: LibraryPdf,
    thumbnailRepository: PdfThumbnailRepository,
    onClick: () -> Unit,
    showPath: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onManage: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = ParchmentCard.copy(alpha = 0.95f),
        elevation = 5.dp,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box {
                PdfThumbnail(pdf.uri, thumbnailRepository)
                if (onManage != null) {
                    IconButton(
                        onClick = onManage,
                        modifier = Modifier.align(Alignment.TopStart).size(40.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.manage_score),
                            tint = Navy,
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd).size(40.dp),
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) stringResource(R.string.remove_favorite) else stringResource(R.string.add_favorite),
                        tint = Burgundy,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                pdf.displayName,
                fontSize = 16.sp,
                color = Navy,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (showPath && pdf.relativePath.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    pdf.relativePath,
                    fontSize = 11.sp,
                    color = MutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PdfThumbnail(uri: Uri, repository: PdfThumbnailRepository) {
    val thumbnail by produceState<Bitmap?>(initialValue = null, uri) {
        value = repository.firstPage(uri)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.32f)
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, AgedGold.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when (val bitmap = thumbnail) {
            null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Burgundy, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(6.dp))
                    CircularProgressIndicator(color = AgedGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            else -> Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.score_thumbnail),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(3.dp),
            )
        }
    }
}

@Composable
private fun EmptyDirectoryPanel(isRoot: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = ParchmentCard.copy(alpha = 0.92f),
    ) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(52.dp), tint = Burgundy)
            Spacer(Modifier.height(10.dp))
            Text(
                if (isRoot) stringResource(R.string.library_empty_root)
                else stringResource(R.string.library_empty_folder),
                fontSize = 18.sp,
            )
            Text(stringResource(R.string.library_empty_hint), color = MutedInk)
        }
    }
}

@Composable
private fun ErrorPanel(message: String, onChangeRoot: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = ParchmentCard.copy(alpha = 0.96f),
    ) {
        Column(Modifier.padding(26.dp)) {
            Text(stringResource(R.string.library_open_error), fontSize = 20.sp, color = Burgundy, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = MutedInk)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onChangeRoot,
                colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
            ) {
                Text(stringResource(R.string.library_select_another))
            }
        }
    }
}

@Composable
internal fun CimoPlaceholder() {
    Image(
        painter = painterResource(com.partituresfesteres.app.R.drawable.brand_emblem),
        contentDescription = stringResource(R.string.musical_emblem),
        modifier = Modifier.size(64.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
internal fun AppTitle() {
    Text(stringResource(R.string.partitures_festeres), fontSize = 34.sp, color = Burgundy, fontWeight = FontWeight.SemiBold)
}
