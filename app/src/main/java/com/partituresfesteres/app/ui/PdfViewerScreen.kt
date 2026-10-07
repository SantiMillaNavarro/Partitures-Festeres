package com.partituresfesteres.app.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partituresfesteres.app.R
import com.partituresfesteres.app.data.AnnotationPoint
import com.partituresfesteres.app.data.AppSettings
import com.partituresfesteres.app.data.AnnotationStore
import com.partituresfesteres.app.data.AnnotationStroke
import com.partituresfesteres.app.data.AnnotationShape
import com.partituresfesteres.app.data.AnnotationShapeType
import com.partituresfesteres.app.data.AnnotationText
import com.partituresfesteres.app.data.AnnotationTextAlign
import com.partituresfesteres.app.data.AnnotationTool
import com.partituresfesteres.app.data.PdfPageAdjustment
import com.partituresfesteres.app.data.PdfPageAdjustmentStore
import com.partituresfesteres.app.data.PdfViewerRepository
import com.partituresfesteres.app.data.remapAnnotationShape
import com.partituresfesteres.app.data.remapAnnotationStroke
import com.partituresfesteres.app.data.remapAnnotationText
import com.partituresfesteres.app.data.ViewerStateStore
import com.partituresfesteres.app.model.LibraryPdf
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.Ink
import com.partituresfesteres.app.ui.theme.Navy
import com.partituresfesteres.app.ui.theme.ParchmentCard
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.roundToInt

enum class ViewerStartMode {
    RESUME,
    FIRST,
    LAST,
}

private enum class AnnotationUiTool {
    SELECT,
    PENCIL,
    HIGHLIGHTER,
    TEXT,
    SHAPE,
    ERASER,
}

private enum class TextResizeHandle {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
}

private data class AnnotationSnapshot(
    val strokes: List<AnnotationStroke>,
    val texts: List<AnnotationText>,
    val shapes: List<AnnotationShape>,
)

