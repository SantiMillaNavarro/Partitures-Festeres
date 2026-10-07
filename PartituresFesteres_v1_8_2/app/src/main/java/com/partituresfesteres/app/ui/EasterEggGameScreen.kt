package com.partituresfesteres.app.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partituresfesteres.app.R
import com.partituresfesteres.app.ui.theme.AgedGold
import com.partituresfesteres.app.ui.theme.Burgundy
import com.partituresfesteres.app.ui.theme.Ink
import com.partituresfesteres.app.ui.theme.ParchmentCard
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.isActive

private enum class EasterEggGamePhase { READY, RUNNING, GAME_OVER }

private enum class ObstacleDrawing { GLYPH, EIGHTH_PAIR, FOUR_SIXTEENTHS }

private enum class MusicObstacleType(
    val symbol: String,
    val textScale: Float,
    val hitboxWidth: Float,
    val hitboxHeight: Float,
    val unlockAfterSeconds: Float,
    val drawing: ObstacleDrawing = ObstacleDrawing.GLYPH,
) {
    WHOLE("𝅝", 0.126f, 0.026f, 0.038f, 0f),
    HALF("𝅗𝅥", 0.132f, 0.027f, 0.050f, 0f),
    QUARTER("♩", 0.120f, 0.024f, 0.052f, 0f),
    EIGHTH("♪", 0.128f, 0.026f, 0.055f, 0f),
    BEAMED_EIGHTHS("", 0.136f, 0.046f, 0.058f, 3f, ObstacleDrawing.EIGHTH_PAIR),
    SIXTEENTH("𝅘𝅥𝅯", 0.130f, 0.027f, 0.058f, 4f),
    BEAMED_SIXTEENTHS("♬", 0.142f, 0.046f, 0.061f, 5f),
    FOUR_SIXTEENTHS("", 0.142f, 0.072f, 0.061f, 8f, ObstacleDrawing.FOUR_SIXTEENTHS),
    WHOLE_REST("𝄻", 0.112f, 0.027f, 0.030f, 9f),
    HALF_REST("𝄼", 0.112f, 0.027f, 0.030f, 9f),
    QUARTER_REST("𝄽", 0.120f, 0.027f, 0.050f, 10f),
    EIGHTH_REST("𝄾", 0.120f, 0.027f, 0.050f, 11f),
    SIXTEENTH_REST("𝄿", 0.124f, 0.028f, 0.052f, 12f),
    TIED_NOTES("♩‿♩", 0.104f, 0.060f, 0.047f, 14f),
    SHARP("♯", 0.116f, 0.024f, 0.045f, 16f),
    FLAT("♭", 0.120f, 0.022f, 0.048f, 16f),
    NATURAL("♮", 0.116f, 0.022f, 0.047f, 17f),
    TREBLE_CLEF("𝄞", 0.142f, 0.034f, 0.062f, 20f),
    FERMATA("𝄐", 0.118f, 0.032f, 0.038f, 22f),
}

private data class MusicObstacle(
    val id: Long,
    val x: Float,
    val type: MusicObstacleType,
    val airborne: Boolean,
    val baselineY: Float,
)

private fun randomSpawnDelay(elapsed: Float): Float {
    val difficulty = (elapsed / 55f).coerceIn(0f, 1f)
    val minDelay = 1.18f - 0.22f * difficulty
    val maxDelay = 1.62f - 0.28f * difficulty
    return minDelay + Random.nextFloat() * (maxDelay - minDelay)
}

private fun createRandomObstacle(id: Long, elapsed: Float): MusicObstacle {
    val available = MusicObstacleType.entries.filter { elapsed >= it.unlockAfterSeconds }
    val type = available.random()

    // Els obstacles aeris apareixen només després d'haver jugat una estona.
    // Es mantenen prou alts perquè quedar-se a terra siga sempre una opció justa.
    val airborne = elapsed >= 18f && Random.nextFloat() < (0.18f + (elapsed / 100f).coerceAtMost(0.14f))
    val baseline = if (airborne) {
        if (Random.nextBoolean()) 0.625f else 0.665f
    } else {
        0.79f
    }

    return MusicObstacle(
        id = id,
        x = 1.08f,
        type = type,
        airborne = airborne,
        baselineY = baseline,
    )
}

