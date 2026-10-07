package com.partituresfesteres.app.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Musical description of a bar. BPM always refers to the main pulse.
 * In compound meters (6/8, 9/8, 12/8) each dotted-quarter pulse is
 * subdivided into three audible eighth-note ticks.
 */
data class MetronomeSignature(
    val label: String,
    val mainBeats: Int,
    val subdivisionsPerBeat: Int = 1,
) {
    val eventsPerBar: Int get() = mainBeats * subdivisionsPerBeat

    // Visual/UI helpers only. They do not introduce extra metronome sounds.
    val isCompound: Boolean get() = subdivisionsPerBeat > 1

    fun isGroupStart(eventIndex: Int): Boolean {
        val subdivisions = subdivisionsPerBeat.coerceAtLeast(1)
        return eventIndex % subdivisions == 0
    }
}

/**
 * Sample-accurate metronome based on a looping AudioTrack buffer.
 *
 * There are deliberately only two sounds: a stronger downbeat and one
 * identical weak click for every other event. This keeps compound meters
 * readable instead of introducing extra group/subdivision timbres.
 */
class MetronomeEngine {
    companion object {
        private const val SAMPLE_RATE = 44_100
    }

    private var audioTrack: AudioTrack? = null
    private var startNanos: Long = 0L
    private var eventIntervalNanos: Long = 0L
    private var eventCount: Int = 1

    fun start(bpm: Int, signature: MetronomeSignature) {
        stop()

        val safeBpm = bpm.coerceIn(40, 240)
        val subdivisions = signature.subdivisionsPerBeat.coerceAtLeast(1)
        val secondsPerMainBeat = 60.0 / safeBpm.toDouble()
        val secondsPerEvent = secondsPerMainBeat / subdivisions.toDouble()
        val framesPerEvent = (SAMPLE_RATE * secondsPerEvent).roundToInt().coerceAtLeast(1)
        val events = signature.eventsPerBar.coerceAtLeast(1)
        val totalFrames = framesPerEvent * events
        val pcm = ShortArray(totalFrames)

        for (event in 0 until events) {
            val click = mechanicalClick(strong = event == 0)
            val offset = event * framesPerEvent
            val max = minOf(click.size, pcm.size - offset)
            for (i in 0 until max) {
                val mixed = pcm[offset + i].toInt() + click[i].toInt()
                pcm[offset + i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(pcm, 0, pcm.size)
        track.setLoopPoints(0, totalFrames, -1)
        eventIntervalNanos = (secondsPerEvent * 1_000_000_000.0).toLong().coerceAtLeast(1L)
        eventCount = events
        startNanos = SystemClock.elapsedRealtimeNanos()
        audioTrack = track
        track.play()
    }

    fun stop() {
        val track = audioTrack
        audioTrack = null
        if (track != null) {
            runCatching { track.pause() }
            runCatching { track.flush() }
            runCatching { track.release() }
        }
        startNanos = 0L
        eventIntervalNanos = 0L
        eventCount = 1
    }

    fun currentEventIndex(): Int {
        if (startNanos == 0L || eventIntervalNanos <= 0L) return 0
        val elapsed = (SystemClock.elapsedRealtimeNanos() - startNanos).coerceAtLeast(0L)
        return ((elapsed / eventIntervalNanos) % eventCount).toInt()
    }

    /**
     * Two deliberately stable timbres only: strong first beat and weak rest.
     * No alternating tick/tock and no special sound for compound subdivisions.
     */
    private fun mechanicalClick(strong: Boolean): ShortArray {
        val durationSeconds = if (strong) 0.058 else 0.043
        val count = (SAMPLE_RATE * durationSeconds).roundToInt()
        val out = ShortArray(count)
        val amplitude = if (strong) 0.88 else 0.53
        val bodyHz = if (strong) 1_520.0 else 1_820.0
        val upperHz = bodyHz * 1.80

        var noiseState = if (strong) 0x13579BDF else 0x2468ACE
        for (i in 0 until count) {
            val t = i.toDouble() / SAMPLE_RATE.toDouble()
            val envelope = exp(-t * if (strong) 82.0 else 94.0)
            val attack = (i / 14.0).coerceAtMost(1.0)

            noiseState = noiseState * 1664525 + 1013904223
            val noise = (((noiseState ushr 9) and 0x7FFFFF) / 4194303.5) - 1.0

            val body = sin(2.0 * PI * bodyHz * t)
            val upper = 0.38 * sin(2.0 * PI * upperHz * t + 0.35)
            val transient = if (i < 68) noise * (1.0 - i / 68.0) * 0.28 else 0.0
            val impulse = if (i < 5) (1.0 - i / 5.0) * 0.50 else 0.0
            val sample = (body + upper + transient + impulse) * envelope * attack * amplitude
            out[i] = (sample.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).roundToInt().toShort()
        }
        return out
    }
}