@Composable
fun PdfViewerScreen(
    pdf: LibraryPdf,
    repository: PdfViewerRepository,
    stateStore: ViewerStateStore,
    annotationStore: AnnotationStore,
    pageAdjustmentStore: PdfPageAdjustmentStore,
    settings: AppSettings,
    tunerReferenceHz: Int,
    onTunerReferenceChange: (Int) -> Unit,
    startMode: ViewerStartMode,
    scorePosition: Int,
    totalScores: Int,
    scoreList: List<LibraryPdf>,
    onSelectScore: (Int) -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onPreviousScore: () -> Unit,
    onNextScore: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val activity = remember(context) { context.findActivity() }

    var pageCount by remember(pdf.uri) { mutableStateOf<Int?>(null) }
    var pageIndex by remember(pdf.uri) { mutableStateOf(0) }
    var countError by remember(pdf.uri) { mutableStateOf<String?>(null) }
    var menuVisible by remember(pdf.uri) { mutableStateOf(false) }
    var showScorePicker by remember { mutableStateOf(false) }
    var showQuickTools by remember { mutableStateOf(false) }
    var showRepairDialog by remember { mutableStateOf(false) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(pdf.uri, settings.rememberZoom) { mutableStateOf(if (settings.rememberZoom) stateStore.loadZoom(pdf.uri) else 1f) }
    var translation by remember(pdf.uri) { mutableStateOf(Offset.Zero) }
    var pageAdjustment by remember(pdf.uri, pageIndex) {
        mutableStateOf(pageAdjustmentStore.load(pdf.uri, pageIndex))
    }

    var annotationMode by remember(pdf.uri) { mutableStateOf(false) }
    var annotationUiTool by remember(pdf.uri) { mutableStateOf(AnnotationUiTool.SELECT) }
    var annotationsVisible by remember(pdf.uri) { mutableStateOf(true) }
    var pencilColor by remember(pdf.uri) { mutableStateOf(Color(0xFFE53935)) }
    var highlighterColor by remember(pdf.uri) { mutableStateOf(Color(0xFFFFD54F)) }
    var shapeColor by remember(pdf.uri) { mutableStateOf(Color(0xFFE53935)) }
    var pencilWidth by remember(pdf.uri) { mutableStateOf(0.0030f) }
    var highlighterWidth by remember(pdf.uri) { mutableStateOf(0.018f) }
    var shapeWidth by remember(pdf.uri) { mutableStateOf(0.0035f) }
    var currentShapeType by remember(pdf.uri) { mutableStateOf(AnnotationShapeType.ARROW) }
    var shapeFillEnabled by remember(pdf.uri) { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showScoreNameBanner by remember(pdf.uri) { mutableStateOf(settings.showScoreNameOnChange) }

    var strokes by remember(pdf.uri, pageIndex) {
        mutableStateOf(annotationStore.load(pdf.uri, pageIndex))
    }
    var textAnnotations by remember(pdf.uri, pageIndex) {
        mutableStateOf(annotationStore.loadTexts(pdf.uri, pageIndex))
    }
    var shapes by remember(pdf.uri, pageIndex) {
        mutableStateOf(annotationStore.loadShapes(pdf.uri, pageIndex))
    }
    var textColor by remember(pdf.uri) { mutableStateOf(Color(0xFF8A2430)) }
    var textSizeFraction by remember(pdf.uri) { mutableStateOf(0.026f) }
    var textBold by remember(pdf.uri) { mutableStateOf(false) }
    var textItalic by remember(pdf.uri) { mutableStateOf(false) }
    var textUnderline by remember(pdf.uri) { mutableStateOf(false) }
    var textAlignment by remember(pdf.uri) { mutableStateOf(AnnotationTextAlign.LEFT) }
    var textBackgroundEnabled by remember(pdf.uri) { mutableStateOf(false) }
    var textBorderEnabled by remember(pdf.uri) { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var textDraft by remember { mutableStateOf("") }
    var pendingTextPoint by remember { mutableStateOf<AnnotationPoint?>(null) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var selectedTextId by remember(pdf.uri, pageIndex) { mutableStateOf<String?>(null) }
    var selectedShapeId by remember(pdf.uri, pageIndex) { mutableStateOf<String?>(null) }
    var lastTextTapId by remember(pdf.uri, pageIndex) { mutableStateOf<String?>(null) }
    var lastTextTapTimeMs by remember(pdf.uri, pageIndex) { mutableStateOf(0L) }
    var undoStack by remember(pdf.uri, pageIndex) { mutableStateOf<List<AnnotationSnapshot>>(emptyList()) }
    var redoStack by remember(pdf.uri, pageIndex) { mutableStateOf<List<AnnotationSnapshot>>(emptyList()) }
    var inProgressStroke by remember(pdf.uri, pageIndex) { mutableStateOf<AnnotationStroke?>(null) }
    var inProgressShape by remember(pdf.uri, pageIndex) { mutableStateOf<AnnotationShape?>(null) }

    BackHandler {
        if (annotationMode) {
            annotationMode = false
        } else {
            onClose()
        }
    }

    ViewerEnvironment(activity, settings)

    LaunchedEffect(pdf.uri, startMode) {
        pageCount = null
        countError = null
        repository.pageCount(pdf.uri)
            .onSuccess { count ->
                pageCount = count
                pageIndex = when (startMode) {
                    ViewerStartMode.RESUME -> stateStore.loadPage(pdf.uri).coerceIn(0, (count - 1).coerceAtLeast(0))
                    ViewerStartMode.FIRST -> 0
                    ViewerStartMode.LAST -> (count - 1).coerceAtLeast(0)
                }
            }
            .onFailure { countError = it.message ?: context.getString(R.string.viewer_open_error) }
    }

    LaunchedEffect(menuVisible, annotationMode) {
        if (menuVisible && !annotationMode) {
            delay(settings.menuHideSeconds.coerceIn(1, 10) * 1000L)
            menuVisible = false
        }
    }

    LaunchedEffect(pdf.uri, settings.showScoreNameOnChange) {
        if (settings.showScoreNameOnChange) {
            showScoreNameBanner = true
            delay(1200)
            showScoreNameBanner = false
        } else {
            showScoreNameBanner = false
        }
    }

    LaunchedEffect(pageIndex, pdf.uri) {
        translation = Offset.Zero
        annotationMode = false
        selectedTextId = null
        selectedShapeId = null
        stateStore.savePage(pdf.uri, pageIndex)
    }

    LaunchedEffect(scale, pdf.uri, settings.rememberZoom) {
        if (settings.rememberZoom) {
            delay(500)
            stateStore.saveZoom(pdf.uri, scale)
        }
    }

    val renderWidth = remember(viewportSize) {
        if (viewportSize.width > 0) {
            (viewportSize.width * 1.30f).roundToInt().coerceIn(1200, 2200)
        } else {
            1600
        }
    }

    val renderKey = "${pdf.uri}|$pageIndex|$renderWidth|$pageCount|${pageAdjustment.signature()}"
    val pageResult by produceState<Result<Bitmap>?>(
        initialValue = null,
        key1 = renderKey,
    ) {
        value = if ((pageCount ?: 0) > 0) {
            repository.renderPage(pdf.uri, pageIndex, renderWidth, pageAdjustment)
        } else {
            null
        }
    }
    val bitmap = pageResult?.getOrNull()

    LaunchedEffect(pageIndex, renderWidth, pageCount, pdf.uri) {
        val count = pageCount ?: return@LaunchedEffect
        val neighbours = listOf(pageIndex - 1, pageIndex + 1).filter { it in 0 until count }
        repository.prefetch(pdf.uri, neighbours, renderWidth) { neighbour ->
            pageAdjustmentStore.load(pdf.uri, neighbour)
        }
    }

    fun previousPage() {
        val count = pageCount ?: return
        if (count <= 0 || annotationMode) return
        if (pageIndex > 0) pageIndex -= 1 else onPreviousScore()
    }

    fun nextPage() {
        val count = pageCount ?: return
        if (count <= 0 || annotationMode) return
        if (pageIndex < count - 1) pageIndex += 1 else onNextScore()
    }

    fun snapshot(): AnnotationSnapshot = AnnotationSnapshot(strokes, textAnnotations, shapes)

    fun pushUndo() {
        undoStack = undoStack + snapshot()
        redoStack = emptyList()
    }

    fun persistStrokes(updated: List<AnnotationStroke>, addUndo: Boolean = true) {
        if (addUndo) pushUndo()
        strokes = updated
        annotationStore.save(pdf.uri, pageIndex, updated)
    }

    fun persistTexts(updated: List<AnnotationText>, addUndo: Boolean = true) {
        if (addUndo) pushUndo()
        textAnnotations = updated
        annotationStore.saveTexts(pdf.uri, pageIndex, updated)
    }

    fun persistShapes(updated: List<AnnotationShape>, addUndo: Boolean = true) {
        if (addUndo) pushUndo()
        shapes = updated
        annotationStore.saveShapes(pdf.uri, pageIndex, updated)
    }

    fun restoreSnapshot(value: AnnotationSnapshot) {
        strokes = value.strokes
        textAnnotations = value.texts
        shapes = value.shapes
        annotationStore.save(pdf.uri, pageIndex, value.strokes)
        annotationStore.saveTexts(pdf.uri, pageIndex, value.texts)
        annotationStore.saveShapes(pdf.uri, pageIndex, value.shapes)
        selectedTextId = selectedTextId?.takeIf { id -> value.texts.any { it.id == id } }
        selectedShapeId = selectedShapeId?.takeIf { id -> value.shapes.any { it.id == id } }
    }

    fun undo() {
        val previous = undoStack.lastOrNull() ?: return
        redoStack = redoStack + snapshot()
        undoStack = undoStack.dropLast(1)
        restoreSnapshot(previous)
    }

    fun redo() {
        val next = redoStack.lastOrNull() ?: return
        undoStack = undoStack + snapshot()
        redoStack = redoStack.dropLast(1)
        restoreSnapshot(next)
    }

    fun remapSnapshot(
        source: AnnotationSnapshot,
        from: PdfPageAdjustment,
        to: PdfPageAdjustment,
    ): AnnotationSnapshot = AnnotationSnapshot(
        strokes = source.strokes.map { remapAnnotationStroke(it, from, to) },
        texts = source.texts.map { remapAnnotationText(it, from, to) },
        shapes = source.shapes.map { remapAnnotationShape(it, from, to) },
    )

    fun applyAdjustmentToPage(targetPage: Int, newValue: PdfPageAdjustment) {
        val target = newValue.normalized()
        val old = if (targetPage == pageIndex) pageAdjustment else pageAdjustmentStore.load(pdf.uri, targetPage)
        if (old.signature() == target.signature()) return

        val source = if (targetPage == pageIndex) {
            snapshot()
        } else {
            AnnotationSnapshot(
                strokes = annotationStore.load(pdf.uri, targetPage),
                texts = annotationStore.loadTexts(pdf.uri, targetPage),
                shapes = annotationStore.loadShapes(pdf.uri, targetPage),
            )
        }
        val mapped = if (old.sameGeometryAs(target)) source else remapSnapshot(source, old, target)
        annotationStore.save(pdf.uri, targetPage, mapped.strokes)
        annotationStore.saveTexts(pdf.uri, targetPage, mapped.texts)
        annotationStore.saveShapes(pdf.uri, targetPage, mapped.shapes)
        pageAdjustmentStore.save(pdf.uri, targetPage, target)

        if (targetPage == pageIndex) {
            strokes = mapped.strokes
            textAnnotations = mapped.texts
            shapes = mapped.shapes
            pageAdjustment = target
            selectedTextId = null
            selectedShapeId = null
            undoStack = emptyList()
            redoStack = emptyList()
            scale = 1f
            translation = Offset.Zero
        }
    }

    fun applyAdjustmentToAllPages(newValue: PdfPageAdjustment) {
        val count = pageCount ?: return
        val target = newValue.normalized()
        for (targetPage in 0 until count) {
            applyAdjustmentToPage(targetPage, target)
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 3f)
        scale = newScale
        if (newScale <= 1.01f) {
            translation = Offset.Zero
        } else {
            val maxX = viewportSize.width * (newScale - 1f) * 0.5f
            val maxY = viewportSize.height * (newScale - 1f) * 0.5f
            translation = Offset(
                x = (translation.x + panChange.x).coerceIn(-maxX, maxX),
                y = (translation.y + panChange.y).coerceIn(-maxY, maxY),
            )
        }
    }

    fun contentRectFor(bitmapValue: Bitmap): Rect {
        val vw = viewportSize.width.toFloat().coerceAtLeast(1f)
        val vh = viewportSize.height.toFloat().coerceAtLeast(1f)
        val imageAspect = bitmapValue.width.toFloat() / bitmapValue.height.toFloat().coerceAtLeast(1f)
        val viewAspect = vw / vh
        return if (imageAspect >= viewAspect) {
            val h = vw / imageAspect
            val top = (vh - h) / 2f
            Rect(0f, top, vw, top + h)
        } else {
            val w = vh * imageAspect
            val left = (vw - w) / 2f
            Rect(left, 0f, left + w, vh)
        }
    }

    fun screenToNormalized(screen: Offset, bitmapValue: Bitmap): AnnotationPoint? {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return null
        val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
        val base = Offset(
            x = ((screen.x - center.x - translation.x) / scale) + center.x,
            y = ((screen.y - center.y - translation.y) / scale) + center.y,
        )
        val rect = contentRectFor(bitmapValue)
        if (!rect.contains(base)) return null
        return AnnotationPoint(
            x = ((base.x - rect.left) / rect.width).coerceIn(0f, 1f),
            y = ((base.y - rect.top) / rect.height).coerceIn(0f, 1f),
        )
    }

    fun eraseAt(point: AnnotationPoint, startingSnapshot: List<AnnotationStroke>): List<AnnotationStroke> {
        val radius = 0.022f
        return startingSnapshot.filterNot { stroke ->
            stroke.points.any { p -> hypot((p.x - point.x).toDouble(), (p.y - point.y).toDouble()) < radius.toDouble() }
        }
    }

    fun textContains(item: AnnotationText, point: AnnotationPoint, margin: Float = 0f): Boolean {
        return point.x >= item.x - margin && point.x <= item.x + item.widthFraction + margin &&
            point.y >= item.y - margin && point.y <= item.y + item.heightFraction + margin
    }

    fun normalizedToScreen(point: AnnotationPoint, bitmapValue: Bitmap): Offset {
        val center = Offset(viewportSize.width / 2f, viewportSize.height / 2f)
        val rect = contentRectFor(bitmapValue)
        val base = Offset(
            x = rect.left + point.x * rect.width,
            y = rect.top + point.y * rect.height,
        )
        return Offset(
            x = center.x + (base.x - center.x) * scale + translation.x,
            y = center.y + (base.y - center.y) * scale + translation.y,
        )
    }

    fun screenContentRectFor(bitmapValue: Bitmap): Rect {
        val topLeft = normalizedToScreen(AnnotationPoint(0f, 0f), bitmapValue)
        val bottomRight = normalizedToScreen(AnnotationPoint(1f, 1f), bitmapValue)
        return Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    fun textScreenRect(item: AnnotationText, bitmapValue: Bitmap): Rect {
        val topLeft = normalizedToScreen(AnnotationPoint(item.x, item.y), bitmapValue)
        val bottomRight = normalizedToScreen(
            AnnotationPoint(item.x + item.widthFraction, item.y + item.heightFraction),
            bitmapValue,
        )
        return Rect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)
    }

    fun textResizeHandleAtScreen(item: AnnotationText, screen: Offset, bitmapValue: Bitmap): TextResizeHandle? {
        // Fixed screen-space target: easy to grab regardless of zoom, PDF aspect ratio or box size.
        // Inside the box only the immediate corner area resizes; the rest always moves the box.
        val radiusPx = with(density) { 26.dp.toPx() }
        val innerCornerPx = with(density) { 11.dp.toPx() }
        val rect = textScreenRect(item, bitmapValue)
        val insideBox = rect.contains(screen)
        val corners = listOf(
            TextResizeHandle.TOP_LEFT to Offset(rect.left, rect.top),
            TextResizeHandle.TOP_RIGHT to Offset(rect.right, rect.top),
            TextResizeHandle.BOTTOM_LEFT to Offset(rect.left, rect.bottom),
            TextResizeHandle.BOTTOM_RIGHT to Offset(rect.right, rect.bottom),
        )
        return corners
            .map { (handle, corner) ->
                val distance = hypot((screen.x - corner.x).toDouble(), (screen.y - corner.y).toDouble())
                val immediateInsideCorner = abs(screen.x - corner.x) <= innerCornerPx && abs(screen.y - corner.y) <= innerCornerPx
                Triple(handle, distance, !insideBox || immediateInsideCorner)
            }
            .filter { (_, distance, allowed) -> allowed && distance <= radiusPx }
            .minByOrNull { (_, distance, _) -> distance }
            ?.first
    }

    fun textContainsScreen(item: AnnotationText, screen: Offset, bitmapValue: Bitmap, paddingDp: Float = 10f): Boolean {
        val pad = with(density) { paddingDp.dp.toPx() }
        val rect = textScreenRect(item, bitmapValue)
        return screen.x >= rect.left - pad && screen.x <= rect.right + pad &&
            screen.y >= rect.top - pad && screen.y <= rect.bottom + pad
    }

    fun eraseTextAt(point: AnnotationPoint, startingSnapshot: List<AnnotationText>): List<AnnotationText> {
        return startingSnapshot.filterNot { item -> textContains(item, point, 0.012f) }
    }

    fun textAtScreen(screen: Offset, bitmapValue: Bitmap): AnnotationText? {
        return textAnnotations.lastOrNull { item -> textContainsScreen(item, screen, bitmapValue, paddingDp = 8f) }
    }

    fun textAt(point: AnnotationPoint): AnnotationText? {
        return textAnnotations.lastOrNull { item -> textContains(item, point, 0.012f) }
    }

    fun shapeBounds(item: AnnotationShape): Rect {
        val left = min(item.x1, item.x2)
        val top = min(item.y1, item.y2)
        val right = max(item.x1, item.x2)
        val bottom = max(item.y1, item.y2)
        return Rect(left, top, right, bottom)
    }

    fun shapeContains(item: AnnotationShape, point: AnnotationPoint): Boolean {
        val margin = 0.025f
        val bounds = shapeBounds(item)
        if (item.type == AnnotationShapeType.RECTANGLE || item.type == AnnotationShapeType.ELLIPSE) {
            return point.x in (bounds.left - margin)..(bounds.right + margin) &&
                point.y in (bounds.top - margin)..(bounds.bottom + margin)
        }
        val dx = item.x2 - item.x1
        val dy = item.y2 - item.y1
        val lengthSq = dx * dx + dy * dy
        if (lengthSq < 0.00001f) return false
        val u = (((point.x - item.x1) * dx + (point.y - item.y1) * dy) / lengthSq).coerceIn(0f, 1f)
        val px = item.x1 + u * dx
        val py = item.y1 + u * dy
        return hypot((point.x - px).toDouble(), (point.y - py).toDouble()) < margin
    }

    fun shapeResizeHandleContains(item: AnnotationShape, point: AnnotationPoint): Boolean {
        return hypot((point.x - item.x2).toDouble(), (point.y - item.y2).toDouble()) < 0.035
    }

    fun shapeAt(point: AnnotationPoint): AnnotationShape? = shapes.lastOrNull { shapeContains(it, point) }

    fun eraseShapeAt(point: AnnotationPoint, startingSnapshot: List<AnnotationShape>): List<AnnotationShape> {
        return startingSnapshot.filterNot { shapeContains(it, point) }
    }

    // Keep the gesture detector alive while annotation state changes during a drag.
    // These holders always expose the newest values without becoming pointerInput keys.
    val latestStrokes by rememberUpdatedState(strokes)
    val latestTexts by rememberUpdatedState(textAnnotations)
    val latestShapes by rememberUpdatedState(shapes)
    val latestSelectedTextId by rememberUpdatedState(selectedTextId)
    val latestSelectedShapeId by rememberUpdatedState(selectedShapeId)

    fun openTextEditor(item: AnnotationText) {
        editingTextId = item.id
        pendingTextPoint = AnnotationPoint(item.x, item.y)
        textDraft = item.text
        textColor = Color(item.colorArgb)
        textSizeFraction = item.sizeFraction
        textBold = item.bold
        textItalic = item.italic
        textUnderline = item.underline
        textAlignment = item.alignment
        textBackgroundEnabled = item.backgroundEnabled
        textBorderEnabled = item.borderEnabled
        showTextDialog = true
    }

    val viewerGestureModifier = if (!annotationMode) {
        Modifier
            .pointerInput(pdf.uri, pageIndex, pageCount) {
                detectTapGestures { tap ->
                    val fraction = if (size.width > 0) tap.x / size.width.toFloat() else 0.5f
                    when {
                        fraction < settings.sideTapFraction -> previousPage()
                        fraction > 1f - settings.sideTapFraction -> nextPage()
                        else -> menuVisible = !menuVisible
                    }
                }
            }
            .transformable(state = transformState)
    } else {
        Modifier.pointerInput(
            pdf.uri,
            pageIndex,
            annotationUiTool,
            bitmap,
        ) {
            val bitmapValue = bitmap ?: return@pointerInput
            awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                val initialPoint = screenToNormalized(firstDown.position, bitmapValue)
                var currentPoints = mutableListOf<AnnotationPoint>()
                var hadMultiTouch = false
                var eraserStrokes = latestStrokes
                var eraserTexts = latestTexts
                var eraserShapes = latestShapes
                val originalSnapshot = AnnotationSnapshot(latestStrokes, latestTexts, latestShapes)
                var workingTexts = originalSnapshot.texts
                var workingShapes = originalSnapshot.shapes
                var textTapPoint: AnnotationPoint? = null
                var shapeStartPoint: AnnotationPoint? = null
                var changed = false
                var textDragStarted = false

                var selectedTextOriginal: AnnotationText? = null
                var selectedShapeOriginal: AnnotationShape? = null
                var textResizeHandle: TextResizeHandle? = null
                var resizingShape = false

                when (annotationUiTool) {
                    AnnotationUiTool.SELECT -> {
                        val currentText = latestSelectedTextId?.let { id -> latestTexts.firstOrNull { it.id == id } }
                        val currentShape = latestSelectedShapeId?.let { id -> latestShapes.firstOrNull { it.id == id } }

                        // Text manipulation is resolved in screen pixels so that the handles never
                        // become tiny/huge with zoom and never overlap unpredictably in small boxes.
                        val currentHandle = currentText?.let { textResizeHandleAtScreen(it, firstDown.position, bitmapValue) }
                        val hitCurrentText = currentText?.takeIf {
                            currentHandle != null || textContainsScreen(it, firstDown.position, bitmapValue, paddingDp = 10f)
                        }
                        val hitText = hitCurrentText ?: latestTexts.lastOrNull { item ->
                            textContainsScreen(item, firstDown.position, bitmapValue, paddingDp = 8f)
                        }
                        val hitShape = if (hitText == null && initialPoint != null) {
                            currentShape?.takeIf { shapeResizeHandleContains(it, initialPoint) }
                                ?: latestShapes.lastOrNull { shapeContains(it, initialPoint) }
                        } else null

                        selectedTextId = hitText?.id
                        selectedShapeId = hitShape?.id
                        selectedTextOriginal = hitText
                        selectedShapeOriginal = hitShape
                        textResizeHandle = if (hitText?.id == currentText?.id) currentHandle else null
                        resizingShape = if (initialPoint != null) {
                            hitShape?.let { shapeResizeHandleContains(it, initialPoint) } == true
                        } else false
                    }
                    AnnotationUiTool.ERASER -> {
                        initialPoint?.let { point ->
                            eraserStrokes = eraseAt(point, eraserStrokes)
                            eraserTexts = eraseTextAt(point, eraserTexts)
                            eraserShapes = eraseShapeAt(point, eraserShapes)
                            changed = eraserStrokes != strokes || eraserTexts != textAnnotations || eraserShapes != shapes
                        }
                    }
                    AnnotationUiTool.TEXT -> textTapPoint = initialPoint
                    AnnotationUiTool.SHAPE -> shapeStartPoint = initialPoint
                    AnnotationUiTool.PENCIL,
                    AnnotationUiTool.HIGHLIGHTER -> initialPoint?.let(currentPoints::add)
                }
                firstDown.consume()

                while (true) {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.isEmpty()) break

                    if (pressed.size >= 2) {
                        hadMultiTouch = true
                        currentPoints.clear()
                        textTapPoint = null
                        shapeStartPoint = null
                        inProgressStroke = null
                        inProgressShape = null
                        if (changed && annotationUiTool == AnnotationUiTool.SELECT) {
                            workingTexts = originalSnapshot.texts
                            workingShapes = originalSnapshot.shapes
                            textAnnotations = workingTexts
                            shapes = workingShapes
                            changed = false
                            textDragStarted = false
                        }
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val newScale = (scale * zoomChange).coerceIn(1f, 3f)
                        scale = newScale
                        if (newScale <= 1.01f) {
                            translation = Offset.Zero
                        } else {
                            val maxX = viewportSize.width * (newScale - 1f) * 0.5f
                            val maxY = viewportSize.height * (newScale - 1f) * 0.5f
                            translation = Offset(
                                x = (translation.x + panChange.x).coerceIn(-maxX, maxX),
                                y = (translation.y + panChange.y).coerceIn(-maxY, maxY),
                            )
                        }
                        event.changes.forEach { it.consume() }
                        continue
                    }

                    if (!hadMultiTouch) {
                        val change = pressed.first()
                        val point = screenToNormalized(change.position, bitmapValue)

                        if (annotationUiTool == AnnotationUiTool.SELECT && selectedTextOriginal != null) {
                            val originalText = selectedTextOriginal!!
                            // Do not start a drag on tiny finger jitter. Once the threshold is crossed,
                            // the box follows the finger continuously until pointer-up.
                            val totalDxPx = change.position.x - firstDown.position.x
                            val totalDyPx = change.position.y - firstDown.position.y
                            val dragSlopPx = with(density) { 6.dp.toPx() }
                            if (!textDragStarted && hypot(totalDxPx.toDouble(), totalDyPx.toDouble()) >= dragSlopPx) {
                                textDragStarted = true
                            }
                            if (!textDragStarted) {
                                change.consume()
                                continue
                            }

                            // Screen-space drag: robust at every zoom level and even if the finger
                            // leaves the PDF while moving/resizing the box.
                            val screenRect = screenContentRectFor(bitmapValue)
                            val dx = totalDxPx / screenRect.width.coerceAtLeast(1f)
                            val dy = totalDyPx / screenRect.height.coerceAtLeast(1f)
                            val minWidth = 0.065f
                            val minHeight = 0.035f
                            val updated = when (textResizeHandle) {
                                TextResizeHandle.TOP_LEFT -> {
                                    val right = originalText.x + originalText.widthFraction
                                    val bottom = originalText.y + originalText.heightFraction
                                    val newX = (originalText.x + dx).coerceIn(0f, right - minWidth)
                                    val newY = (originalText.y + dy).coerceIn(0f, bottom - minHeight)
                                    originalText.copy(
                                        x = newX,
                                        y = newY,
                                        widthFraction = (right - newX).coerceAtLeast(minWidth),
                                        heightFraction = (bottom - newY).coerceAtLeast(minHeight),
                                    )
                                }
                                TextResizeHandle.TOP_RIGHT -> {
                                    val bottom = originalText.y + originalText.heightFraction
                                    val newRight = (originalText.x + originalText.widthFraction + dx)
                                        .coerceIn(originalText.x + minWidth, 1f)
                                    val newY = (originalText.y + dy).coerceIn(0f, bottom - minHeight)
                                    originalText.copy(
                                        y = newY,
                                        widthFraction = newRight - originalText.x,
                                        heightFraction = (bottom - newY).coerceAtLeast(minHeight),
                                    )
                                }
                                TextResizeHandle.BOTTOM_LEFT -> {
                                    val right = originalText.x + originalText.widthFraction
                                    val newX = (originalText.x + dx).coerceIn(0f, right - minWidth)
                                    val newBottom = (originalText.y + originalText.heightFraction + dy)
                                        .coerceIn(originalText.y + minHeight, 1f)
                                    originalText.copy(
                                        x = newX,
                                        widthFraction = (right - newX).coerceAtLeast(minWidth),
                                        heightFraction = newBottom - originalText.y,
                                    )
                                }
                                TextResizeHandle.BOTTOM_RIGHT -> {
                                    val newRight = (originalText.x + originalText.widthFraction + dx)
                                        .coerceIn(originalText.x + minWidth, 1f)
                                    val newBottom = (originalText.y + originalText.heightFraction + dy)
                                        .coerceIn(originalText.y + minHeight, 1f)
                                    originalText.copy(
                                        widthFraction = newRight - originalText.x,
                                        heightFraction = newBottom - originalText.y,
                                    )
                                }
                                null -> originalText.copy(
                                    x = (originalText.x + dx).coerceIn(0f, (1f - originalText.widthFraction).coerceAtLeast(0f)),
                                    y = (originalText.y + dy).coerceIn(0f, (1f - originalText.heightFraction).coerceAtLeast(0f)),
                                )
                            }
                            workingTexts = originalSnapshot.texts.map { if (it.id == updated.id) updated else it }
                            textAnnotations = workingTexts
                            changed = true
                        } else if (point != null) {
                            when (annotationUiTool) {
                                AnnotationUiTool.SELECT -> {
                                    val originalShape = selectedShapeOriginal
                                    if (initialPoint != null && originalShape != null) {
                                        val dx = point.x - initialPoint.x
                                        val dy = point.y - initialPoint.y
                                        val updated = if (resizingShape) {
                                            originalShape.copy(x2 = point.x, y2 = point.y)
                                        } else {
                                            val minX = min(originalShape.x1, originalShape.x2)
                                            val maxX = max(originalShape.x1, originalShape.x2)
                                            val minY = min(originalShape.y1, originalShape.y2)
                                            val maxY = max(originalShape.y1, originalShape.y2)
                                            val clampedDx = dx.coerceIn(-minX, 1f - maxX)
                                            val clampedDy = dy.coerceIn(-minY, 1f - maxY)
                                            originalShape.copy(
                                                x1 = originalShape.x1 + clampedDx,
                                                y1 = originalShape.y1 + clampedDy,
                                                x2 = originalShape.x2 + clampedDx,
                                                y2 = originalShape.y2 + clampedDy,
                                            )
                                        }
                                        workingShapes = originalSnapshot.shapes.map { if (it.id == updated.id) updated else it }
                                        shapes = workingShapes
                                        changed = true
                                    }
                                }
                                AnnotationUiTool.ERASER -> {
                                    eraserStrokes = eraseAt(point, eraserStrokes)
                                    eraserTexts = eraseTextAt(point, eraserTexts)
                                    eraserShapes = eraseShapeAt(point, eraserShapes)
                                    changed = eraserStrokes != strokes || eraserTexts != textAnnotations || eraserShapes != shapes
                                }
                                AnnotationUiTool.TEXT -> textTapPoint = point
                                AnnotationUiTool.SHAPE -> {
                                    val startPoint = shapeStartPoint
                                    if (startPoint != null) {
                                        inProgressShape = AnnotationShape(
                                            id = "preview",
                                            type = currentShapeType,
                                            x1 = startPoint.x,
                                            y1 = startPoint.y,
                                            x2 = point.x,
                                            y2 = point.y,
                                            colorArgb = shapeColor.toArgb(),
                                            widthFraction = shapeWidth,
                                            fillEnabled = shapeFillEnabled,
                                        )
                                    }
                                }
                                AnnotationUiTool.PENCIL,
                                AnnotationUiTool.HIGHLIGHTER -> {
                                    currentPoints.add(point)
                                    val isHighlighter = annotationUiTool == AnnotationUiTool.HIGHLIGHTER
                                    val chosenColor = if (isHighlighter) highlighterColor else pencilColor
                                    val width = if (isHighlighter) highlighterWidth else pencilWidth
                                    inProgressStroke = AnnotationStroke(
                                        tool = if (isHighlighter) AnnotationTool.HIGHLIGHTER else AnnotationTool.PENCIL,
                                        colorArgb = chosenColor.toArgb(),
                                        widthFraction = width,
                                        points = currentPoints.toList(),
                                    )
                                }
                            }
                        }
                        change.consume()
                    }
                }

                if (!hadMultiTouch) {
                    when (annotationUiTool) {
                        AnnotationUiTool.SELECT -> {
                            if (changed) {
                                undoStack = undoStack + originalSnapshot
                                redoStack = emptyList()
                                annotationStore.saveTexts(pdf.uri, pageIndex, workingTexts)
                                annotationStore.saveShapes(pdf.uri, pageIndex, workingShapes)
                            } else {
                                // Double tap on a text box opens its editor directly.
                                selectedTextOriginal?.let { item ->
                                    val now = SystemClock.uptimeMillis()
                                    if (lastTextTapId == item.id && now - lastTextTapTimeMs <= 420L) {
                                        openTextEditor(item)
                                        lastTextTapId = null
                                        lastTextTapTimeMs = 0L
                                    } else {
                                        lastTextTapId = item.id
                                        lastTextTapTimeMs = now
                                    }
                                } ?: run {
                                    lastTextTapId = null
                                    lastTextTapTimeMs = 0L
                                }
                            }
                        }
                        AnnotationUiTool.ERASER -> {
                            if (changed) {
                                undoStack = undoStack + originalSnapshot
                                redoStack = emptyList()
                                strokes = eraserStrokes
                                textAnnotations = eraserTexts
                                shapes = eraserShapes
                                annotationStore.save(pdf.uri, pageIndex, eraserStrokes)
                                annotationStore.saveTexts(pdf.uri, pageIndex, eraserTexts)
                                annotationStore.saveShapes(pdf.uri, pageIndex, eraserShapes)
                                selectedTextId = selectedTextId?.takeIf { id -> eraserTexts.any { it.id == id } }
                                selectedShapeId = selectedShapeId?.takeIf { id -> eraserShapes.any { it.id == id } }
                            }
                        }
                        AnnotationUiTool.TEXT -> {
                            textTapPoint?.let { point ->
                                val existing = originalSnapshot.texts.lastOrNull { item -> textContains(item, point, 0.012f) }
                                pendingTextPoint = existing?.let { AnnotationPoint(it.x, it.y) } ?: point
                                editingTextId = existing?.id
                                selectedTextId = existing?.id
                                selectedShapeId = null
                                textDraft = existing?.text.orEmpty()
                                if (existing != null) {
                                    textColor = Color(existing.colorArgb)
                                    textSizeFraction = existing.sizeFraction
                                    textBold = existing.bold
                                    textItalic = existing.italic
                                    textUnderline = existing.underline
                                    textAlignment = existing.alignment
                                    textBackgroundEnabled = existing.backgroundEnabled
                                    textBorderEnabled = existing.borderEnabled
                                }
                                showTextDialog = true
                            }
                        }
                        AnnotationUiTool.SHAPE -> {
                            val preview = inProgressShape
                            if (preview != null && hypot(
                                    (preview.x2 - preview.x1).toDouble(),
                                    (preview.y2 - preview.y1).toDouble(),
                                ) > 0.015
                            ) {
                                val item = preview.copy(id = UUID.randomUUID().toString())
                                persistShapes(shapes + item)
                                selectedShapeId = item.id
                                selectedTextId = null
                            }
                            inProgressShape = null
                        }
                        AnnotationUiTool.PENCIL,
                        AnnotationUiTool.HIGHLIGHTER -> {
                            if (currentPoints.isNotEmpty()) {
                                val isHighlighter = annotationUiTool == AnnotationUiTool.HIGHLIGHTER
                                val chosenColor = if (isHighlighter) highlighterColor else pencilColor
                                val width = if (isHighlighter) highlighterWidth else pencilWidth
                                val stroke = AnnotationStroke(
                                    tool = if (isHighlighter) AnnotationTool.HIGHLIGHTER else AnnotationTool.PENCIL,
                                    colorArgb = chosenColor.toArgb(),
                                    widthFraction = width,
                                    points = currentPoints.toList(),
                                )
                                persistStrokes(strokes + stroke)
                                inProgressStroke = null
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        when {
            countError != null -> ViewerError(message = countError!!, onClose = onClose)
            pageCount == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Burgundy)
            }
            pageCount == 0 -> ViewerError(message = stringResource(R.string.viewer_pdf_empty), onClose = onClose)
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { viewportSize = it }
                        .then(viewerGestureModifier),
                    contentAlignment = Alignment.Center,
                ) {
                    when (val result = pageResult) {
                        null -> CircularProgressIndicator(color = Burgundy)
                        else -> result.fold(
                            onSuccess = { renderedBitmap ->
                                Image(
                                    bitmap = renderedBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.page_of_score, pageIndex + 1, pdf.displayName),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = translation.x,
                                            translationY = translation.y,
                                        ),
                                )

                                if (annotationsVisible) {
                                    AnnotationCanvas(
                                        strokes = if (inProgressStroke != null) strokes + listOf(inProgressStroke!!) else strokes,
                                        texts = textAnnotations,
                                        shapes = if (inProgressShape != null) shapes + listOf(inProgressShape!!) else shapes,
                                        selectedTextId = if (annotationMode && annotationUiTool == AnnotationUiTool.SELECT) selectedTextId else null,
                                        selectedShapeId = if (annotationMode && annotationUiTool == AnnotationUiTool.SELECT) selectedShapeId else null,
                                        bitmap = renderedBitmap,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = translation.x,
                                                translationY = translation.y,
                                            ),
                                    )
                                }
                            },
                            onFailure = { error ->
                                ViewerError(
                                    message = error.message ?: context.getString(R.string.viewer_render_error),
                                    onClose = onClose,
                                )
                            },
                        )
                    }
                }
            }
        }

        if (!annotationMode && showScoreNameBanner && !menuVisible) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp),
                color = Color.White.copy(alpha = 0.88f),
                shape = RoundedCornerShape(14.dp),
                elevation = 3.dp,
            ) {
                Text(
                    pdf.displayName,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!annotationMode && menuVisible && (pageCount ?: 0) > 0 && countError == null) {
            ViewerMenu(
                title = pdf.displayName,
                pageIndex = pageIndex,
                pageCount = pageCount ?: 0,
                scorePosition = scorePosition,
                totalScores = totalScores,
                zoom = scale,
                isFavorite = isFavorite,
                annotationsVisible = annotationsVisible,
                hasAnnotations = strokes.isNotEmpty() || textAnnotations.isNotEmpty() || shapes.isNotEmpty(),
                onToggleFavorite = onToggleFavorite,
                onClose = onClose,
                onOpenScoreList = {
                    menuVisible = false
                    showScorePicker = true
                },
                onResetZoom = {
                    scale = 1f
                    translation = Offset.Zero
                },
                rotationDegrees = pageAdjustment.rotationQuarterTurns * 90,
                onOpenTools = {
                    menuVisible = false
                    showQuickTools = true
                },
                onRotateLeft = {
                    applyAdjustmentToPage(
                        pageIndex,
                        pageAdjustment.copy(rotationQuarterTurns = pageAdjustment.rotationQuarterTurns - 1),
                    )
                },
                onRotateRight = {
                    applyAdjustmentToPage(
                        pageIndex,
                        pageAdjustment.copy(rotationQuarterTurns = pageAdjustment.rotationQuarterTurns + 1),
                    )
                },
                onOpenRepair = {
                    menuVisible = false
                    showRepairDialog = true
                },
                onStartAnnotations = {
                    menuVisible = false
                    annotationMode = true
                    annotationsVisible = true
                },
                onToggleAnnotationsVisible = { annotationsVisible = !annotationsVisible },
            )
        }

        if (showQuickTools) {
            ViewerToolsDialog(
                tunerReferenceHz = tunerReferenceHz,
                onTunerReferenceChange = onTunerReferenceChange,
                onDismiss = { showQuickTools = false },
            )
        }

        if (showRepairDialog) {
            PdfRepairDialog(
                current = pageAdjustment,
                pageIndex = pageIndex,
                pageCount = pageCount ?: 1,
                onDismiss = { showRepairDialog = false },
                onApplyCurrent = { adjustment ->
                    applyAdjustmentToPage(pageIndex, adjustment)
                    showRepairDialog = false
                },
                onApplyAll = { adjustment ->
                    applyAdjustmentToAllPages(adjustment)
                    showRepairDialog = false
                },
                onResetCurrent = {
                    applyAdjustmentToPage(pageIndex, PdfPageAdjustment())
                    showRepairDialog = false
                },
            )
        }

        val selectedText = selectedTextId?.let { id -> textAnnotations.firstOrNull { it.id == id } }
        val selectedShape = selectedShapeId?.let { id -> shapes.firstOrNull { it.id == id } }

        if (annotationMode && bitmap != null && countError == null) {
            AnnotationToolbar(
                selectedTool = annotationUiTool,
                currentColor = when {
                    annotationUiTool == AnnotationUiTool.SELECT && selectedText != null -> Color(selectedText.colorArgb)
                    annotationUiTool == AnnotationUiTool.SELECT && selectedShape != null -> Color(selectedShape.colorArgb)
                    annotationUiTool == AnnotationUiTool.HIGHLIGHTER -> highlighterColor
                    annotationUiTool == AnnotationUiTool.TEXT -> textColor
                    annotationUiTool == AnnotationUiTool.SHAPE -> shapeColor
                    else -> pencilColor
                },
                pencilWidth = pencilWidth,
                highlighterWidth = highlighterWidth,
                textSizeFraction = textSizeFraction,
                currentShapeType = selectedShape?.type ?: currentShapeType,
                shapeFillEnabled = selectedShape?.fillEnabled ?: shapeFillEnabled,
                selectedText = selectedText,
                selectedShape = selectedShape,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                onTool = {
                    annotationUiTool = it
                    if (it != AnnotationUiTool.SELECT) {
                        selectedTextId = null
                        selectedShapeId = null
                    }
                },
                onColor = { showColorDialog = true },
                onCycleWidth = {
                    when (annotationUiTool) {
                        AnnotationUiTool.HIGHLIGHTER -> {
                            highlighterWidth = when {
                                highlighterWidth < 0.014f -> 0.018f
                                highlighterWidth < 0.024f -> 0.030f
                                else -> 0.010f
                            }
                        }
                        AnnotationUiTool.TEXT -> {
                            textSizeFraction = when {
                                textSizeFraction < 0.024f -> 0.030f
                                textSizeFraction < 0.038f -> 0.046f
                                else -> 0.020f
                            }
                        }
                        AnnotationUiTool.SHAPE -> {
                            shapeWidth = when {
                                shapeWidth < 0.0035f -> 0.006f
                                shapeWidth < 0.008f -> 0.011f
                                else -> 0.0025f
                            }
                        }
                        else -> {
                            pencilWidth = when {
                                pencilWidth < 0.0035f -> 0.0055f
                                pencilWidth < 0.007f -> 0.009f
                                else -> 0.0025f
                            }
                        }
                    }
                },
                onCycleShapeType = {
                    val next = when (selectedShape?.type ?: currentShapeType) {
                        AnnotationShapeType.LINE -> AnnotationShapeType.ARROW
                        AnnotationShapeType.ARROW -> AnnotationShapeType.RECTANGLE
                        AnnotationShapeType.RECTANGLE -> AnnotationShapeType.ELLIPSE
                        AnnotationShapeType.ELLIPSE -> AnnotationShapeType.LINE
                    }
                    if (selectedShape != null) {
                        persistShapes(shapes.map { if (it.id == selectedShape.id) it.copy(type = next) else it })
                    } else {
                        currentShapeType = next
                    }
                },
                onToggleShapeFill = {
                    if (selectedShape != null) {
                        persistShapes(shapes.map { if (it.id == selectedShape.id) it.copy(fillEnabled = !it.fillEnabled) else it })
                    } else {
                        shapeFillEnabled = !shapeFillEnabled
                    }
                },
                onEditSelectedText = {
                    selectedText?.let(::openTextEditor)
                },
                onTextSizeDelta = { delta ->
                    selectedText?.let { item ->
                        val size = (item.sizeFraction + delta).coerceIn(0.012f, 0.09f)
                        persistTexts(textAnnotations.map { if (it.id == item.id) it.copy(sizeFraction = size) else it })
                    }
                },
                onToggleBold = {
                    selectedText?.let { item ->
                        persistTexts(textAnnotations.map { if (it.id == item.id) it.copy(bold = !it.bold) else it })
                    }
                },
                onToggleItalic = {
                    selectedText?.let { item ->
                        persistTexts(textAnnotations.map { if (it.id == item.id) it.copy(italic = !it.italic) else it })
                    }
                },
                onToggleUnderline = {
                    selectedText?.let { item ->
                        persistTexts(textAnnotations.map { if (it.id == item.id) it.copy(underline = !it.underline) else it })
                    }
                },
                onCycleTextAlign = {
                    selectedText?.let { item ->
                        val next = when (item.alignment) {
                            AnnotationTextAlign.LEFT -> AnnotationTextAlign.CENTER
                            AnnotationTextAlign.CENTER -> AnnotationTextAlign.RIGHT
                            AnnotationTextAlign.RIGHT -> AnnotationTextAlign.LEFT
                        }
                        persistTexts(textAnnotations.map { if (it.id == item.id) it.copy(alignment = next) else it })
                    }
                },
                onToggleTextBackground = {
                    selectedText?.let { item ->
                        persistTexts(textAnnotations.map { if (it.id == item.id) it.copy(backgroundEnabled = !it.backgroundEnabled) else it })
                    }
                },
                onToggleTextBorder = {
                    selectedText?.let { item ->
                        persistTexts(textAnnotations.map { if (it.id == item.id) it.copy(borderEnabled = !it.borderEnabled) else it })
                    }
                },
                onNudgeText = { dx, dy ->
                    selectedText?.let { item ->
                        val moved = item.copy(
                            x = (item.x + dx).coerceIn(0f, (1f - item.widthFraction).coerceAtLeast(0f)),
                            y = (item.y + dy).coerceIn(0f, (1f - item.heightFraction).coerceAtLeast(0f)),
                        )
                        persistTexts(textAnnotations.map { if (it.id == item.id) moved else it })
                    }
                },
                onDeleteSelection = {
                    when {
                        selectedText != null -> {
                            persistTexts(textAnnotations.filterNot { it.id == selectedText.id })
                            selectedTextId = null
                        }
                        selectedShape != null -> {
                            persistShapes(shapes.filterNot { it.id == selectedShape.id })
                            selectedShapeId = null
                        }
                    }
                },
                onUndo = ::undo,
                onRedo = ::redo,
                onClear = { if (strokes.isNotEmpty() || textAnnotations.isNotEmpty() || shapes.isNotEmpty()) showClearDialog = true },
                onDone = {
                    annotationMode = false
                    selectedTextId = null
                    selectedShapeId = null
                },
            )
        }

        if (showScorePicker) {
            ScorePickerDialog(
                scores = scoreList,
                currentIndex = scorePosition - 1,
                onDismiss = { showScorePicker = false },
                onSelect = { index ->
                    showScorePicker = false
                    onSelectScore(index)
                },
            )
        }

        if (showColorDialog) {
            val pickerColor = when {
                annotationUiTool == AnnotationUiTool.SELECT && selectedText != null -> Color(selectedText.colorArgb)
                annotationUiTool == AnnotationUiTool.SELECT && selectedShape != null -> Color(selectedShape.colorArgb)
                annotationUiTool == AnnotationUiTool.HIGHLIGHTER -> highlighterColor
                annotationUiTool == AnnotationUiTool.TEXT -> textColor
                annotationUiTool == AnnotationUiTool.SHAPE -> shapeColor
                else -> pencilColor
            }
            ColorPickerDialog(
                selected = pickerColor,
                onDismiss = { showColorDialog = false },
                onSelect = { color ->
                    when {
                        annotationUiTool == AnnotationUiTool.SELECT && selectedText != null -> {
                            persistTexts(textAnnotations.map { if (it.id == selectedText.id) it.copy(colorArgb = color.toArgb()) else it })
                        }
                        annotationUiTool == AnnotationUiTool.SELECT && selectedShape != null -> {
                            persistShapes(shapes.map { if (it.id == selectedShape.id) it.copy(colorArgb = color.toArgb()) else it })
                        }
                        annotationUiTool == AnnotationUiTool.HIGHLIGHTER -> highlighterColor = color
                        annotationUiTool == AnnotationUiTool.TEXT -> textColor = color
                        annotationUiTool == AnnotationUiTool.SHAPE -> shapeColor = color
                        else -> pencilColor = color
                    }
                    showColorDialog = false
                },
            )
        }

        if (showTextDialog) {
            AlertDialog(
                onDismissRequest = {
                    showTextDialog = false
                    textDraft = ""
                    pendingTextPoint = null
                    editingTextId = null
                },
                title = { Text(if (editingTextId == null) stringResource(R.string.annotation_add_text) else stringResource(R.string.annotation_edit_text)) },
                text = {
                    Column(modifier = Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState())) {
                        Text(
                            stringResource(R.string.annotation_text_help),
                            color = Color.Gray,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = textDraft,
                            onValueChange = { if (it.length <= 240) textDraft = it },
                            singleLine = false,
                            maxLines = 5,
                            label = { Text(stringResource(R.string.annotation_text)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = festiveTextFieldColors(),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.annotation_quick_styles), color = Ink, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            AnnotationToolChip(stringResource(R.string.annotation_fingering), false, onClick = {
                                textColor = Color(0xFF1976D2); textSizeFraction = 0.028f; textBold = true
                                textItalic = false; textUnderline = false; textBackgroundEnabled = false; textBorderEnabled = false
                            })
                            Spacer(Modifier.width(6.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_warning), false, onClick = {
                                textColor = Color(0xFFE53935); textSizeFraction = 0.033f; textBold = true
                                textItalic = false; textUnderline = false; textBackgroundEnabled = true; textBorderEnabled = true
                            })
                            Spacer(Modifier.width(6.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_comment), false, onClick = {
                                textColor = Color(0xFF222222); textSizeFraction = 0.024f; textBold = false
                                textItalic = false; textUnderline = false; textBackgroundEnabled = true; textBorderEnabled = false
                            })
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.annotation_format), color = Ink, fontWeight = FontWeight.SemiBold)
                        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                            AnnotationToolChip("−", false, onClick = { textSizeFraction = (textSizeFraction - 0.003f).coerceAtLeast(0.012f) })
                            Spacer(Modifier.width(4.dp))
                            Text("${(textSizeFraction * 1000).roundToInt()}", modifier = Modifier.padding(horizontal = 7.dp))
                            AnnotationToolChip("+", false, onClick = { textSizeFraction = (textSizeFraction + 0.003f).coerceAtMost(0.09f) })
                            Spacer(Modifier.width(8.dp))
                            AnnotationToolChip("B", textBold, onClick = { textBold = !textBold })
                            Spacer(Modifier.width(4.dp))
                            AnnotationToolChip("I", textItalic, onClick = { textItalic = !textItalic })
                            Spacer(Modifier.width(4.dp))
                            AnnotationToolChip("U", textUnderline, onClick = { textUnderline = !textUnderline })
                            Spacer(Modifier.width(8.dp))
                            AnnotationToolChip(
                                when (textAlignment) {
                                    AnnotationTextAlign.LEFT -> stringResource(R.string.annotation_left)
                                    AnnotationTextAlign.CENTER -> stringResource(R.string.annotation_center)
                                    AnnotationTextAlign.RIGHT -> stringResource(R.string.annotation_right)
                                },
                                false,
                                onClick = {
                                    textAlignment = when (textAlignment) {
                                        AnnotationTextAlign.LEFT -> AnnotationTextAlign.CENTER
                                        AnnotationTextAlign.CENTER -> AnnotationTextAlign.RIGHT
                                        AnnotationTextAlign.RIGHT -> AnnotationTextAlign.LEFT
                                    }
                                },
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.annotation_color), color = Ink, fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            listOf(
                                Color(0xFFE53935), Color(0xFF1976D2), Color(0xFF43A047),
                                Color(0xFF8E24AA), Color(0xFF111111), Color(0xFF8A2430),
                            ).forEach { color ->
                                Surface(
                                    color = color,
                                    shape = CircleShape,
                                    elevation = if (color == textColor) 6.dp else 1.dp,
                                    modifier = Modifier.size(if (color == textColor) 34.dp else 29.dp).clickable { textColor = color },
                                ) {}
                                Spacer(Modifier.width(5.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    Text(
                        stringResource(R.string.save),
                        color = if (textDraft.isBlank()) Color.Gray else Burgundy,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(enabled = textDraft.isNotBlank()) {
                                val point = pendingTextPoint ?: return@clickable
                                val id = editingTextId ?: UUID.randomUUID().toString()
                                val existing = textAnnotations.firstOrNull { it.id == id }
                                val boxWidth = existing?.widthFraction ?: 0.24f
                                val boxHeight = existing?.heightFraction ?: 0.09f
                                val item = AnnotationText(
                                    id = id,
                                    text = textDraft.trim(),
                                    x = (existing?.x ?: point.x).coerceIn(0f, (1f - boxWidth).coerceAtLeast(0f)),
                                    y = (existing?.y ?: point.y).coerceIn(0f, (1f - boxHeight).coerceAtLeast(0f)),
                                    widthFraction = boxWidth,
                                    heightFraction = boxHeight,
                                    colorArgb = textColor.toArgb(),
                                    sizeFraction = textSizeFraction,
                                    bold = textBold,
                                    italic = textItalic,
                                    underline = textUnderline,
                                    alignment = textAlignment,
                                    backgroundEnabled = false,
                                    borderEnabled = false,
                                )
                                val updated = textAnnotations.filterNot { it.id == id } + item
                                persistTexts(updated)
                                selectedTextId = id
                                selectedShapeId = null
                                annotationUiTool = AnnotationUiTool.SELECT
                                showTextDialog = false
                                textDraft = ""
                                pendingTextPoint = null
                                editingTextId = null
                            }
                            .padding(12.dp),
                    )
                },
                dismissButton = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (editingTextId != null) {
                            Text(
                                stringResource(R.string.delete_score),
                                color = Burgundy,
                                modifier = Modifier
                                    .clickable {
                                        val id = editingTextId
                                        persistTexts(textAnnotations.filterNot { it.id == id })
                                        selectedTextId = null
                                        showTextDialog = false
                                        textDraft = ""
                                        pendingTextPoint = null
                                        editingTextId = null
                                    }
                                    .padding(12.dp),
                            )
                        }
                        Text(
                            stringResource(R.string.cancel),
                            modifier = Modifier
                                .clickable {
                                    showTextDialog = false
                                    textDraft = ""
                                    pendingTextPoint = null
                                    editingTextId = null
                                }
                                .padding(12.dp),
                        )
                    }
                },
            )
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(stringResource(R.string.annotation_delete_all)) },
                text = { Text(stringResource(R.string.annotation_delete_all_desc)) },
                confirmButton = {
                    Text(
                        stringResource(R.string.delete_score),
                        color = Burgundy,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                showClearDialog = false
                                pushUndo()
                                strokes = emptyList()
                                textAnnotations = emptyList()
                                shapes = emptyList()
                                selectedTextId = null
                                selectedShapeId = null
                                annotationStore.clearPage(pdf.uri, pageIndex)
                            }
                            .padding(12.dp),
                    )
                },
                dismissButton = {
                    Text(
                        stringResource(R.string.cancel),
                        modifier = Modifier.clickable { showClearDialog = false }.padding(12.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun AnnotationCanvas(
    strokes: List<AnnotationStroke>,
    texts: List<AnnotationText>,
    shapes: List<AnnotationShape>,
    selectedTextId: String?,
    selectedShapeId: String?,
    bitmap: Bitmap,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val viewAspect = size.width / size.height.coerceAtLeast(1f)
        val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
        val rect = if (imageAspect >= viewAspect) {
            val h = size.width / imageAspect
            Rect(0f, (size.height - h) / 2f, size.width, (size.height + h) / 2f)
        } else {
            val w = size.height * imageAspect
            Rect((size.width - w) / 2f, 0f, (size.width + w) / 2f, size.height)
        }

        strokes.forEach { stroke ->
            if (stroke.points.isEmpty()) return@forEach
            val path = Path()
            stroke.points.forEachIndexed { index, point ->
                val x = rect.left + point.x * rect.width
                val y = rect.top + point.y * rect.height
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val base = Color(stroke.colorArgb)
            val color = if (stroke.tool == AnnotationTool.HIGHLIGHTER) base.copy(alpha = 0.28f) else base
            val strokeWidth = (stroke.widthFraction * rect.width).coerceAtLeast(2f)
            if (stroke.points.size == 1) {
                val point = stroke.points.first()
                drawCircle(
                    color = color,
                    radius = strokeWidth / 2f,
                    center = Offset(rect.left + point.x * rect.width, rect.top + point.y * rect.height),
                )
            } else {
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }

        shapes.forEach { item ->
            val p1 = Offset(rect.left + item.x1 * rect.width, rect.top + item.y1 * rect.height)
            val p2 = Offset(rect.left + item.x2 * rect.width, rect.top + item.y2 * rect.height)
            val left = min(p1.x, p2.x)
            val top = min(p1.y, p2.y)
            val right = max(p1.x, p2.x)
            val bottom = max(p1.y, p2.y)
            val width = (item.widthFraction * rect.width).coerceAtLeast(2f)
            val base = Color(item.colorArgb)

            when (item.type) {
                AnnotationShapeType.LINE -> drawLine(base, p1, p2, strokeWidth = width, cap = StrokeCap.Round)
                AnnotationShapeType.ARROW -> {
                    drawLine(base, p1, p2, strokeWidth = width, cap = StrokeCap.Round)
                    val angle = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                    val head = (rect.width * 0.018f).coerceAtLeast(12f)
                    val a1 = angle + Math.PI * 0.82
                    val a2 = angle - Math.PI * 0.82
                    val h1 = Offset((p2.x + cos(a1).toFloat() * head), (p2.y + sin(a1).toFloat() * head))
                    val h2 = Offset((p2.x + cos(a2).toFloat() * head), (p2.y + sin(a2).toFloat() * head))
                    drawLine(base, p2, h1, strokeWidth = width, cap = StrokeCap.Round)
                    drawLine(base, p2, h2, strokeWidth = width, cap = StrokeCap.Round)
                }
                AnnotationShapeType.RECTANGLE -> {
                    if (item.fillEnabled) {
                        drawRect(base.copy(alpha = 0.12f), topLeft = Offset(left, top), size = Size(right - left, bottom - top))
                    }
                    drawRect(base, topLeft = Offset(left, top), size = Size(right - left, bottom - top), style = Stroke(width))
                }
                AnnotationShapeType.ELLIPSE -> {
                    if (item.fillEnabled) {
                        drawOval(base.copy(alpha = 0.12f), topLeft = Offset(left, top), size = Size(right - left, bottom - top))
                    }
                    drawOval(base, topLeft = Offset(left, top), size = Size(right - left, bottom - top), style = Stroke(width))
                }
            }

            if (item.id == selectedShapeId) {
                val selection = Color(0xFF1976D2)
                drawRect(
                    selection.copy(alpha = 0.8f),
                    topLeft = Offset(left - 5f, top - 5f),
                    size = Size((right - left) + 10f, (bottom - top) + 10f),
                    style = Stroke(width = 2.5f),
                )
                drawCircle(selection, radius = 8f, center = p2)
            }
        }

        val native = drawContext.canvas.nativeCanvas
        texts.forEach { item ->
            val left = rect.left + item.x * rect.width
            val top = rect.top + item.y * rect.height
            val boxWidth = (item.widthFraction * rect.width).coerceAtLeast(40f)
            val boxHeight = (item.heightFraction * rect.height).coerceAtLeast(28f)
            val right = (left + boxWidth).coerceAtMost(rect.right)
            val bottom = (top + boxHeight).coerceAtMost(rect.bottom)
            val textSize = (item.sizeFraction * rect.width).coerceAtLeast(14f)
            val padding = (textSize * 0.28f).coerceAtLeast(4f)


            val style = when {
                item.bold && item.italic -> Typeface.BOLD_ITALIC
                item.bold -> Typeface.BOLD
                item.italic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.colorArgb
                this.textSize = textSize
                typeface = Typeface.create(Typeface.DEFAULT, style)
                isUnderlineText = item.underline
            }
            val alignment = when (item.alignment) {
                AnnotationTextAlign.LEFT -> Layout.Alignment.ALIGN_NORMAL
                AnnotationTextAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                AnnotationTextAlign.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
            }
            val usableWidth = (right - left - padding * 2f).roundToInt().coerceAtLeast(1)
            val layout = StaticLayout.Builder
                .obtain(item.text, 0, item.text.length, textPaint, usableWidth)
                .setAlignment(alignment)
                .setIncludePad(false)
                .setLineSpacing(0f, 1f)
                .build()

            native.save()
            native.clipRect(left, top, right, bottom)
            native.translate(left + padding, top + padding)
            layout.draw(native)
            native.restore()

            if (item.id == selectedTextId) {
                val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF1976D2.toInt()
                    this.style = Paint.Style.STROKE
                    strokeWidth = 3.2f
                    pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
                }
                native.drawRoundRect(left - 6f, top - 6f, right + 6f, bottom + 6f, 8f, 8f, selectionPaint)

                val handleRadius = 11f
                val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF1976D2.toInt()
                    this.style = Paint.Style.FILL
                }
                val handleOutline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFFFFFFFF.toInt()
                    this.style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                listOf(
                    left to top,
                    right to top,
                    left to bottom,
                    right to bottom,
                ).forEach { (hx, hy) ->
                    native.drawCircle(hx, hy, handleRadius, handlePaint)
                    native.drawCircle(hx, hy, handleRadius, handleOutline)
                }

            }
        }
    }
}

@Composable
private fun AnnotationToolbar(
    selectedTool: AnnotationUiTool,
    currentColor: Color,
    pencilWidth: Float,
    highlighterWidth: Float,
    textSizeFraction: Float,
    currentShapeType: AnnotationShapeType,
    shapeFillEnabled: Boolean,
    selectedText: AnnotationText?,
    selectedShape: AnnotationShape?,
    canUndo: Boolean,
    canRedo: Boolean,
    onTool: (AnnotationUiTool) -> Unit,
    onColor: () -> Unit,
    onCycleWidth: () -> Unit,
    onCycleShapeType: () -> Unit,
    onToggleShapeFill: () -> Unit,
    onEditSelectedText: () -> Unit,
    onTextSizeDelta: (Float) -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onCycleTextAlign: () -> Unit,
    onToggleTextBackground: () -> Unit,
    onToggleTextBorder: () -> Unit,
    onNudgeText: (Float, Float) -> Unit,
    onDeleteSelection: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
) {
    Surface(
        color = Color.White.copy(alpha = 0.97f),
        elevation = 8.dp,
        shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnnotationToolChip(stringResource(R.string.annotation_select), selectedTool == AnnotationUiTool.SELECT, onClick = { onTool(AnnotationUiTool.SELECT) })
                Spacer(Modifier.width(5.dp))
                AnnotationToolChip(stringResource(R.string.annotation_pencil), selectedTool == AnnotationUiTool.PENCIL, onClick = { onTool(AnnotationUiTool.PENCIL) })
                Spacer(Modifier.width(5.dp))
                AnnotationToolChip(stringResource(R.string.annotation_highlighter), selectedTool == AnnotationUiTool.HIGHLIGHTER, onClick = { onTool(AnnotationUiTool.HIGHLIGHTER) })
                Spacer(Modifier.width(5.dp))
                AnnotationToolChip(stringResource(R.string.annotation_text), selectedTool == AnnotationUiTool.TEXT, onClick = { onTool(AnnotationUiTool.TEXT) })
                Spacer(Modifier.width(5.dp))
                AnnotationToolChip(stringResource(R.string.annotation_shapes), selectedTool == AnnotationUiTool.SHAPE, onClick = { onTool(AnnotationUiTool.SHAPE) })
                Spacer(Modifier.width(5.dp))
                AnnotationToolChip(stringResource(R.string.annotation_eraser), selectedTool == AnnotationUiTool.ERASER, onClick = { onTool(AnnotationUiTool.ERASER) })
                Spacer(Modifier.width(12.dp))

                AnnotationToolChip(stringResource(R.string.annotation_undo), false, onUndo, enabled = canUndo)
                Spacer(Modifier.width(5.dp))
                AnnotationToolChip(stringResource(R.string.annotation_redo), false, onRedo, enabled = canRedo)
                Spacer(Modifier.width(5.dp))
                AnnotationToolChip(stringResource(R.string.clear), false, onClear)
                Spacer(Modifier.width(12.dp))

                Surface(
                    color = Burgundy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { onDone() },
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.annotation_done), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            val showContext = selectedTool != AnnotationUiTool.SELECT || selectedText != null || selectedShape != null
            if (showContext) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when {
                        selectedTool == AnnotationUiTool.SELECT && selectedText != null -> {
                            Text(stringResource(R.string.annotation_text_selected), color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.annotation_drag_help), color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.width(12.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_edit), false, onEditSelectedText)
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_decrease_font), false, onClick = { onTextSizeDelta(-0.003f) })
                            Spacer(Modifier.width(4.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_increase_font), false, onClick = { onTextSizeDelta(0.003f) })
                            Spacer(Modifier.width(6.dp))
                            AnnotationToolChip("B", selectedText.bold, onToggleBold)
                            Spacer(Modifier.width(4.dp))
                            AnnotationToolChip("I", selectedText.italic, onToggleItalic)
                            Spacer(Modifier.width(4.dp))
                            AnnotationToolChip("U", selectedText.underline, onToggleUnderline)
                            Spacer(Modifier.width(6.dp))
                            AnnotationToolChip(
                                when (selectedText.alignment) {
                                    AnnotationTextAlign.LEFT -> stringResource(R.string.annotation_left)
                                    AnnotationTextAlign.CENTER -> stringResource(R.string.annotation_center)
                                    AnnotationTextAlign.RIGHT -> stringResource(R.string.annotation_right)
                                },
                                false,
                                onCycleTextAlign,
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(color = currentColor, shape = CircleShape, modifier = Modifier.size(31.dp).clickable { onColor() }) {}
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_color), false, onColor)
                            Spacer(Modifier.width(8.dp))
                            AnnotationToolChip("←", false, onClick = { onNudgeText(-0.003f, 0f) })
                            Spacer(Modifier.width(3.dp))
                            AnnotationToolChip("↑", false, onClick = { onNudgeText(0f, -0.003f) })
                            Spacer(Modifier.width(3.dp))
                            AnnotationToolChip("↓", false, onClick = { onNudgeText(0f, 0.003f) })
                            Spacer(Modifier.width(3.dp))
                            AnnotationToolChip("→", false, onClick = { onNudgeText(0.003f, 0f) })
                            Spacer(Modifier.width(8.dp))
                            AnnotationToolChip(stringResource(R.string.delete_score), false, onDeleteSelection)
                        }
                        selectedTool == AnnotationUiTool.SELECT && selectedShape != null -> {
                            Text(stringResource(R.string.annotation_shape_selected), color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Spacer(Modifier.width(10.dp))
                            AnnotationToolChip(shapeLabel(currentShapeType), false, onCycleShapeType)
                            Spacer(Modifier.width(6.dp))
                            Surface(color = currentColor, shape = CircleShape, modifier = Modifier.size(31.dp).clickable { onColor() }) {}
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_color), false, onColor)
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_fill), shapeFillEnabled, onToggleShapeFill)
                            Spacer(Modifier.width(8.dp))
                            AnnotationToolChip(stringResource(R.string.delete_score), false, onDeleteSelection)
                        }
                        selectedTool == AnnotationUiTool.SHAPE -> {
                            AnnotationToolChip(shapeLabel(currentShapeType), false, onCycleShapeType)
                            Spacer(Modifier.width(6.dp))
                            Surface(color = currentColor, shape = CircleShape, modifier = Modifier.size(31.dp).clickable { onColor() }) {}
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_color), false, onColor)
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_width), false, onCycleWidth)
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_fill), shapeFillEnabled, onToggleShapeFill)
                        }
                        selectedTool == AnnotationUiTool.TEXT -> {
                            Surface(color = currentColor, shape = CircleShape, modifier = Modifier.size(31.dp).clickable { onColor() }) {}
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_color), false, onColor)
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_size, (textSizeFraction * 1000).roundToInt()), false, onCycleWidth)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.annotation_touch_create), color = Color.Gray, fontSize = 12.sp)
                        }
                        selectedTool == AnnotationUiTool.PENCIL || selectedTool == AnnotationUiTool.HIGHLIGHTER -> {
                            Surface(color = currentColor, shape = CircleShape, modifier = Modifier.size(31.dp).clickable { onColor() }) {}
                            Spacer(Modifier.width(5.dp))
                            AnnotationToolChip(stringResource(R.string.annotation_color), false, onColor)
                            Spacer(Modifier.width(5.dp))
                            val width = if (selectedTool == AnnotationUiTool.HIGHLIGHTER) highlighterWidth else pencilWidth
                            AnnotationToolChip(stringResource(R.string.annotation_width_value, (width * 1000).roundToInt()), false, onCycleWidth)
                        }
                        selectedTool == AnnotationUiTool.ERASER -> {
                            Text(stringResource(R.string.annotation_erase_help), color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun shapeLabel(type: AnnotationShapeType): String = when (type) {
    AnnotationShapeType.LINE -> stringResource(R.string.shape_line)
    AnnotationShapeType.ARROW -> stringResource(R.string.shape_arrow)
    AnnotationShapeType.RECTANGLE -> stringResource(R.string.shape_rectangle)
    AnnotationShapeType.ELLIPSE -> stringResource(R.string.shape_ellipse)
}

@Composable
private fun AnnotationToolChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(
        color = when {
            !enabled -> Color(0xFFF3F0EB)
            selected -> Color(0xFFFFEDB0)
            else -> Color(0xFFF7F5F1)
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.clickable(enabled = enabled) { onClick() },
    ) {
        Text(
            label,
            color = if (enabled) Ink else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun ColorPickerDialog(
    selected: Color,
    onDismiss: () -> Unit,
    onSelect: (Color) -> Unit,
) {
    val colors = listOf(
        Color(0xFFE53935),
        Color(0xFF1976D2),
        Color(0xFF43A047),
        Color(0xFFFFC107),
        Color(0xFF8E24AA),
        Color(0xFF111111),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.annotation_color_title)) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                colors.forEach { color ->
                    Surface(
                        color = color,
                        shape = CircleShape,
                        elevation = if (color == selected) 8.dp else 1.dp,
                        modifier = Modifier.size(if (color == selected) 48.dp else 42.dp).clickable { onSelect(color) },
                    ) {}
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text(stringResource(R.string.close), modifier = Modifier.clickable { onDismiss() }.padding(12.dp))
        },
    )
}

@Composable
private fun PdfRepairDialog(
    current: PdfPageAdjustment,
    pageIndex: Int,
    pageCount: Int,
    onDismiss: () -> Unit,
    onApplyCurrent: (PdfPageAdjustment) -> Unit,
    onApplyAll: (PdfPageAdjustment) -> Unit,
    onResetCurrent: () -> Unit,
) {
    var draft by remember(current, pageIndex) { mutableStateOf(current.normalized()) }

    @Composable
    fun CropControl(label: String, value: Float, onChange: (Float) -> Unit) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = Ink, fontSize = 13.sp)
                Text("${(value * 100f).roundToInt()}%", color = Burgundy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = value,
                onValueChange = onChange,
                valueRange = 0f..0.20f,
                steps = 19,
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.pdf_repair), color = Burgundy, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.pdf_repair_subtitle, pageIndex + 1, pageCount),
                    color = Color.DarkGray,
                    fontSize = 12.sp,
                )
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(stringResource(R.string.rotation), color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FestiveButton(
                        onClick = { draft = draft.copy(rotationQuarterTurns = draft.rotationQuarterTurns - 1).normalized() },
                        backgroundColor = ParchmentCard,
                        contentColor = Navy,
                    ) {
                        Icon(Icons.Default.RotateLeft, contentDescription = null, tint = Navy)
                        Spacer(Modifier.width(5.dp))
                        Text("−90°", color = Navy)
                    }
                    FestiveButton(
                        onClick = { draft = draft.copy(rotationQuarterTurns = draft.rotationQuarterTurns + 1).normalized() },
                        backgroundColor = ParchmentCard,
                        contentColor = Navy,
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null, tint = Navy)
                        Spacer(Modifier.width(5.dp))
                        Text("+90°", color = Navy)
                    }
                    Text(
                        "${draft.rotationQuarterTurns * 90}°",
                        color = Burgundy,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(15.dp))
                Text(stringResource(R.string.manual_crop), color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    stringResource(R.string.manual_crop_desc),
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(6.dp))
                CropControl(stringResource(R.string.left), draft.cropLeft) { draft = draft.copy(cropLeft = it).normalized() }
                CropControl(stringResource(R.string.right), draft.cropRight) { draft = draft.copy(cropRight = it).normalized() }
                CropControl(stringResource(R.string.top), draft.cropTop) { draft = draft.copy(cropTop = it).normalized() }
                CropControl(stringResource(R.string.bottom), draft.cropBottom) { draft = draft.copy(cropBottom = it).normalized() }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.contrast), color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("${(draft.contrast * 100f).roundToInt()}%", color = Burgundy, fontWeight = FontWeight.SemiBold)
                }
                Slider(
                    value = draft.contrast,
                    onValueChange = { draft = draft.copy(contrast = it).normalized() },
                    valueRange = 0.70f..1.60f,
                    steps = 17,
                )
                Text(
                    stringResource(R.string.contrast_desc),
                    color = Color.DarkGray,
                    fontSize = 11.sp,
                )

                Spacer(Modifier.height(14.dp))
                FestiveButton(
                    onClick = onResetCurrent,
                    backgroundColor = ParchmentCard,
                    contentColor = Burgundy,
                    cornerRadius = 12.dp,
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Burgundy)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.reset_page), color = Burgundy)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onApplyCurrent(draft.normalized()) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.apply_page))
                }
                Button(
                    onClick = { onApplyAll(draft.normalized()) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Navy, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(R.string.apply_pdf))
                }
            }
        },
        dismissButton = {
            Text(
                stringResource(R.string.cancel),
                color = Burgundy,
                modifier = Modifier.clickable(onClick = onDismiss).padding(12.dp),
            )
        },
    )
}

