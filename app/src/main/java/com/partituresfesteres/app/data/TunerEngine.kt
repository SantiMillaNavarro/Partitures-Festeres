package com.partituresfesteres.app.data

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Una lectura del afinador cromático. */
data class PitchReading(
    val frequencyHz: Double,
    val midi: Double,
    val noteName: String,
    val cents: Double,
    val stable: Boolean,
    val confidence: Double,
)

/**
 * Afinador cromático offline.
 *
 * v1.4 usa una variante del algoritmo YIN (CMNDF + interpolación parabólica),
 * más robusta frente a armónicos/octavas que la autocorrelación simple usada antes.
 */
class TunerEngine {
    private var job: Job? = null
    private var recorder: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun start(
        scope: CoroutineScope,
        referenceA: Double,
        onReading: (PitchReading?) -> Unit,
    ) {
        stop()

        val sampleRate = 44_100
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(8_192)

        val audioRecord = createRecorder(sampleRate, minBuffer)
        recorder = audioRecord

        job = scope.launch(Dispatchers.Default) {
            val buffer = ShortArray(4_096)
            val recent = ArrayDeque<Double>(5)

            runCatching { audioRecord.startRecording() }
                .onFailure {
                    onReading(null)
                    return@launch
                }

            try {
                while (isActive) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 1_024) {
                        val detection = detectPitchYin(buffer, read, sampleRate)
                        val reading = detection?.let { (rawFrequency, confidence) ->
                            // Mediana corta: reduce vibración visual y falsos picos sin hacer
                            // que un cambio real de nota tarde demasiado en aparecer.
                            recent.addLast(rawFrequency)
                            while (recent.size > 3) recent.removeFirst()
                            val smoothed = recent.sorted()[recent.size / 2]
                            toReading(smoothed, referenceA, confidence)
                        }
                        if (detection == null) recent.clear()
                        onReading(reading)
                    }
                }
            } finally {
                runCatching { audioRecord.stop() }
                audioRecord.release()
                if (recorder === audioRecord) recorder = null
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        recorder?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        recorder = null
    }

    @SuppressLint("MissingPermission")
    private fun createRecorder(sampleRate: Int, bufferSize: Int): AudioRecord {
        // VOICE_RECOGNITION tiende a evitar parte del procesado de voz/AGC que
        // distorsiona una señal musical sostenida. Si el dispositivo no lo admite,
        // AudioRecord seguirá usando una configuración compatible del sistema.
        val preferred = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        }.getOrNull()

        if (preferred != null && preferred.state == AudioRecord.STATE_INITIALIZED) return preferred
        runCatching { preferred?.release() }

        return AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
    }

    private fun toReading(frequency: Double, referenceA: Double, confidence: Double): PitchReading {
        val midi = 69.0 + 12.0 * (ln(frequency / referenceA) / ln(2.0))
        val nearest = midi.roundToInt()
        val cents = (midi - nearest) * 100.0
        val names = arrayOf(
            "Do", "Do♯", "Re", "Mi♭", "Mi", "Fa",
            "Fa♯", "Sol", "La♭", "La", "Si♭", "Si",
        )
        val noteName = names[((nearest % 12) + 12) % 12]
        return PitchReading(
            frequencyHz = frequency,
            midi = midi,
            noteName = noteName,
            cents = cents,
            stable = abs(cents) <= IN_TUNE_CENTS,
            confidence = confidence,
        )
    }

    /** Devuelve frecuencia + confianza (0..1). */
    private fun detectPitchYin(samples: ShortArray, length: Int, sampleRate: Int): Pair<Double, Double>? {
        val n = minOf(length, 4_096)
        if (n < 2_048) return null

        var mean = 0.0
        for (i in 0 until n) mean += samples[i]
        mean /= n

        val data = DoubleArray(n)
        var energy = 0.0
        for (i in 0 until n) {
            val value = (samples[i].toDouble() - mean) / 32_768.0
            data[i] = value
            energy += value * value
        }
        val rms = sqrt(energy / n)
        if (rms < 0.008) return null

        val minFreq = 55.0
        val maxFreq = 1_800.0
        val minTau = (sampleRate / maxFreq).toInt().coerceAtLeast(2)
        val maxTau = minOf(n / 2, (sampleRate / minFreq).toInt())
        if (maxTau <= minTau + 2) return null

        val difference = DoubleArray(maxTau + 1)
        for (tau in 1..maxTau) {
            var sum = 0.0
            val limit = n - tau
            var i = 0
            while (i < limit) {
                val delta = data[i] - data[i + tau]
                sum += delta * delta
                i++
            }
            difference[tau] = sum
        }

        val cmndf = DoubleArray(maxTau + 1)
        cmndf[0] = 1.0
        var runningSum = 0.0
        for (tau in 1..maxTau) {
            runningSum += difference[tau]
            cmndf[tau] = if (runningSum <= 1e-12) 1.0 else difference[tau] * tau / runningSum
        }

        val threshold = 0.15
        var tau = minTau
        var candidate = -1
        while (tau <= maxTau) {
            if (cmndf[tau] < threshold) {
                while (tau + 1 <= maxTau && cmndf[tau + 1] < cmndf[tau]) tau++
                candidate = tau
                break
            }
            tau++
        }

        // Si no cruza el umbral, permite un mínimo muy bueno; evita lecturas
        // aleatorias en ruido mediante un límite conservador.
        if (candidate < 0) {
            var bestTau = minTau
            var best = cmndf[minTau]
            for (t in (minTau + 1)..maxTau) {
                if (cmndf[t] < best) {
                    best = cmndf[t]
                    bestTau = t
                }
            }
            if (best > 0.28) return null
            candidate = bestTau
        }

        var refinedTau = candidate.toDouble()
        if (candidate > 1 && candidate < maxTau) {
            val left = cmndf[candidate - 1]
            val center = cmndf[candidate]
            val right = cmndf[candidate + 1]
            val denominator = 2.0 * (2.0 * center - right - left)
            if (abs(denominator) > 1e-12) {
                refinedTau += (right - left) / denominator
            }
        }

        val frequency = sampleRate / refinedTau
        if (frequency !in minFreq..maxFreq) return null
        val confidence = (1.0 - cmndf[candidate]).coerceIn(0.0, 1.0)
        if (confidence < 0.72) return null
        return frequency to confidence
    }

    companion object {
        /** Banda verde del afinador: ±5 cents. */
        const val IN_TUNE_CENTS = 5.0
    }
}
