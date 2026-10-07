package com.partituresfesteres.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.partituresfesteres.app.R
import com.partituresfesteres.app.data.LibraryRepository
import com.partituresfesteres.app.data.PdfThumbnailRepository
import com.partituresfesteres.app.model.LibraryFolder
import com.partituresfesteres.app.model.LibraryPdf
import com.partituresfesteres.app.model.Repertoire
import com.partituresfesteres.app.ui.theme.AgedGold
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.Ink
import com.partituresfesteres.app.ui.theme.MutedInk
import com.partituresfesteres.app.ui.theme.Navy
import com.partituresfesteres.app.ui.theme.ParchmentCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AddSource { SCAN, IMPORT }
private enum class ConflictChoice { KEEP_BOTH, REPLACE }

@Composable
fun AddContentScreen(
    rootUri: Uri,
    repository: LibraryRepository,
    thumbnailRepository: PdfThumbnailRepository,
    repertoires: List<Repertoire>,
    initialImportUri: Uri? = null,
    onSaved: (LibraryPdf, String?) -> Unit,
    onLibraryClick: () -> Unit,
    onRepertoiresClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onToolsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val rootFolderLabel = stringResource(R.string.root_folder)
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()

    var sourceUri by remember { mutableStateOf<Uri?>(null) }
    var source by remember { mutableStateOf<AddSource?>(null) }
    var scanPageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pageCount by remember { mutableStateOf<Int?>(null) }
    var name by remember { mutableStateOf("") }
    var folders by remember { mutableStateOf<List<LibraryFolder>>(emptyList()) }
    var loadingFolders by remember { mutableStateOf(true) }
    var selectedFolder by remember(rootUri, rootFolderLabel) {
        mutableStateOf(LibraryFolder(rootFolderLabel, rootUri, 0, ""))
    }
    var addToRepertoire by remember { mutableStateOf(false) }
    var selectedRepertoireId by remember { mutableStateOf(repertoires.firstOrNull()?.id) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showRepertoireDialog by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingConflict by remember { mutableStateOf(false) }

    LaunchedEffect(rootUri) {
        loadingFolders = true
        folders = repository.readAllFolders(rootUri).getOrDefault(emptyList())
        loadingFolders = false
    }

    LaunchedEffect(initialImportUri) {
        val uri = initialImportUri ?: return@LaunchedEffect
        sourceUri = uri
        source = AddSource.IMPORT
        scanPageUris = emptyList()
        pageCount = null
        name = context.displayName(uri).removePdfSuffix().ifBlank { context.getString(R.string.new_score) }
        error = null
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(30)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember(scannerOptions) { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            runCatching { GmsDocumentScanningResult.fromActivityResultIntent(result.data) }
                .onSuccess { scanResult ->
                    val pdf = scanResult?.pdf
                    if (pdf != null) {
                        sourceUri = pdf.uri
                        source = AddSource.SCAN
                        pageCount = pdf.pageCount
                        scanPageUris = scanResult.pages?.map { it.imageUri }.orEmpty()
                        if (name.isBlank()) name = context.getString(R.string.new_score)
                        error = null
                    }
                }
                .onFailure { throwable ->
                    error = throwable.message ?: context.getString(R.string.scan_recover_error)
                }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            sourceUri = uri
            source = AddSource.IMPORT
            scanPageUris = emptyList()
            pageCount = null
            name = context.displayName(uri).removePdfSuffix().ifBlank { context.getString(R.string.new_score) }
            error = null
        }
    }

    fun startScanner() {
        val host = activity
        if (host == null) {
            error = context.getString(R.string.scanner_start_error)
            return
        }
        scanner.getStartScanIntent(host)
            .addOnSuccessListener { sender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(sender).build())
            }
            .addOnFailureListener { throwable ->
                error = throwable.message ?: context.getString(R.string.scanner_start_error)
            }
    }

    fun performSave(conflictChoice: ConflictChoice? = null) {
        val currentSource = sourceUri ?: return
        if (name.isBlank()) {
            error = context.getString(R.string.name_required)
            return
        }
        scope.launch {
            saving = true
            error = null
            val finalName = if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
            val exists = repository.pdfExists(selectedFolder.uri, finalName)
            if (exists && conflictChoice == null) {
                pendingConflict = true
                saving = false
                return@launch
            }
            repository.savePdf(
                sourceUri = currentSource,
                directoryUri = selectedFolder.uri,
                requestedName = name,
                relativePath = selectedFolder.relativePath,
                replaceExisting = conflictChoice == ConflictChoice.REPLACE,
                keepBoth = conflictChoice == ConflictChoice.KEEP_BOTH,
            ).onSuccess { saved ->
                onSaved(saved, if (addToRepertoire) selectedRepertoireId else null)
            }.onFailure { throwable ->
                error = throwable.message ?: context.getString(R.string.save_score_error)
            }
            saving = false
        }
    }

    AdaptiveNavigationScaffold(
        activeSection = AppSection.ADD_CONTENT,
        onLibraryClick = onLibraryClick,
        onRepertoiresClick = onRepertoiresClick,
        onRecentsClick = onRecentsClick,
        onFavoritesClick = onFavoritesClick,
        onToolsClick = onToolsClick,
        onAddContentClick = {},
    ) {
        val compact = LocalAdaptiveWindowSize.current == AdaptiveWindowSize.COMPACT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 10.dp else 26.dp,
                    end = if (compact) 10.dp else 38.dp,
                    top = if (compact) 6.dp else 16.dp,
                    bottom = if (compact) 6.dp else 22.dp,
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CimoPlaceholder(size = if (compact) 40.dp else 64.dp)
                Spacer(Modifier.width(if (compact) 7.dp else 12.dp))
                Column {
                    AppTitle(fontSize = if (compact) 24.sp else 34.sp)
                    Text(stringResource(R.string.add_title), fontSize = if (compact) 18.sp else 25.sp, color = Burgundy, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(if (compact) 6.dp else 12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 12.dp)) {
                FestiveButton(
                    onClick = ::startScanner,
                    backgroundColor = Burgundy,
                    contentColor = Color.White,
                    cornerRadius = 14.dp,
                    horizontalPadding = if (compact) 12.dp else 20.dp,
                    verticalPadding = if (compact) 9.dp else 12.dp,
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (compact) 19.dp else 24.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.scan_score), color = Color.White, fontSize = if (compact) 12.sp else 14.sp)
                }
                FestiveButton(
                    onClick = { importLauncher.launch(arrayOf("application/pdf")) },
                    backgroundColor = ParchmentCard,
                    contentColor = Burgundy,
                    cornerRadius = 14.dp,
                    horizontalPadding = if (compact) 12.dp else 20.dp,
                    verticalPadding = if (compact) 9.dp else 12.dp,
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Burgundy, modifier = Modifier.size(if (compact) 19.dp else 24.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.import_pdf), color = Burgundy, fontSize = if (compact) 12.sp else 14.sp)
                }
            }

            Spacer(Modifier.height(if (compact) 7.dp else 12.dp))

            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    FestivePanel(
                        modifier = Modifier.fillMaxWidth().height(230.dp),
                        cornerRadius = 18.dp,
                        backgroundColor = ParchmentCard,
                    ) {
                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                            Text(
                                if (source == AddSource.IMPORT) stringResource(R.string.selected_pdf) else stringResource(R.string.captured_pages),
                                color = Burgundy,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            when {
                                sourceUri == null -> EmptySourcePanel(onScan = ::startScanner, onImport = { importLauncher.launch(arrayOf("application/pdf")) })
                                source == AddSource.SCAN && scanPageUris.isNotEmpty() -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        scanPageUris.take(3).forEachIndexed { index, uri ->
                                            ScanImagePreview(uri = uri, label = stringResource(R.string.page_number, index + 1), modifier = Modifier.weight(1f))
                                        }
                                    }
                                    if (scanPageUris.size > 3) {
                                        Spacer(Modifier.height(5.dp))
                                        Text(stringResource(R.string.more_pages, scanPageUris.size - 3), color = MutedInk, fontSize = 11.sp)
                                    }
                                }
                                else -> ImportedPdfPreview(uri = sourceUri!!, thumbnailRepository = thumbnailRepository)
                            }
                        }
                    }

                    FestivePanel(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        backgroundColor = ParchmentCard,
                    ) {
                        Column(Modifier.fillMaxWidth().padding(13.dp)) {
                            Text(stringResource(R.string.score_data), color = Burgundy, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text(stringResource(R.string.name)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = festiveTextFieldColors(),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.save_in), color = Ink, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            FestiveButton(
                                onClick = { showFolderDialog = true },
                                enabled = !loadingFolders,
                                backgroundColor = ParchmentCard,
                                contentColor = Navy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Navy)
                                Spacer(Modifier.width(7.dp))
                                Text(
                                    selectedFolder.relativePath.ifBlank { rootFolderLabel },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Navy,
                                )
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Navy)
                            }
                            if (repertoires.isNotEmpty()) {
                                Spacer(Modifier.height(7.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = addToRepertoire,
                                        onCheckedChange = { addToRepertoire = it },
                                        colors = CheckboxDefaults.colors(checkedColor = Burgundy),
                                    )
                                    Text(stringResource(R.string.add_to_repertoire), color = Ink)
                                }
                                if (addToRepertoire) {
                                    FestiveButton(
                                        onClick = { showRepertoireDialog = true },
                                        backgroundColor = ParchmentCard,
                                        contentColor = Navy,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            repertoires.firstOrNull { it.id == selectedRepertoireId }?.name ?: stringResource(R.string.select_repertoire),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = Navy,
                                        )
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Navy)
                                    }
                                }
                            }
                            pageCount?.let {
                                Spacer(Modifier.height(7.dp))
                                Text(stringResource(R.string.pages_detected, it), color = MutedInk, fontSize = 12.sp)
                            }
                            error?.let {
                                Spacer(Modifier.height(6.dp))
                                Text(it, color = Burgundy, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { performSave() },
                                enabled = sourceUri != null && !saving,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 11.dp),
                            ) {
                                if (saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                else {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.save))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    FestivePanel(
                        modifier = Modifier.weight(0.56f).fillMaxHeight(),
                        cornerRadius = 20.dp,
                        backgroundColor = ParchmentCard,
                    ) {
                        Column(Modifier.fillMaxSize().padding(18.dp)) {
                            Text(
                                if (source == AddSource.IMPORT) stringResource(R.string.selected_pdf) else stringResource(R.string.captured_pages),
                                color = Burgundy,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(12.dp))
                            when {
                                sourceUri == null -> EmptySourcePanel(onScan = ::startScanner, onImport = { importLauncher.launch(arrayOf("application/pdf")) })
                                source == AddSource.SCAN && scanPageUris.isNotEmpty() -> {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        scanPageUris.take(3).forEachIndexed { index, uri ->
                                            ScanImagePreview(uri = uri, label = stringResource(R.string.page_number, index + 1), modifier = Modifier.weight(1f))
                                        }
                                    }
                                    if (scanPageUris.size > 3) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(stringResource(R.string.more_pages, scanPageUris.size - 3), color = MutedInk, fontSize = 13.sp)
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    FestiveButton(onClick = ::startScanner, backgroundColor = ParchmentCard, contentColor = Burgundy) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Burgundy)
                                        Spacer(Modifier.width(7.dp))
                                        Text(stringResource(R.string.scan_again), color = Burgundy)
                                    }
                                }
                                else -> ImportedPdfPreview(uri = sourceUri!!, thumbnailRepository = thumbnailRepository)
                            }
                        }
                    }

                    FestivePanel(
                        modifier = Modifier.weight(0.44f).fillMaxHeight(),
                        cornerRadius = 20.dp,
                        backgroundColor = ParchmentCard,
                    ) {
                        Column(Modifier.fillMaxSize().padding(18.dp)) {
                            Text(stringResource(R.string.score_data), color = Burgundy, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.name)) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = festiveTextFieldColors())
                            Spacer(Modifier.height(10.dp))
                            Text(stringResource(R.string.save_in), color = Ink, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(5.dp))
                            FestiveButton(onClick = { showFolderDialog = true }, enabled = !loadingFolders, backgroundColor = ParchmentCard, contentColor = Navy, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = Navy)
                                Spacer(Modifier.width(8.dp))
                                Text(selectedFolder.relativePath.ifBlank { rootFolderLabel }, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Navy)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Navy)
                            }
                            if (repertoires.isNotEmpty()) {
                                Spacer(Modifier.height(9.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = addToRepertoire, onCheckedChange = { addToRepertoire = it }, colors = CheckboxDefaults.colors(checkedColor = Burgundy))
                                    Text(stringResource(R.string.add_to_repertoire), color = Ink)
                                }
                                if (addToRepertoire) {
                                    FestiveButton(onClick = { showRepertoireDialog = true }, backgroundColor = ParchmentCard, contentColor = Navy, modifier = Modifier.fillMaxWidth()) {
                                        Text(repertoires.firstOrNull { it.id == selectedRepertoireId }?.name ?: stringResource(R.string.select_repertoire), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Navy)
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Navy)
                                    }
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                            pageCount?.let { Text(stringResource(R.string.pages_detected, it), color = MutedInk, fontSize = 13.sp) }
                            error?.let { Spacer(Modifier.height(7.dp)); Text(it, color = Burgundy, fontSize = 13.sp) }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = { performSave() },
                                enabled = sourceUri != null && !saving,
                                colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 12.dp),
                            ) {
                                if (saving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                else {
                                    Icon(Icons.Default.Save, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.save))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFolderDialog) {
        FolderPickerDialog(
            rootUri = rootUri,
            folders = folders,
            onDismiss = { showFolderDialog = false },
            onSelect = { folder ->
                selectedFolder = folder
                showFolderDialog = false
            },
        )
    }

    if (showRepertoireDialog) {
        SimpleChoiceDialog(
            title = stringResource(R.string.add_to_repertoire_title),
            options = repertoires.map { it.id to it.name },
            onDismiss = { showRepertoireDialog = false },
            onSelect = { id ->
                selectedRepertoireId = id
                showRepertoireDialog = false
            },
        )
    }

    if (pendingConflict) {
        AlertDialog(
            onDismissRequest = { pendingConflict = false },
            title = { Text(stringResource(R.string.duplicate_score_title)) },
            text = { Text(stringResource(R.string.duplicate_import_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingConflict = false
                    performSave(ConflictChoice.KEEP_BOTH)
                }) { Text(stringResource(R.string.keep_both), color = Navy) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        pendingConflict = false
                        performSave(ConflictChoice.REPLACE)
                    }) { Text(stringResource(R.string.replace), color = Burgundy) }
                    TextButton(onClick = { pendingConflict = false }) { Text(stringResource(R.string.cancel)) }
                }
            },
        )
    }
}