@Composable
internal fun EasterEggGameScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("partitures_easter_egg", 0) }

    var phase by remember { mutableStateOf(EasterEggGamePhase.READY) }
    var jumpOffset by remember { mutableFloatStateOf(0f) }
    var jumpVelocity by remember { mutableFloatStateOf(0f) }
    var obstacles by remember { mutableStateOf(emptyList<MusicObstacle>()) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var spawnClock by remember { mutableFloatStateOf(0f) }
    var nextSpawnDelay by remember { mutableFloatStateOf(randomSpawnDelay(0f)) }
    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(prefs.getInt("best_score", 0)) }
    var nextObstacleId by remember { mutableLongStateOf(1L) }

    fun resetGame() {
        jumpOffset = 0f
        jumpVelocity = 0f
        obstacles = emptyList()
        elapsed = 0f
        spawnClock = 0f
        nextSpawnDelay = randomSpawnDelay(0f)
        score = 0
        phase = EasterEggGamePhase.READY
    }

    fun startGame() {
        if (phase == EasterEggGamePhase.READY) {
            jumpOffset = 0f
            jumpVelocity = 0f
            obstacles = emptyList()
            elapsed = 0f
            spawnClock = 0f
            nextSpawnDelay = randomSpawnDelay(0f)
            score = 0
            phase = EasterEggGamePhase.RUNNING
        }
    }

    fun jump() {
        if (phase == EasterEggGamePhase.RUNNING && jumpOffset <= 0.003f) {
            jumpVelocity = 1.03f
        }
    }

    BackHandler(onBack = onExit)

    LaunchedEffect(phase) {
        if (phase != EasterEggGamePhase.RUNNING) return@LaunchedEffect
        var lastFrame = 0L
        while (isActive && phase == EasterEggGamePhase.RUNNING) {
            withFrameNanos { frameTime ->
                if (lastFrame == 0L) {
                    lastFrame = frameTime
                    return@withFrameNanos
                }
                val dt = ((frameTime - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.035f)
                lastFrame = frameTime

                elapsed += dt
                score = (elapsed * 10f).toInt()

                val gravity = 2.75f
                jumpVelocity -= gravity * dt
                jumpOffset += jumpVelocity * dt
                if (jumpOffset < 0f) {
                    jumpOffset = 0f
                    jumpVelocity = 0f
                }

                val speed = (0.30f + elapsed * 0.0034f).coerceAtMost(0.57f)
                obstacles = obstacles
                    .map { it.copy(x = it.x - speed * dt) }
                    .filter { it.x > -0.18f }

                spawnClock += dt
                if (spawnClock >= nextSpawnDelay) {
                    spawnClock = 0f
                    obstacles = obstacles + createRandomObstacle(nextObstacleId++, elapsed)
                    nextSpawnDelay = randomSpawnDelay(elapsed)
                }

                // Hitboxes deliberadament més xicotetes que els dibuixos visibles.
                // El jugador ha de perdre per un contacte clar, no per la caixa del glif.
                val groundY = 0.79f
                val saxLeft = 0.174f
                val saxRight = 0.204f
                val saxBottom = groundY - jumpOffset - 0.018f
                val saxTop = saxBottom - 0.070f

                val collided = obstacles.any { obstacle ->
                    val width = obstacle.type.hitboxWidth
                    val height = obstacle.type.hitboxHeight
                    val left = obstacle.x + 0.010f
                    val right = left + width
                    val bottom = if (obstacle.airborne) {
                        obstacle.baselineY - 0.006f
                    } else {
                        groundY - 0.010f
                    }
                    val top = bottom - height
                    saxRight > left && saxLeft < right && saxBottom > top && saxTop < bottom
                }

                if (collided) {
                    phase = EasterEggGamePhase.GAME_OVER
                    if (score > bestScore) {
                        bestScore = score
                        prefs.edit().putInt("best_score", score).apply()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(phase) {
                detectTapGestures(
                    onTap = {
                        when (phase) {
                            EasterEggGamePhase.READY -> startGame()
                            EasterEggGamePhase.RUNNING -> jump()
                            EasterEggGamePhase.GAME_OVER -> Unit
                        }
                    }
                )
            }
    ) {
        Image(
            painter = painterResource(R.drawable.bg_parchment),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.30f)))

        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val groundY = h * 0.79f
            val unit = min(w, h)

            drawLine(
                color = Ink.copy(alpha = 0.58f),
                start = Offset(w * 0.055f, groundY),
                end = Offset(w * 0.945f, groundY),
                strokeWidth = (unit * 0.0032f).coerceAtLeast(2f),
            )
            drawLine(
                color = AgedGold.copy(alpha = 0.42f),
                start = Offset(w * 0.055f, groundY + unit * 0.013f),
                end = Offset(w * 0.945f, groundY + unit * 0.013f),
                strokeWidth = (unit * 0.0017f).coerceAtLeast(1f),
            )

            obstacles.forEach { obstacle ->
                when (obstacle.type.drawing) {
                    ObstacleDrawing.GLYPH -> {
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = android.graphics.Color.rgb(46, 57, 69)
                            textAlign = Paint.Align.LEFT
                            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                            textSize = h * obstacle.type.textScale
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            obstacle.type.symbol,
                            w * obstacle.x,
                            h * obstacle.baselineY,
                            paint,
                        )
                    }
                    ObstacleDrawing.EIGHTH_PAIR -> drawBeamedNotes(
                        left = w * obstacle.x,
                        baseline = h * obstacle.baselineY,
                        height = h * 0.092f,
                        count = 2,
                        beams = 1,
                    )
                    ObstacleDrawing.FOUR_SIXTEENTHS -> drawBeamedNotes(
                        left = w * obstacle.x,
                        baseline = h * obstacle.baselineY,
                        height = h * 0.094f,
                        count = 4,
                        beams = 2,
                    )
                }
            }

            drawCuteSaxophone(
                left = w * 0.145f,
                bottom = groundY - h * jumpOffset,
                width = w * 0.092f,
                height = h * 0.148f,
                running = phase == EasterEggGamePhase.RUNNING && jumpOffset <= 0.003f,
                elapsed = elapsed,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 24.dp),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = stringResource(R.string.easter_score, score),
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.easter_best, bestScore),
                color = Burgundy,
                fontSize = 14.sp,
            )
        }

        Button(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = ParchmentCard.copy(alpha = 0.92f),
                contentColor = Ink,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.easter_exit), fontSize = 14.sp)
        }

        if (phase == EasterEggGamePhase.READY) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.easter_tap_to_start),
                    color = Burgundy,
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(7.dp))
                Text(
                    text = stringResource(R.string.easter_jump_hint),
                    color = Ink.copy(alpha = 0.78f),
                    fontSize = 15.sp,
                )
            }
        }

        if (phase == EasterEggGamePhase.GAME_OVER) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(ParchmentCard.copy(alpha = 0.96f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 30.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.easter_game_over),
                    color = Burgundy,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.easter_score, score), color = Ink, fontSize = 17.sp)
                Text(stringResource(R.string.easter_best, bestScore), color = Ink, fontSize = 15.sp)
                Spacer(Modifier.size(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { resetGame() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Burgundy, contentColor = Color.White),
                    ) {
                        Text(stringResource(R.string.easter_play_again))
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = onExit,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(backgroundColor = AgedGold, contentColor = Ink),
                    ) {
                        Text(stringResource(R.string.easter_exit))
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBeamedNotes(
    left: Float,
    baseline: Float,
    height: Float,
    count: Int,
    beams: Int,
) {
    val ink = Color(0xFF2E3945)
    val headW = height * 0.23f
    val headH = height * 0.15f
    val spacing = if (count == 2) height * 0.40f else height * 0.28f
    val stemTop = baseline - height * 0.82f
    val stemWidth = (height * 0.035f).coerceAtLeast(2f)

    repeat(count) { i ->
        val x = left + i * spacing
        drawOval(
            color = ink,
            topLeft = Offset(x, baseline - headH),
            size = androidx.compose.ui.geometry.Size(headW, headH),
        )
        drawLine(
            color = ink,
            start = Offset(x + headW * 0.88f, baseline - headH * 0.55f),
            end = Offset(x + headW * 0.88f, stemTop),
            strokeWidth = stemWidth,
        )
    }

    val firstStemX = left + headW * 0.88f
    val lastStemX = left + (count - 1) * spacing + headW * 0.88f
    repeat(beams) { beam ->
        val y = stemTop + beam * height * 0.10f
        drawLine(
            color = ink,
            start = Offset(firstStemX, y),
            end = Offset(lastStemX, y),
            strokeWidth = (height * 0.075f).coerceAtLeast(3f),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCuteSaxophone(
    left: Float,
    bottom: Float,
    width: Float,
    height: Float,
    running: Boolean,
    elapsed: Float,
) {
    val brass = Color(0xFFD6A23A)
    val brassLight = Color(0xFFF3D779)
    val brassDark = Color(0xFF8A5B18)
    val outline = Color(0xFF49311D)
    val cheek = Color(0xFFE78A73)
    val legSwing = if (running) kotlin.math.sin(elapsed * 17f) * height * 0.018f else 0f

    // Silueta principal inspirada en un saxo alt: tudell corbat, cos vertical,
    // colze inferior i campana oberta. Primer es dibuixa un contorn fosc i
    // damunt el tub de llautó perquè la forma siga recognoscible a mida xicoteta.
    val tube = Path().apply {
        moveTo(left + width * 0.18f, bottom - height * 0.91f)
        cubicTo(
            left + width * 0.28f, bottom - height * 1.00f,
            left + width * 0.48f, bottom - height * 0.98f,
            left + width * 0.48f, bottom - height * 0.84f,
        )
        cubicTo(
            left + width * 0.50f, bottom - height * 0.66f,
            left + width * 0.48f, bottom - height * 0.43f,
            left + width * 0.47f, bottom - height * 0.27f,
        )
        cubicTo(
            left + width * 0.46f, bottom - height * 0.10f,
            left + width * 0.55f, bottom - height * 0.05f,
            left + width * 0.67f, bottom - height * 0.10f,
        )
        cubicTo(
            left + width * 0.77f, bottom - height * 0.14f,
            left + width * 0.78f, bottom - height * 0.27f,
            left + width * 0.74f, bottom - height * 0.36f,
        )
    }
    drawPath(
        tube,
        outline,
        style = Stroke(width = (width * 0.19f).coerceAtLeast(4f)),
    )
    drawPath(
        tube,
        brass,
        style = Stroke(width = (width * 0.12f).coerceAtLeast(2.7f)),
    )

    // Boquilla negra y tudell: la boquilla sobresale hacia la izquierda.
    drawLine(
        color = outline,
        start = Offset(left + width * 0.18f, bottom - height * 0.91f),
        end = Offset(left + width * 0.035f, bottom - height * 0.92f),
        strokeWidth = (width * 0.070f).coerceAtLeast(2f),
    )
    drawLine(
        color = brassLight,
        start = Offset(left + width * 0.23f, bottom - height * 0.91f),
        end = Offset(left + width * 0.33f, bottom - height * 0.94f),
        strokeWidth = (width * 0.025f).coerceAtLeast(1f),
    )

    // Campana acampanada a la derecha, con boca oscura para dar volumen.
    val bell = Path().apply {
        moveTo(left + width * 0.69f, bottom - height * 0.43f)
        cubicTo(
            left + width * 0.80f, bottom - height * 0.46f,
            left + width * 0.92f, bottom - height * 0.42f,
            left + width * 0.98f, bottom - height * 0.33f,
        )
        cubicTo(
            left + width * 0.91f, bottom - height * 0.24f,
            left + width * 0.80f, bottom - height * 0.22f,
            left + width * 0.70f, bottom - height * 0.26f,
        )
        close()
    }
    drawPath(bell, brass, style = Fill)
    drawPath(bell, outline, style = Stroke(width = (width * 0.045f).coerceAtLeast(1.4f)))
    drawOval(
        color = outline,
        topLeft = Offset(left + width * 0.86f, bottom - height * 0.39f),
        size = androidx.compose.ui.geometry.Size(width * 0.12f, height * 0.11f),
    )
    drawOval(
        color = brassDark,
        topLeft = Offset(left + width * 0.875f, bottom - height * 0.375f),
        size = androidx.compose.ui.geometry.Size(width * 0.085f, height * 0.078f),
    )

    // Protector de llaves y pequeñas llaves: detalles mínimos pero propios del saxo.
    drawLine(
        color = brassDark,
        start = Offset(left + width * 0.58f, bottom - height * 0.72f),
        end = Offset(left + width * 0.64f, bottom - height * 0.35f),
        strokeWidth = (width * 0.020f).coerceAtLeast(1f),
    )
    repeat(4) { i ->
        val y = bottom - height * (0.68f - i * 0.105f)
        drawCircle(
            color = brassLight,
            radius = width * 0.032f,
            center = Offset(left + width * (0.56f + (i % 2) * 0.025f), y),
        )
        drawCircle(
            color = outline.copy(alpha = 0.65f),
            radius = width * 0.010f,
            center = Offset(left + width * (0.56f + (i % 2) * 0.025f), y),
        )
    }

    // Cara: queda sobre el cuerpo, sin borrar la lectura de instrumento.
    val faceX = left + width * 0.43f
    val eyeY = bottom - height * 0.69f
    drawCircle(outline, radius = width * 0.032f, center = Offset(faceX - width * 0.055f, eyeY))
    drawCircle(outline, radius = width * 0.032f, center = Offset(faceX + width * 0.055f, eyeY))
    drawCircle(Color.White, radius = width * 0.010f, center = Offset(faceX - width * 0.063f, eyeY - width * 0.010f))
    drawCircle(Color.White, radius = width * 0.010f, center = Offset(faceX + width * 0.047f, eyeY - width * 0.010f))
    drawCircle(cheek.copy(alpha = 0.78f), radius = width * 0.035f, center = Offset(faceX - width * 0.12f, bottom - height * 0.61f))
    drawCircle(cheek.copy(alpha = 0.78f), radius = width * 0.035f, center = Offset(faceX + width * 0.12f, bottom - height * 0.61f))
    drawArc(
        color = outline,
        startAngle = 8f,
        sweepAngle = 164f,
        useCenter = false,
        topLeft = Offset(faceX - width * 0.060f, bottom - height * 0.635f),
        size = androidx.compose.ui.geometry.Size(width * 0.12f, height * 0.075f),
        style = Stroke(width = (width * 0.020f).coerceAtLeast(1f)),
    )

    // Dos patitas salen del colze inferior. Son pequeñas para no confundir la silueta.
    val hipY = bottom - height * 0.075f
    val footY = bottom + height * 0.012f
    drawLine(
        outline,
        Offset(left + width * 0.50f, hipY),
        Offset(left + width * 0.42f + legSwing, footY),
        strokeWidth = (width * 0.036f).coerceAtLeast(1.4f),
    )
    drawLine(
        outline,
        Offset(left + width * 0.63f, hipY),
        Offset(left + width * 0.70f - legSwing, footY),
        strokeWidth = (width * 0.036f).coerceAtLeast(1.4f),
    )
    drawLine(
        outline,
        Offset(left + width * 0.36f + legSwing, footY),
        Offset(left + width * 0.45f + legSwing, footY),
        strokeWidth = (width * 0.047f).coerceAtLeast(1.5f),
    )
    drawLine(
        outline,
        Offset(left + width * 0.67f - legSwing, footY),
        Offset(left + width * 0.76f - legSwing, footY),
        strokeWidth = (width * 0.047f).coerceAtLeast(1.5f),
    )
}
