package com.partituresfesteres.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.partituresfesteres.app.data.AnnotationStore
import com.partituresfesteres.app.data.LanguageManager
import com.partituresfesteres.app.data.LibraryStateStore
import com.partituresfesteres.app.data.PdfPageAdjustmentStore
import com.partituresfesteres.app.data.PdfViewerRepository
import com.partituresfesteres.app.data.SettingsStore
import com.partituresfesteres.app.data.ViewerStateStore
import com.partituresfesteres.app.model.LibraryPdf
import com.partituresfesteres.app.ui.theme.PartituresFesteresTheme

/**
 * Visor de partitures en una Activity independent.
 *
 * La separació és deliberada: MainActivity manté la seua política d'orientació
 * estable mentre el visor pot usar el sensor complet sense obligar Android a
 * canviar requestedOrientation dins de la mateixa Activity una vegada i una altra.
 */
class PdfViewerActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialSession = decodeSession(intent) ?: run {
            finish()
            return
        }

        // Per defecte l'Activity ja usa fullSensor des del Manifest. Només
        // fem una petició programàtica si l'usuari ha desactivat la rotació
        // del visor; així evitem canvis de política innecessaris en OnePlus.
        val initialSettings = SettingsStore(this).load()
        if (!initialSettings.allowViewerRotation) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        setContent {
            PartituresFesteresTheme {
                AdaptiveWindowProvider {
                val repository = remember { PdfViewerRepository(this) }
                val stateStore = remember { ViewerStateStore(this) }
                val annotationStore = remember { AnnotationStore(this) }
                val pageAdjustmentStore = remember { PdfPageAdjustmentStore(this) }
                val settingsStore = remember { SettingsStore(this) }
                val libraryStateStore = remember { LibraryStateStore(this) }

                var session by remember { mutableStateOf(initialSession) }
                var settings by remember { mutableStateOf(settingsStore.load()) }
                var favorites by remember { mutableStateOf(libraryStateStore.loadFavorites()) }

                LaunchedEffect(session.currentPdf.uri) {
                    libraryStateStore.recordRecent(session.currentPdf)
                }

                key(session.currentPdf.uri, session.revision) {
                PdfViewerScreen(
                    pdf = session.currentPdf,
                    repository = repository,
                    stateStore = stateStore,
                    annotationStore = annotationStore,
                    pageAdjustmentStore = pageAdjustmentStore,
                    settings = settings,
                    tunerReferenceHz = settings.tunerReferenceHz,
                    onTunerReferenceChange = { hz ->
                        val updated = settings.copy(tunerReferenceHz = hz.coerceIn(430, 450))
                        settings = updated
                        settingsStore.save(updated)
                    },
                    startMode = session.startMode,
                    scorePosition = session.index + 1,
                    totalScores = session.pdfs.size,
                    scoreList = session.pdfs,
                    onSelectScore = { targetIndex ->
                        if (targetIndex in session.pdfs.indices) {
                            session = session.copy(index = targetIndex, startMode = ViewerStartMode.FIRST)
                        }
                    },
                    isFavorite = favorites.any { it.uri == session.currentPdf.uri },
                    onToggleFavorite = {
                        val current = session.currentPdf
                        favorites = if (favorites.any { it.uri == current.uri }) {
                            favorites.filterNot { it.uri == current.uri }
                        } else {
                            listOf(current) + favorites
                        }
                        libraryStateStore.saveFavorites(favorites)
                    },
                    onClose = { finish() },
                    onPreviousScore = {
                        if (session.index > 0) {
                            session = session.copy(index = session.index - 1, startMode = ViewerStartMode.LAST)
                        }
                    },
                    onNextScore = {
                        when {
                            session.index < session.pdfs.lastIndex -> {
                                session = session.copy(index = session.index + 1, startMode = ViewerStartMode.FIRST)
                            }
                            session.loopForward && session.pdfs.isNotEmpty() -> {
                                session = session.copy(
                                    index = 0,
                                    startMode = ViewerStartMode.FIRST,
                                    revision = session.revision + 1,
                                )
                            }
                        }
                    },
                )
                }
                }
            }
        }
    }

    private data class ViewerSession(
        val pdfs: List<LibraryPdf>,
        val index: Int,
        val startMode: ViewerStartMode,
        val loopForward: Boolean,
        val revision: Int = 0,
    ) {
        val currentPdf: LibraryPdf get() = pdfs[index]
    }

    companion object {
        private const val EXTRA_URIS = "viewer_uris"
        private const val EXTRA_FILE_NAMES = "viewer_file_names"
        private const val EXTRA_DISPLAY_NAMES = "viewer_display_names"
        private const val EXTRA_RELATIVE_PATHS = "viewer_relative_paths"
        private const val EXTRA_INDEX = "viewer_index"
        private const val EXTRA_START_MODE = "viewer_start_mode"
        private const val EXTRA_LOOP_FORWARD = "viewer_loop_forward"

        fun createIntent(
            context: Context,
            pdfs: List<LibraryPdf>,
            index: Int,
            startMode: ViewerStartMode,
            loopForward: Boolean = false,
        ): Intent {
            val safePdfs = pdfs.ifEmpty { return Intent(context, PdfViewerActivity::class.java) }
            val safeIndex = index.coerceIn(0, safePdfs.lastIndex)
            return Intent(context, PdfViewerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_URIS, ArrayList(safePdfs.map { it.uri.toString() }))
                putStringArrayListExtra(EXTRA_FILE_NAMES, ArrayList(safePdfs.map { it.fileName }))
                putStringArrayListExtra(EXTRA_DISPLAY_NAMES, ArrayList(safePdfs.map { it.displayName }))
                putStringArrayListExtra(EXTRA_RELATIVE_PATHS, ArrayList(safePdfs.map { it.relativePath }))
                putExtra(EXTRA_INDEX, safeIndex)
                putExtra(EXTRA_START_MODE, startMode.name)
                putExtra(EXTRA_LOOP_FORWARD, loopForward)
            }
        }

        private fun decodeSession(intent: Intent): ViewerSession? {
            val uris = intent.getStringArrayListExtra(EXTRA_URIS) ?: return null
            if (uris.isEmpty()) return null
            val fileNames = intent.getStringArrayListExtra(EXTRA_FILE_NAMES).orEmpty()
            val displayNames = intent.getStringArrayListExtra(EXTRA_DISPLAY_NAMES).orEmpty()
            val relativePaths = intent.getStringArrayListExtra(EXTRA_RELATIVE_PATHS).orEmpty()

            val pdfs = uris.mapIndexed { i, uriString ->
                val fileName = fileNames.getOrNull(i).orEmpty().ifBlank { "partitura.pdf" }
                LibraryPdf(
                    fileName = fileName,
                    displayName = displayNames.getOrNull(i).orEmpty().ifBlank { fileName.substringBeforeLast('.') },
                    uri = Uri.parse(uriString),
                    relativePath = relativePaths.getOrNull(i).orEmpty(),
                )
            }
            val index = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, pdfs.lastIndex)
            val startMode = runCatching {
                ViewerStartMode.valueOf(intent.getStringExtra(EXTRA_START_MODE) ?: ViewerStartMode.RESUME.name)
            }.getOrDefault(ViewerStartMode.RESUME)
            return ViewerSession(
                pdfs = pdfs,
                index = index,
                startMode = startMode,
                loopForward = intent.getBooleanExtra(EXTRA_LOOP_FORWARD, false),
            )
        }
    }
}