@Composable
private fun ViewerMenu(
    title: String,
    pageIndex: Int,
    pageCount: Int,
    scorePosition: Int,
    totalScores: Int,
    zoom: Float,
    isFavorite: Boolean,
    annotationsVisible: Boolean,
    hasAnnotations: Boolean,
    rotationDegrees: Int,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onOpenScoreList: () -> Unit,
    onResetZoom: () -> Unit,
    onOpenTools: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onOpenRepair: () -> Unit,
    onStartAnnotations: () -> Unit,
    onToggleAnnotationsVisible: () -> Unit,
) {
    val compact = LocalAdaptiveWindowSize.current == AdaptiveWindowSize.COMPACT

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Surface(color = Color.White.copy(alpha = 0.94f), elevation = 5.dp) {
            if (compact) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.viewer_return), tint = Ink)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) stringResource(R.string.remove_favorite) else stringResource(R.string.add_favorite),
                                tint = Burgundy,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onOpenScoreList) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.viewer_search_jump), tint = Ink)
                        }
                        IconButton(onClick = onOpenTools) {
                            Icon(Icons.Default.Build, contentDescription = stringResource(R.string.viewer_tools), tint = Color(0xFF76507C))
                        }
                        IconButton(onClick = onRotateLeft) {
                            Icon(Icons.Default.RotateLeft, contentDescription = stringResource(R.string.rotate_left), tint = Navy)
                        }
                        IconButton(onClick = onRotateRight) {
                            Icon(Icons.Default.RotateRight, contentDescription = stringResource(R.string.rotate_right), tint = Navy)
                        }
                        IconButton(onClick = onOpenRepair) {
                            Icon(Icons.Default.Crop, contentDescription = stringResource(R.string.repair_pdf_view), tint = Burgundy)
                        }
                        IconButton(onClick = onStartAnnotations) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.annotate), tint = Navy)
                        }
                        if (hasAnnotations) {
                            IconButton(onClick = onToggleAnnotationsVisible) {
                                Icon(
                                    if (annotationsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (annotationsVisible) stringResource(R.string.hide_annotations) else stringResource(R.string.show_annotations),
                                    tint = Ink,
                                )
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("${(zoom * 100).roundToInt()}%", color = Ink, fontSize = 14.sp)
                        IconButton(onClick = onResetZoom) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_zoom), tint = Burgundy)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.viewer_return), tint = Ink)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onOpenScoreList) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.viewer_search_jump), tint = Ink)
                    }
                    IconButton(onClick = onOpenTools) {
                        Icon(Icons.Default.Build, contentDescription = stringResource(R.string.viewer_tools), tint = Color(0xFF76507C))
                    }
                    IconButton(onClick = onRotateLeft) {
                        Icon(Icons.Default.RotateLeft, contentDescription = stringResource(R.string.rotate_left), tint = Navy)
                    }
                    IconButton(onClick = onRotateRight) {
                        Icon(Icons.Default.RotateRight, contentDescription = stringResource(R.string.rotate_right), tint = Navy)
                    }
                    IconButton(onClick = onOpenRepair) {
                        Icon(Icons.Default.Crop, contentDescription = stringResource(R.string.repair_pdf_view), tint = Burgundy)
                    }
                    IconButton(onClick = onStartAnnotations) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.annotate), tint = Navy)
                    }
                    if (hasAnnotations) {
                        IconButton(onClick = onToggleAnnotationsVisible) {
                            Icon(
                                if (annotationsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (annotationsVisible) stringResource(R.string.hide_annotations) else stringResource(R.string.show_annotations),
                                tint = Ink,
                            )
                        }
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) stringResource(R.string.remove_favorite) else stringResource(R.string.add_favorite),
                            tint = Burgundy,
                        )
                    }
                    Text("${(zoom * 100).roundToInt()}%", color = Ink, fontSize = 15.sp)
                    IconButton(onClick = onResetZoom) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset_zoom), tint = Burgundy)
                    }
                }
            }
        }

        Surface(color = Color.White.copy(alpha = 0.94f), elevation = 5.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = if (compact) 12.dp else 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.page_count, pageIndex + 1, pageCount), color = Ink, fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(14.dp))
                Text("•", color = Color.Gray)
                Spacer(Modifier.width(14.dp))
                Text(stringResource(R.string.score_position, scorePosition, totalScores), color = Color.DarkGray, fontSize = if (compact) 13.sp else 15.sp)
                if (rotationDegrees != 0) {
                    Spacer(Modifier.width(14.dp))
                    Text("•", color = Color.Gray)
                    Spacer(Modifier.width(14.dp))
                    Text(stringResource(R.string.rotation_value, rotationDegrees), color = Navy, fontSize = 13.sp)
                }
            }
        }
    }

}