@Composable
private fun EmptySourcePanel(onScan: () -> Unit, onImport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Default.Description, contentDescription = null, tint = Burgundy, modifier = Modifier.size(60.dp))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.nothing_prepared), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(5.dp))
        Text(stringResource(R.string.nothing_prepared_hint), color = MutedInk, fontSize = 13.sp)
        Spacer(Modifier.height(15.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FestiveButton(onClick = onScan, backgroundColor = Burgundy, contentColor = Color.White) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.scan), color = Color.White)
            }
            FestiveButton(onClick = onImport, backgroundColor = ParchmentCard, contentColor = Burgundy) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Burgundy)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.import_pdf), color = Burgundy)
            }
        }
    }
}

@Composable
private fun ScanImagePreview(uri: Uri, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val rootFolderLabel = stringResource(R.string.root_folder)
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input) }
            }.getOrNull()
        }
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.72f),
            backgroundColor = Color.White,
            shape = RoundedCornerShape(10.dp),
            elevation = 2.dp,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Burgundy, modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = Ink, fontSize = 12.sp)
    }
}

@Composable
private fun ImportedPdfPreview(uri: Uri, thumbnailRepository: PdfThumbnailRepository) {
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = thumbnailRepository.firstPage(uri, 520)
    }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.fillMaxHeight(0.82f).aspectRatio(0.72f),
            backgroundColor = Color.White,
            shape = RoundedCornerShape(12.dp),
            elevation = 3.dp,
        ) {
            if (bitmap != null) {
                Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = stringResource(R.string.preview), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Burgundy)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.first_page_preview), color = MutedInk, fontSize = 13.sp)
    }
}

@Composable
private fun FolderPickerDialog(
    rootUri: Uri,
    folders: List<LibraryFolder>,
    onDismiss: () -> Unit,
    onSelect: (LibraryFolder) -> Unit,
) {
    val rootLabel = stringResource(R.string.root_folder)
    val all = remember(rootUri, folders, rootLabel) {
        listOf(LibraryFolder(rootLabel, rootUri, 0, "")) + folders
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_in)) },
        text = {
            LazyColumn(modifier = Modifier.height(350.dp)) {
                items(all, key = { it.uri.toString() }) { folder ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(folder) }.padding(vertical = 11.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = Burgundy)
                        Spacer(Modifier.width(9.dp))
                        Text(folder.relativePath.ifBlank { rootLabel }, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SimpleChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                items(options, key = { it.first }) { option ->
                    Text(
                        option.second,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(option.first) }.padding(vertical = 12.dp),
                        color = Ink,
                        fontSize = 16.sp,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun Context.displayName(uri: Uri): String {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }.getOrNull().orEmpty()
}

private fun String.removePdfSuffix(): String =
    if (endsWith(".pdf", ignoreCase = true)) dropLast(4) else this
