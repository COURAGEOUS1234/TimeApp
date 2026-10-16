package com.gabriel.timeapp

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/** Synthesised air-raid style siren: a tone sweeping 500 Hz → 1300 Hz → 500 Hz, looped. */
class Siren {
    private var track: AudioTrack? = null

    fun start() {
        val rate = 44100
        val seconds = 2.4
        val n = (rate * seconds).toInt()
        val pcm = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n                      // 0..1 over one cycle
            val sweep = if (t < 0.5) t * 2 else (1 - t) * 2 // up then down
            val freq = 500 + 800 * sweep
            phase += 2 * PI * freq / rate
            // add a harsh square-ish edge for a distorted, mechanical tone
            val s = sin(phase) * 0.7 + (if (sin(phase) > 0) 0.3 else -0.3)
            pcm[i] = (s * Short.MAX_VALUE * 0.9).toInt().toShort()
        }

        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(n * 2)
            .build()
        t.write(pcm, 0, n)
        t.setLoopPoints(0, n, -1)
        t.play()
        track = t
    }

    fun stop() {
        track?.let { try { it.stop() } catch (_: Exception) { }; it.release() }
        track = null
    }
}
