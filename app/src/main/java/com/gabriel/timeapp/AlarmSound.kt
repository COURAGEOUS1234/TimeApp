package com.gabriel.timeapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** Plays the synthesised siren or the device alarm tone, optionally fading in. */
class AlarmSound(private val c: Context) {
    private var track: AudioTrack? = null
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var vol = 1f

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val ramp = object : Runnable {
        override fun run() {
            vol = min(1f, vol + 0.032f) // ~30 s to full volume
            apply()
            if (vol < 1f) handler.postDelayed(this, 1000)
        }
    }

    fun start(mode: String, gradual: Boolean) {
        vol = if (gradual) 0.05f else 1f
        if (mode == Alarm.SOUND_TONE) startTone() else startSiren()
        apply()
        if (gradual) handler.postDelayed(ramp, 1000)
    }

    private fun apply() {
        track?.setVolume(vol)
        player?.setVolume(vol, vol)
    }

    private fun startTone() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(c, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(c, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            player?.release(); player = null
            startSiren()
        }
    }

    /** Air-raid style siren: tone sweeping 500 → 1300 → 500 Hz, looped. */
    private fun startSiren() {
        val rate = 44100
        val n = (rate * 2.4).toInt()
        val pcm = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / n
            val sweep = if (t < 0.5) t * 2 else (1 - t) * 2
            phase += 2 * PI * (500 + 800 * sweep) / rate
            val s = sin(phase) * 0.7 + (if (sin(phase) > 0) 0.3 else -0.3)
            pcm[i] = (s * Short.MAX_VALUE * 0.9).toInt().toShort()
        }
        val t = AudioTrack.Builder()
            .setAudioAttributes(attrs)
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
        handler.removeCallbacksAndMessages(null)
        track?.let { try { it.stop() } catch (_: Exception) { }; it.release() }
        track = null
        player?.let { try { it.stop() } catch (_: Exception) { }; it.release() }
        player = null
    }
}