@Composable
private fun ScorePickerDialog(
    scores: List<LibraryPdf>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val indexedScores = scores.mapIndexed { index, score -> index to score }
    val filtered = remember(scores, search) {
        smartSearch(search, indexedScores) { (_, score) ->
            listOf(score.displayName, score.fileName, score.relativePath)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sequence_search)) },
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
                    placeholder = { Text(stringResource(R.string.score_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = festiveTextFieldColors(),
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(filtered, key = { _, pair -> pair.second.uri.toString() }) { _, pair ->
                        val (originalIndex, score) = pair
                        val isCurrent = originalIndex == currentIndex
                        Surface(
                            color = if (isCurrent) Burgundy.copy(alpha = 0.10f) else Color.Transparent,
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(originalIndex) },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${originalIndex + 1}", color = Burgundy, fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        score.displayName,
                                        color = Ink,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (score.relativePath.isNotBlank()) {
                                        Text(score.relativePath, color = Color.DarkGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                if (isCurrent) Text(stringResource(R.string.current), color = Burgundy, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun ViewerError(message: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.viewer_display_error), color = Burgundy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(message, color = Color.DarkGray, fontSize = 16.sp)
        Spacer(Modifier.height(18.dp))
        IconButton(onClick = onClose) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.viewer_return), tint = Burgundy) }
    }
}

@Composable
private fun ViewerEnvironment(activity: Activity?, settings: AppSettings) {
    DisposableEffect(activity, settings.keepScreenOn, settings.overrideBrightness, settings.brightnessPercent) {
        if (activity == null) {
            onDispose { }
        } else {
            // IMPORTANT: l'orientació NO es modifica ací. El visor viu en una
            // PdfViewerActivity independent que fixa la seua política una sola
            // vegada en onCreate. Això evita els bucles de rotació observats en
            // algunes tablets (especialment OnePlus/Nokia).
            val previousBrightness = activity.window.attributes.screenBrightness
            if (settings.keepScreenOn) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            if (settings.overrideBrightness) {
                val attributes = activity.window.attributes
                attributes.screenBrightness = (settings.brightnessPercent.coerceIn(20, 100) / 100f)
                activity.window.attributes = attributes
            }

            val decorView = activity.window.decorView
            val previousSystemUi = decorView.systemUiVisibility

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                run {
                    decorView.systemUiVisibility =
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                }
            }

            onDispose {
                if (settings.keepScreenOn) {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                val restoreAttributes = activity.window.attributes
                restoreAttributes.screenBrightness = previousBrightness
                activity.window.attributes = restoreAttributes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    activity.window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                } else {
                    @Suppress("DEPRECATION")
                    run { decorView.systemUiVisibility = previousSystemUi }
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
