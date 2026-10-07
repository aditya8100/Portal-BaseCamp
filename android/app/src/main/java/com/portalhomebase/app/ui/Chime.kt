package com.portalhomebase.app.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.min
import kotlin.math.sin

// Two-tone reminder chime, synthesized — no asset files. Runs on a plain
// background thread; safe to call from a coroutine or effect.
fun playReminderChime(tag: String) {
    Thread {
        try {
            val rate = 22050
            val notes = listOf(880.0 to 0.28, 659.0 to 0.45)
            val total = (notes.sumOf { it.second } * rate).toInt() + rate / 10
            val buf = ShortArray(total)
            var pos = 0
            for ((freq, dur) in notes) {
                val n = (dur * rate).toInt()
                for (i in 0 until n) {
                    val t = i.toDouble() / rate
                    val attack = i / (rate * 0.02)
                    val release = (n - i) / (rate * 0.06)
                    val env = min(1.0, min(attack, release))
                    buf[pos++] = (sin(2 * Math.PI * freq * t) * 20000 * env).toInt().toShort()
                }
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(rate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buf, 0, buf.size)
            track.play()
            Thread.sleep((total * 1000L) / rate + 200)
            track.release()
            Log.i("BaseCamp", "chime fired for $tag")
        } catch (e: Exception) {
            Log.w("BaseCamp", "chime failed: ${e.message}")
        }
    }.start()
}
