package com.partituresfesteres.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.partituresfesteres.app.R
import com.partituresfesteres.app.data.PitchReading
import com.partituresfesteres.app.data.ReferenceToneEngine
import com.partituresfesteres.app.data.TunerEngine
import com.partituresfesteres.app.ui.theme.AgedGold
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.HeritageGreen
import com.partituresfesteres.app.ui.theme.Ink
import com.partituresfesteres.app.ui.theme.MutedGold
import com.partituresfesteres.app.ui.theme.MutedInk
import com.partituresfesteres.app.ui.theme.Navy
import com.partituresfesteres.app.ui.theme.ParchmentCard
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.pow

private enum class ToolTab { TUNER, METRONOME, REFERENCE }

private data class PitchHistoryPoint(
    val elapsedMs: Long,
    val cents: Float,
    val noteName: String,
)

@Composable
fun ToolsScreen(
    tunerReferenceHz: Int,
    onTunerReferenceChange: (Int) -> Unit,
    onLibraryClick: () -> Unit,
    onRepertoiresClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onToolsClick: () -> Unit,
    onAddContentClick: () -> Unit,
) {
    var tab by remember { mutableStateOf(ToolTab.TUNER) }
    Row(Modifier.fillMaxSize()) {
        Sidebar(
            activeSection = AppSection.TOOLS,
            onLibraryClick = onLibraryClick,
            onRepertoiresClick = onRepertoiresClick,
            onRecentsClick = onRecentsClick,
            onFavoritesClick = onFavoritesClick,
            onToolsClick = onToolsClick,
            onAddContentClick = onAddContentClick,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 22.dp, end = 28.dp, top = 14.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CimoPlaceholder()
                Spacer(Modifier.width(12.dp))
                Column {
                    AppTitle()
                    Text(stringResource(R.string.tools_title), fontSize = 24.sp, color = Burgundy, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolTabButton(stringResource(R.string.tuner), tab == ToolTab.TUNER) { tab = ToolTab.TUNER }
                ToolTabButton(stringResource(R.string.metronome), tab == ToolTab.METRONOME) { tab = ToolTab.METRONOME }
                ToolTabButton(stringResource(R.string.reference_note), tab == ToolTab.REFERENCE) { tab = ToolTab.REFERENCE }
            }
            Spacer(Modifier.height(10.dp))
            when (tab) {
                ToolTab.TUNER -> TunerPanel(tunerReferenceHz, onTunerReferenceChange)
                ToolTab.METRONOME -> MetronomePanel()
                ToolTab.REFERENCE -> ReferenceTonePanel(tunerReferenceHz)
            }
        }
    }
}

@Composable
fun ViewerToolsDialog(
    tunerReferenceHz: Int,
    onTunerReferenceChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var tab by remember { mutableStateOf(ToolTab.TUNER) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.91f),
            color = Color(0xFFF4E8D2),
            shape = RoundedCornerShape(24.dp),
            elevation = 12.dp,
        ) {
            Column(Modifier.fillMaxSize().padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.tools_title),
                        color = Burgundy,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    ToolTabButton(stringResource(R.string.tuner), tab == ToolTab.TUNER) { tab = ToolTab.TUNER }
                    ToolTabButton(stringResource(R.string.metronome), tab == ToolTab.METRONOME) { tab = ToolTab.METRONOME }
                    ToolTabButton(stringResource(R.string.note), tab == ToolTab.REFERENCE) { tab = ToolTab.REFERENCE }
                    TextButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Burgundy)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.close), color = Burgundy)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxSize()) {
                    when (tab) {
                        ToolTab.TUNER -> TunerPanel(tunerReferenceHz, onTunerReferenceChange)
                        ToolTab.METRONOME -> MetronomePanel()
                        ToolTab.REFERENCE -> ReferenceTonePanel(tunerReferenceHz)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolTabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) Burgundy else Color.White.copy(alpha = 0.75f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.clickable(onClick = onClick),
        elevation = if (selected) 3.dp else 1.dp,
    ) {
        Text(
            label,
            color = if (selected) Color.White else Navy,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun TunerPanel(
    savedReferenceHz: Int,
    onReferenceChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val engine = remember { TunerEngine() }
    val history = remember { mutableStateListOf<PitchHistoryPoint>() }

    var hasPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var running by remember { mutableStateOf(false) }
    var reading by remember { mutableStateOf<PitchReading?>(null) }
    var referenceA by remember(savedReferenceHz) { mutableStateOf(savedReferenceHz.toDouble()) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        running = granted
    }

    LaunchedEffect(running, hasPermission, referenceA) {
        engine.stop()
        reading = null
        if (running && hasPermission) {
            engine.start(scope, referenceA) { detected ->
                mainHandler.post {
                    reading = detected
                    if (detected != null) {
                        val now = SystemClock.elapsedRealtime()
                        history.add(
                            PitchHistoryPoint(
                                elapsedMs = now,
                                cents = detected.cents.toFloat().coerceIn(-50f, 50f),
                                noteName = detected.noteName,
                            )
                        )
                        // Aproximadamente 25-30 s de histórico a la cadencia del detector.
                        while (history.size > 280) history.removeAt(0)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ParchmentCard.copy(alpha = 0.95f),
        elevation = 5.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Column(
                modifier = Modifier.weight(1.16f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    reading?.noteName ?: "—",
                    fontSize = 58.sp,
                    color = Navy,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    reading?.let { "%.2f Hz".format(it.frequencyHz) } ?: stringResource(R.string.listening),
                    fontSize = 16.sp,
                    color = MutedInk,
                )

                TunerGauge(reading, Modifier.fillMaxWidth().weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf("−50", "−25", "0", "+25", "+50").forEach {
                        Text(it, color = if (it == "0") HeritageGreen else MutedInk, fontSize = 12.sp)
                    }
                }
                reading?.let {
                    val prefix = if (it.cents > 0) "+" else ""
                    Text(
                        "$prefix${it.cents.roundToInt()} cents · ${if (it.stable) stringResource(R.string.tuner_status_in_tune) else if (it.cents < 0) stringResource(R.string.tuner_status_low) else stringResource(R.string.tuner_status_high)}",
                        color = if (it.stable) HeritageGreen else Burgundy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                } ?: Text(stringResource(R.string.in_tune_zone), color = HeritageGreen, fontSize = 13.sp)

                Spacer(Modifier.height(8.dp))
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
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) stringResource(R.string.stop) else stringResource(R.string.start))
                }
                if (!hasPermission) {
                    Spacer(Modifier.height(5.dp))
                    Text(stringResource(R.string.tuner_permission), color = MutedInk, fontSize = 12.sp)
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.tuning_history), color = Burgundy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text(stringResource(R.string.tuning_deviation), color = MutedInk, fontSize = 11.sp)
                    }
                    TextButton(onClick = { history.clear() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Burgundy, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.clear), color = Burgundy, fontSize = 12.sp)
                    }
                }
                PitchHistoryChart(history, Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(10.dp))

                Text(
                    stringResource(R.string.tuner_reference, referenceA.roundToInt()),
                    color = Burgundy,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
                Slider(
                    value = referenceA.toFloat(),
                    onValueChange = { referenceA = it.roundToInt().toDouble() },
                    onValueChangeFinished = { onReferenceChange(referenceA.roundToInt()) },
                    valueRange = 430f..450f,
                    steps = 19,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("430", color = MutedInk, fontSize = 11.sp)
                    TextButton(onClick = {
                        referenceA = 442.0
                        onReferenceChange(442)
                    }) {
                        Text(stringResource(R.string.reset_442), color = Navy, fontSize = 12.sp)
                    }
                    Text("450", color = MutedInk, fontSize = 11.sp)
                }
                Text(
                    stringResource(R.string.tuner_privacy),
                    color = MutedInk,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun TunerGauge(reading: PitchReading?, modifier: Modifier = Modifier) {
    Canvas(modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        val center = Offset(size.width / 2f, size.height * 0.91f)
        val radius = minOf(size.width * 0.43f, size.height * 0.86f)
        val startAngle = 205f
        val sweepAngle = 130f

        // Escala completa y zonas de tolerancia.
        drawArc(
            color = AgedGold.copy(alpha = 0.42f),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 9f),
        )
        // ±10 cents: zona de aproximación en dorado suave.
        drawArc(
            color = MutedGold.copy(alpha = 0.45f),
            startAngle = 270f - 13f,
            sweepAngle = 26f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 18f),
        )
        // ±5 cents: banda afinada.
        drawArc(
            color = HeritageGreen.copy(alpha = 0.82f),
            startAngle = 270f - 6.5f,
            sweepAngle = 13f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = 20f),
        )

        for (cents in -50..50 step 5) {
            val angle = 270f + (cents / 50f) * 65f
            val radians = angle * (PI / 180.0)
            val isMajor = cents % 10 == 0
            val outer = radius
            val inner = radius - if (isMajor) 24f else 12f
            val sx = center.x + cos(radians).toFloat() * inner
            val sy = center.y + sin(radians).toFloat() * inner
            val ex = center.x + cos(radians).toFloat() * outer
            val ey = center.y + sin(radians).toFloat() * outer
            drawLine(
                color = if (cents == 0) HeritageGreen else AgedGold,
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = if (cents == 0) 5f else if (isMajor) 3f else 1.5f,
            )
        }

        val cents = (reading?.cents ?: 0.0).coerceIn(-50.0, 50.0)
        val needleAngle = 270f + (cents / 50.0).toFloat() * 65f
        val radians = needleAngle * (PI / 180.0)
        val tipRadius = radius * 0.83f
        val tip = Offset(
            center.x + cos(radians).toFloat() * tipRadius,
            center.y + sin(radians).toFloat() * tipRadius,
        )
        drawLine(
            color = if (reading?.stable == true) HeritageGreen else Burgundy,
            start = center,
            end = tip,
            strokeWidth = 8f,
        )
        drawCircle(color = MutedGold, radius = 13f, center = center)
        drawCircle(color = Burgundy, radius = 6f, center = center)
    }
}

@Composable
private fun PitchHistoryChart(history: List<PitchHistoryPoint>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.58f), RoundedCornerShape(16.dp))
            .border(1.dp, AgedGold.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
    ) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val left = 8f
            val right = size.width - 8f
            val top = 8f
            val bottom = size.height - 8f
            val height = bottom - top
            val width = right - left
            val centerY = (top + bottom) / 2f

            fun yFor(cents: Float): Float = centerY - (cents.coerceIn(-50f, 50f) / 50f) * (height * 0.46f)

            val greenTop = yFor(5f)
            val greenBottom = yFor(-5f)
            drawRect(
                color = HeritageGreen.copy(alpha = 0.16f),
                topLeft = Offset(left, greenTop),
                size = Size(width, greenBottom - greenTop),
            )
            val amberTop = yFor(10f)
            val amberBottom = yFor(-10f)
            drawRect(
                color = MutedGold.copy(alpha = 0.08f),
                topLeft = Offset(left, amberTop),
                size = Size(width, greenTop - amberTop),
            )
            drawRect(
                color = MutedGold.copy(alpha = 0.08f),
                topLeft = Offset(left, greenBottom),
                size = Size(width, amberBottom - greenBottom),
            )
            drawLine(HeritageGreen.copy(alpha = 0.7f), Offset(left, centerY), Offset(right, centerY), 2f)
            for (c in listOf(-50, -25, 25, 50)) {
                val y = yFor(c.toFloat())
                drawLine(AgedGold.copy(alpha = 0.25f), Offset(left, y), Offset(right, y), 1f)
            }

            if (history.size >= 2) {
                val visible = history.takeLast(220)
                val path = Path()
                var started = false
                visible.forEachIndexed { index, point ->
                    val x = left + (index.toFloat() / (visible.size - 1).coerceAtLeast(1)) * width
                    val y = yFor(point.cents)
                    if (!started) {
                        path.moveTo(x, y)
                        started = true
                    } else {
                        val previous = visible[index - 1]
                        // Al cambiar de nota se inicia un tramo nuevo para no dibujar un salto
                        // artificial de +50 a -50 cents.
                        if (previous.noteName != point.noteName) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                }
                drawPath(path, color = Burgundy, style = Stroke(width = 3.5f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("+50", color = MutedInk, fontSize = 10.sp)
            Text(stringResource(R.string.in_tune_short), color = HeritageGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text("−50", color = MutedInk, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MetronomePanel() {
    val context = LocalContext.current
    var bpm by remember { mutableIntStateOf(100) }
    var beatsPerBar by remember { mutableIntStateOf(4) }
    var running by remember { mutableStateOf(false) }
    var beat by remember { mutableIntStateOf(0) }

    val soundPool = remember {
        SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
    }
    val normalClickId = remember { soundPool.load(context, R.raw.metronome_click, 1) }
    val accentClickId = remember { soundPool.load(context, R.raw.metronome_accent, 1) }

    DisposableEffect(Unit) {
        onDispose { soundPool.release() }
    }

    LaunchedEffect(running, bpm, beatsPerBar) {
        beat = 0
        var nextBeatMs = SystemClock.elapsedRealtime().toDouble()
        while (running) {
            val accent = beat % beatsPerBar == 0
            soundPool.play(if (accent) accentClickId else normalClickId, 1f, 1f, 1, 0, 1f)
            beat = (beat + 1) % beatsPerBar
            nextBeatMs += 60_000.0 / bpm.coerceIn(40, 240)
            val waitMs = (nextBeatMs - SystemClock.elapsedRealtime()).coerceAtLeast(1.0).toLong()
            delay(waitMs)
        }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ParchmentCard.copy(alpha = 0.95f),
        elevation = 5.dp,
    ) {
        Row(Modifier.fillMaxSize().padding(30.dp), horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .background(Navy.copy(alpha = 0.08f), CircleShape)
                        .border(2.dp, AgedGold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Burgundy, modifier = Modifier.size(44.dp))
                        Text("$bpm", fontSize = 62.sp, color = Navy, fontWeight = FontWeight.Bold)
                        Text("BPM", color = MutedInk)
                        if (running) {
                            Spacer(Modifier.height(7.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                repeat(beatsPerBar) { index ->
                                    Box(
                                        Modifier
                                            .size(if (index == beat) 12.dp else 9.dp)
                                            .background(
                                                if (index == beat) Burgundy else AgedGold.copy(alpha = 0.35f),
                                                CircleShape,
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = { running = !running },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (running) Burgundy else HeritageGreen,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(if (running) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) stringResource(R.string.stop) else stringResource(R.string.metronome_start))
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.tempo), color = Burgundy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Slider(value = bpm.toFloat(), onValueChange = { bpm = it.roundToInt() }, valueRange = 40f..240f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(
                        onClick = { bpm = (bpm - 1).coerceAtLeast(40) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White.copy(alpha = 0.8f), contentColor = Navy),
                    ) { Text("−") }
                    Button(
                        onClick = { bpm = (bpm + 1).coerceAtMost(240) },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White.copy(alpha = 0.8f), contentColor = Navy),
                    ) { Text("+") }
                }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.time_signature), color = Burgundy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 4, 6).forEach { beats ->
                        Surface(
                            color = if (beatsPerBar == beats) MutedGold else Color.White.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { beatsPerBar = beats },
                        ) {
                            Text(
                                "$beats/4",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                color = if (beatsPerBar == beats) Color.White else Navy,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.metronome_sound_desc),
                    color = MutedInk,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ReferenceTonePanel(referenceAHz: Int) {
    val engine = remember { ReferenceToneEngine() }
    val noteNames = remember {
        listOf("Do", "Do♯/Re♭", "Re", "Mi♭", "Mi", "Fa", "Fa♯/Sol♭", "Sol", "La♭", "La", "Sib", "Si")
    }
    var pitchClass by remember { mutableIntStateOf(9) } // La
    var octave by remember { mutableIntStateOf(4) }
    var running by remember { mutableStateOf(false) }

    val midi = (octave + 1) * 12 + pitchClass
    val frequency = referenceAHz.toDouble() * 2.0.pow((midi - 69) / 12.0)

    LaunchedEffect(running, frequency) {
        engine.stop()
        if (running) engine.start(frequency)
    }
    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = ParchmentCard.copy(alpha = 0.95f),
        elevation = 5.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            Column(
                modifier = Modifier.weight(0.9f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Burgundy, modifier = Modifier.size(50.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    "${noteNames[pitchClass]}$octave",
                    fontSize = 54.sp,
                    color = Navy,
                    fontWeight = FontWeight.Bold,
                )
                Text("%.2f Hz".format(frequency), color = MutedInk, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { running = !running },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (running) Burgundy else HeritageGreen,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(if (running) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (running) stringResource(R.string.stop_note) else stringResource(R.string.play))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.tuning_reference_a4, referenceAHz),
                    color = MutedInk,
                    fontSize = 12.sp,
                )
                Text(
                    stringResource(R.string.reference_tone_desc),
                    color = MutedInk,
                    fontSize = 11.sp,
                )
            }

            Column(
                modifier = Modifier.weight(1.45f).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.note), color = Burgundy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                noteNames.chunked(6).forEach { rowNotes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        rowNotes.forEach { name ->
                            val index = noteNames.indexOf(name)
                            Surface(
                                color = if (pitchClass == index) Burgundy else Color.White.copy(alpha = 0.80f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).clickable { pitchClass = index },
                                elevation = if (pitchClass == index) 3.dp else 1.dp,
                            ) {
                                Text(
                                    name,
                                    color = if (pitchClass == index) Color.White else Navy,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 10.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                }

                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.octave), color = Burgundy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (2..6).forEach { value ->
                        Surface(
                            color = if (octave == value) MutedGold else Color.White.copy(alpha = 0.78f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { octave = value },
                        ) {
                            Text(
                                value.toString(),
                                color = if (octave == value) Color.White else Navy,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 17.dp, vertical = 10.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.quick_access), color = Burgundy, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { pitchClass = 9; octave = 4 },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White.copy(alpha = 0.82f), contentColor = Navy),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("La4") }
                    Button(
                        onClick = { pitchClass = 10; octave = 3 },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White.copy(alpha = 0.82f), contentColor = Navy),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Sib3") }
                    Button(
                        onClick = { pitchClass = 10; octave = 4 },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White.copy(alpha = 0.82f), contentColor = Navy),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Sib4") }
                }
            }
        }
    }
}

