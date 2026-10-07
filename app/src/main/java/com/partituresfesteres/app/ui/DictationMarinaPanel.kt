package com.partituresfesteres.app.ui

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.partituresfesteres.app.R
import com.partituresfesteres.app.data.PitchReading
import com.partituresfesteres.app.data.TunerEngine
import com.partituresfesteres.app.ui.theme.AgedGold
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.HeritageGreen
import com.partituresfesteres.app.ui.theme.MutedGold
import com.partituresfesteres.app.ui.theme.MutedInk
import com.partituresfesteres.app.ui.theme.Navy
import com.partituresfesteres.app.ui.theme.ParchmentCard
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private data class MarinaNote(
    val midi: Int,
    val label: String,
)

private data class StaffPlacement(
    val diatonicIndexFromC4: Int,
    val accidental: String?,
)

@Composable
fun DictationMarinaPanel(referenceHz: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val engine = remember { TunerEngine() }
    val notes = remember { mutableStateListOf<MarinaNote>() }
    val scroll = rememberScrollState()

    var hasPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var running by remember { mutableStateOf(false) }
    var marinaMode by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<PitchReading?>(null) }

    var candidateMidi by remember { mutableStateOf<Int?>(null) }
    var candidateSince by remember { mutableStateOf(0L) }
    var lastAcceptedMidi by remember { mutableStateOf<Int?>(null) }
    var lastAcceptedAt by remember { mutableStateOf(0L) }
    var lastSignalAt by remember { mutableStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) running = true
    }

    LaunchedEffect(running, hasPermission, referenceHz, marinaMode) {
        engine.stop()
        reading = null
        candidateMidi = null
        candidateSince = 0L
        lastAcceptedMidi = null
        lastAcceptedAt = 0L
        lastSignalAt = 0L
        if (running && hasPermission) {
            engine.start(scope, referenceHz.toDouble()) { detected ->
                mainHandler.post {
                    val now = SystemClock.elapsedRealtime()
                    reading = detected
                    if (detected == null) {
                        val silenceReset = if (marinaMode) 180L else 320L
                        if (now - lastSignalAt > silenceReset) {
                            candidateMidi = null
                            lastAcceptedMidi = null
                        }
                        return@post
                    }
                    lastSignalAt = now
                    val rawMidi = detected.midi.roundToInt().coerceIn(36, 96)
                    val midi = if (marinaMode) normalizeMidiForTrebleDisplay(rawMidi) else rawMidi
                    if (candidateMidi != midi) {
                        candidateMidi = midi
                        candidateSince = now
                    } else {
                        val stableMs = if (marinaMode) 70L else 150L
                        if (now - candidateSince >= stableMs) {
                            if (marinaMode) {
                                val repeatGap = 220L
                                if (lastAcceptedMidi != midi || now - lastAcceptedAt >= repeatGap) {
                                    notes.add(MarinaNote(midi, midiLabel(midi)))
                                    lastAcceptedMidi = midi
                                    lastAcceptedAt = now
                                    candidateSince = now
                                }
                            } else if (lastAcceptedMidi != midi) {
                                notes.add(MarinaNote(midi, midiLabel(midi)))
                                lastAcceptedMidi = midi
                                lastAcceptedAt = now
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    FestivePanel(
        modifier = Modifier.fillMaxSize(),
        cornerRadius = 22.dp,
        backgroundColor = ParchmentCard,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // El contenido puede crecer y desplazarse, pero nunca se desplaza por sí solo.
            // El usuario mantiene el control total del scroll.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier) {
                        Text(
                            text = if (marinaMode) stringResource(R.string.dictation_marina) else stringResource(R.string.dictation_musical),
                            color = Burgundy,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (running) {
                                if (marinaMode) stringResource(R.string.dictation_listening_marina)
                                else stringResource(R.string.dictation_listening)
                            } else {
                                stringResource(R.string.dictation_notes_count, notes.size)
                            },
                            color = MutedInk,
                            fontSize = 12.sp,
                        )
                    }
                    Spacer(Modifier.size(6.dp))
                    reading?.let {
                        Text(
                            it.noteName,
                            color = Navy,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = {
                            marinaMode = !marinaMode
                            running = false
                            reading = null
                            notes.clear()
                        },
                    ) {
                        Text(if (marinaMode) stringResource(R.string.dictation_musical) else stringResource(R.string.dictation_marina))
                    }
                    if (marinaMode) {
                        Text(
                            text = stringResource(R.string.dictation_marina_hint),
                            color = Burgundy,
                            fontSize = 11.sp,
                        )
                    }
                }

                if (marinaMode) {
                    Spacer(Modifier.height(6.dp))
                    MarinaMascotCard()
                }

                if (!hasPermission) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.dictation_permission), color = MutedInk, fontSize = 11.sp)
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (marinaMode) stringResource(R.string.dictation_pitch_only_marina) else stringResource(R.string.dictation_pitch_only),
                    modifier = Modifier.fillMaxWidth(),
                    color = MutedInk,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.height(12.dp))

                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(if (marinaMode) 210.dp else 190.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (marinaMode) stringResource(R.string.dictation_empty_marina) else stringResource(R.string.dictation_empty),
                            color = MutedInk,
                            fontSize = 14.sp,
                        )
                    }
                } else {
                    StaffScore(notes = notes.toList(), modifier = Modifier.fillMaxWidth())
                }

                if (!running && notes.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Button(
                            onClick = {
                                val bitmap = renderScoreBitmap(notes.toList(), marinaMode)
                                val ok = saveBitmapToGallery(context, bitmap, marinaMode)
                                Toast.makeText(
                                    context,
                                    if (ok) context.getString(R.string.dictation_saved) else context.getString(R.string.dictation_save_error),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Navy, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(stringResource(R.string.dictation_save_image))
                        }
                        Spacer(Modifier.size(10.dp))
                        Button(
                            onClick = {
                                runCatching {
                                    shareBitmap(context, renderScoreBitmap(notes.toList(), marinaMode), marinaMode)
                                }.onFailure {
                                    Toast.makeText(context, context.getString(R.string.dictation_share_error), Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.size(7.dp))
                            Text(stringResource(R.string.dictation_share))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Barra fija: permanece visible aunque el pentagrama crezca o el usuario haga scroll.
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = ParchmentCard.copy(alpha = 0.97f),
                elevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            else running = !running
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (running) Burgundy else HeritageGreen,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(if (running) Icons.Default.Stop else Icons.Default.Mic, contentDescription = null)
                        Spacer(Modifier.size(7.dp))
                        Text(if (running) stringResource(R.string.dictation_stop) else stringResource(R.string.dictation_start))
                    }

                    Spacer(Modifier.size(10.dp))
                    TextButton(
                        onClick = { if (!running) notes.clear() },
                        enabled = !running && notes.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.dictation_clear))
                    }
                }
            }
        }
    }
}

