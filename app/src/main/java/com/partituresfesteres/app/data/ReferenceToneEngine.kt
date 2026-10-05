package com.partituresfesteres.app.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generador local de nota de referència.
 *
 * S'utilitza AudioTrack en streaming perquè la freqüència puga canviar sense dependre
 * d'arxius d'àudio pregravats. El timbre combina el fonamental amb dos harmònics suaus:
 * continua sent una referència clara, però resulta menys agressiva que una sinusoide pura.
 */
class ReferenceToneEngine {
    @Volatile
    private var running = false

    private var audioTrack: AudioTrack? = null
    private var worker: Thread? = null

    @Synchronized
    fun start(frequencyHz: Double) {
        stop()
        try {
            val frequency = frequencyHz.coerceIn(40.0, 5000.0)
            val sampleRate = 44_100
            val minBuffer = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuffer * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            running = true
            track.play()

            worker = Thread({
                val frames = 1024
                val buffer = ShortArray(frames)
                var phase = 0.0
                val increment = 2.0 * PI * frequency / sampleRate.toDouble()
                val maxAmplitude = Short.MAX_VALUE * 0.34
                try {
                    while (running) {
                        for (i in buffer.indices) {
                            val sample =
                                0.82 * sin(phase) +
                                0.13 * sin(phase * 2.0) +
                                0.05 * sin(phase * 3.0)
                            buffer[i] = (sample * maxAmplitude).toInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                                .toShort()
                            phase += increment
                            if (phase >= 2.0 * PI) phase -= 2.0 * PI
                        }
                        track.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                    }
                } catch (_: Throwable) {
                    // El dispositiu pot retirar l'eixida d'àudio (BT, auriculars, etc.).
                    // En eixe cas simplement parem el generador sense afectar la resta de l'app.
                }
            }, "PartituresReferenceTone").apply {
                priority = Thread.NORM_PRIORITY
                start()
            }
        } catch (_: Throwable) {
            running = false
            try { audioTrack?.release() } catch (_: Throwable) {}
            audioTrack = null
            worker = null
        }
    }

    @Synchronized
    fun stop() {
        running = false
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
        } catch (_: Throwable) {
        }
        try {
            worker?.join(120)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        worker = null
        try {
            audioTrack?.release()
        } catch (_: Throwable) {
        }
        audioTrack = null
    }
}
