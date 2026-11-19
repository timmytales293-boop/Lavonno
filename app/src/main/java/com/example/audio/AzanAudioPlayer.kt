package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates a gentle, resonant Islamic harmonic chime / Takbir-inspired cadence
 * completely offline using AudioTrack so Azan alarms always sound reliably.
 */
object AzanAudioPlayer {
    private var playJob: Job? = null

    fun playAzanChime() {
        stop()
        playJob = CoroutineScope(Dispatchers.IO).launch {
            val sampleRate = 22050
            // Hijaz-inspired soothing intervals (D4, F4, F#4, A4, F#4, D4)
            val notes = listOf(
                293.66 to 0.75,
                369.99 to 0.65,
                392.00 to 0.85,
                440.00 to 1.10,
                392.00 to 0.65,
                369.99 to 0.70,
                293.66 to 1.30
            )
            val totalSamples = notes.sumOf { (it.second * sampleRate).toInt() }
            val pcm = ShortArray(totalSamples)
            var offset = 0

            for ((freq, durationSec) in notes) {
                if (!isActive) break
                val count = (durationSec * sampleRate).toInt()
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val attack = (t / 0.05).coerceAtMost(1.0)
                    val decay = exp(-2.2 * t)
                    val wave = sin(2.0 * PI * freq * t) * 0.7 +
                        sin(2.0 * PI * (freq * 2.0) * t) * 0.2 +
                        sin(2.0 * PI * (freq * 0.5) * t) * 0.1
                    val sample = (wave * attack * decay * Short.MAX_VALUE * 0.55).toInt()
                        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    pcm[offset + i] = sample.toShort()
                }
                offset += count
            }

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
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
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            try {
                track.write(pcm, 0, pcm.size)
                track.play()
                val durationMs = notes.sumOf { (it.second * 1000).toLong() }
                kotlinx.coroutines.delay(durationMs + 200L)
            } finally {
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }
    }

    fun stop() {
        playJob?.cancel()
        playJob = null
    }
}
