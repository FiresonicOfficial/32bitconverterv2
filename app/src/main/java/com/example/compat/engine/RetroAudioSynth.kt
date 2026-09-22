package com.example.compat.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object RetroAudioSynth {
    private const val SAMPLE_RATE = 22050
    private var isMuted = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun playBeep(frequencyHz: Float, durationMs: Int, type: WaveType = WaveType.SQUARE) {
        if (isMuted) return
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val numSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
                if (numSamples <= 0) return@launch
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / SAMPLE_RATE
                    val angle = 2.0 * Math.PI * frequencyHz * time
                    val sampleValue = when (type) {
                        WaveType.SINE -> sin(angle)
                        WaveType.SQUARE -> if (sin(angle) >= 0) 0.6 else -0.6
                        WaveType.SAWTOOTH -> (2.0 * (time * frequencyHz - Math.floor(time * frequencyHz + 0.5)))
                    }
                    // Apply subtle decay envelope to eliminate pop
                    val envelope = (1.0 - (i.toDouble() / numSamples))
                    buffer[i] = (sampleValue * envelope * 14000).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
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
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                // Release when done
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore audio errors gracefully on restricted devices
            }
        }
    }

    fun playJump() = playBeep(520f, 90, WaveType.SQUARE)
    fun playScore() = playBeep(880f, 130, WaveType.SINE)
    fun playLaser() = playBeep(740f, 70, WaveType.SAWTOOTH)
    fun playExplosion() = playBeep(140f, 180, WaveType.SAWTOOTH)
    fun playPowerup() = playBeep(660f, 150, WaveType.SINE)
    fun playHit() = playBeep(210f, 120, WaveType.SQUARE)

    enum class WaveType {
        SINE, SQUARE, SAWTOOTH
    }
}