@Composable
private fun MarinaMascotCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.22f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.marina_mascot),
                contentDescription = null,
                modifier = Modifier.size(104.dp),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dictation_marina_bubble),
                    color = Navy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.dictation_marina_pitch_fun),
                    color = MutedInk,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

private fun normalizeMidiForTrebleDisplay(midi: Int): Int {
    var adapted = midi
    while (adapted < 60) adapted += 12
    while (adapted > 83) adapted -= 12
    return adapted.coerceIn(60, 83)
}

@Composable
private fun StaffScore(notes: List<MarinaNote>, modifier: Modifier = Modifier) {
    val compact = LocalAdaptiveWindowSize.current == AdaptiveWindowSize.COMPACT
    val notesPerStaff = if (compact) 6 else 10
    val rows = ((notes.size + notesPerStaff - 1) / notesPerStaff).coerceAtLeast(1)
    val rowHeight = 150.dp

    Canvas(modifier.height(rowHeight * rows)) {
        val staffGap = 18.dp.toPx()
        val halfGap = staffGap / 2f
        val marginLeft = 78.dp.toPx()
        val marginRight = 18.dp.toPx()
        val rowHeightPx = rowHeight.toPx()
        val usable = size.width - marginLeft - marginRight
        val slot = usable / notesPerStaff
        val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(28, 58, 87)
            textSize = 56.dp.toPx()
            textAlign = Paint.Align.CENTER
        }
        val accidentalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(28, 58, 87)
            textSize = 24.dp.toPx()
            textAlign = Paint.Align.CENTER
        }

        repeat(rows) { row ->
            val rowTop = row * rowHeightPx
            val bottomLineY = rowTop + 101.dp.toPx()
            repeat(5) { line ->
                val y = bottomLineY - line * staffGap
                drawLine(
                    color = Navy,
                    start = Offset(marginLeft, y),
                    end = Offset(size.width - marginRight, y),
                    strokeWidth = 1.6.dp.toPx(),
                )
            }
            drawContext.canvas.nativeCanvas.drawText(
                "𝄞",
                38.dp.toPx(),
                bottomLineY + 22.dp.toPx(),
                symbolPaint,
            )

            val start = row * notesPerStaff
            val end = minOf(notes.size, start + notesPerStaff)
            for (i in start until end) {
                val local = i - start
                val x = marginLeft + slot * (local + 0.5f)
                val placement = staffPlacement(notes[i].midi)
                val y = bottomLineY - (placement.diatonicIndexFromC4 - 2) * halfGap

                if (placement.diatonicIndexFromC4 <= 0) {
                    var idx = 0
                    while (idx >= placement.diatonicIndexFromC4) {
                        val ly = bottomLineY - (idx - 2) * halfGap
                        drawLine(Navy, Offset(x - 14.dp.toPx(), ly), Offset(x + 14.dp.toPx(), ly), 1.4.dp.toPx())
                        idx -= 2
                    }
                }
                if (placement.diatonicIndexFromC4 >= 12) {
                    var idx = 12
                    while (idx <= placement.diatonicIndexFromC4) {
                        val ly = bottomLineY - (idx - 2) * halfGap
                        drawLine(Navy, Offset(x - 14.dp.toPx(), ly), Offset(x + 14.dp.toPx(), ly), 1.4.dp.toPx())
                        idx += 2
                    }
                }

                drawOval(
                    color = Navy,
                    topLeft = Offset(x - 8.dp.toPx(), y - 5.5.dp.toPx()),
                    size = Size(16.dp.toPx(), 11.dp.toPx()),
                )
                val stemUp = placement.diatonicIndexFromC4 < 6
                if (stemUp) {
                    drawLine(Navy, Offset(x + 7.dp.toPx(), y), Offset(x + 7.dp.toPx(), y - 34.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                } else {
                    drawLine(Navy, Offset(x - 7.dp.toPx(), y), Offset(x - 7.dp.toPx(), y + 34.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                }
                placement.accidental?.let { accidental ->
                    drawContext.canvas.nativeCanvas.drawText(accidental, x - 17.dp.toPx(), y + 8.dp.toPx(), accidentalPaint)
                }
            }
        }
    }
}

private fun midiLabel(midi: Int): String {
    val names = arrayOf("Do", "Do♯", "Re", "Mi♭", "Mi", "Fa", "Fa♯", "Sol", "La♭", "La", "Si♭", "Si")
    val pc = ((midi % 12) + 12) % 12
    val octave = midi / 12 - 1
    return "${names[pc]}$octave"
}

private fun staffPlacement(midi: Int): StaffPlacement {
    val pc = ((midi % 12) + 12) % 12
    val octave = midi / 12 - 1
    val (letterIndex, accidental) = when (pc) {
        0 -> 0 to null
        1 -> 0 to "♯"
        2 -> 1 to null
        3 -> 2 to "♭"
        4 -> 2 to null
        5 -> 3 to null
        6 -> 3 to "♯"
        7 -> 4 to null
        8 -> 5 to "♭"
        9 -> 5 to null
        10 -> 6 to "♭"
        else -> 6 to null
    }
    return StaffPlacement((octave - 4) * 7 + letterIndex, accidental)
}

private fun renderScoreBitmap(notes: List<MarinaNote>, marinaMode: Boolean): Bitmap {
    val width = 1600
    val notesPerStaff = 10
    val rows = ((notes.size + notesPerStaff - 1) / notesPerStaff).coerceAtLeast(1)
    val rowHeight = 240
    val height = 130 + rows * rowHeight + 70
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.rgb(250, 242, 226))

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(123, 40, 48)
        textSize = 54f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(28, 58, 87)
        strokeWidth = 3f
    }
    val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(28, 58, 87)
        style = Paint.Style.FILL
    }
    val clefPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(28, 58, 87)
        textSize = 92f
        textAlign = Paint.Align.CENTER
    }
    val accidentalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(28, 58, 87)
        textSize = 38f
        textAlign = Paint.Align.CENTER
    }

    canvas.drawText(if (marinaMode) "Dictat Marina" else "Dictat Musical", 70f, 78f, titlePaint)
    val left = 170f
    val right = 1530f
    val staffGap = 28f
    val halfGap = 14f
    val slot = (right - left) / notesPerStaff

    repeat(rows) { row ->
        val rowTop = 120f + row * rowHeight
        val bottom = rowTop + 150f
        repeat(5) { line ->
            val y = bottom - line * staffGap
            canvas.drawLine(left, y, right, y, linePaint)
        }
        canvas.drawText("𝄞", 92f, bottom + 34f, clefPaint)

        val start = row * notesPerStaff
        val end = minOf(notes.size, start + notesPerStaff)
        for (i in start until end) {
            val local = i - start
            val x = left + slot * (local + 0.5f)
            val placement = staffPlacement(notes[i].midi)
            val y = bottom - (placement.diatonicIndexFromC4 - 2) * halfGap

            if (placement.diatonicIndexFromC4 <= 0) {
                var idx = 0
                while (idx >= placement.diatonicIndexFromC4) {
                    val ly = bottom - (idx - 2) * halfGap
                    canvas.drawLine(x - 22f, ly, x + 22f, ly, linePaint)
                    idx -= 2
                }
            }
            if (placement.diatonicIndexFromC4 >= 12) {
                var idx = 12
                while (idx <= placement.diatonicIndexFromC4) {
                    val ly = bottom - (idx - 2) * halfGap
                    canvas.drawLine(x - 22f, ly, x + 22f, ly, linePaint)
                    idx += 2
                }
            }

            canvas.save()
            canvas.rotate(-16f, x, y)
            canvas.drawOval(x - 13f, y - 9f, x + 13f, y + 9f, notePaint)
            canvas.restore()
            val stemUp = placement.diatonicIndexFromC4 < 6
            if (stemUp) canvas.drawLine(x + 12f, y, x + 12f, y - 55f, linePaint)
            else canvas.drawLine(x - 12f, y, x - 12f, y + 55f, linePaint)
            placement.accidental?.let { canvas.drawText(it, x - 31f, y + 12f, accidentalPaint) }
        }
    }
    return bitmap
}

private fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap, marinaMode: Boolean): Boolean {
    return runCatching {
        val prefix = if (marinaMode) "Dictat_Marina" else "Dictat_Musical"
        val name = "${prefix}_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Partitures Festeres")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                ?: return false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Partitures Festeres").apply { mkdirs() }
            FileOutputStream(File(dir, name)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        true
    }.getOrDefault(false)
}

private fun shareBitmap(context: android.content.Context, bitmap: Bitmap, marinaMode: Boolean) {
    val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val file = File(dir, if (marinaMode) "dictat_marina.png" else "dictat_musical.png")
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.dictation_share)))
}
