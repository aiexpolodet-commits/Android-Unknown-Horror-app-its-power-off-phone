package com.example

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

object HorrorSoundGenerator {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    /**
     * Synthesizes and plays a truly loud, terrifying monstrous scream/roar
     * through AudioTrack in streaming mode for 30 seconds.
     */
    fun playMonsterRoar30s(context: Context) {
        stop()

        // Maximize all volume streams to ensure it is very loud
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxMusic = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                val maxAlarm = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
            }
        } catch (_: Exception) {}

        playbackJob = CoroutineScope(Dispatchers.Default).launch {
            val sampleRate = 44100
            val numSeconds = 30
            val totalSamples = sampleRate * numSeconds
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = minBuf.coerceAtLeast(4096)

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
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack = track
            track.setVolume(1.0f)
            track.play()

            val buffer = ShortArray(bufferSize)
            var generatedSamples = 0
            var phaseDeep = 0.0
            var phaseMid = 0.0
            var phaseHigh = 0.0

            while (isActive && generatedSamples < totalSamples) {
                val chunkSize = minOf(buffer.size, totalSamples - generatedSamples)
                for (i in 0 until chunkSize) {
                    val t = (generatedSamples + i).toDouble() / sampleRate

                    // Monstrous throat scream frequencies with pitch modulation:
                    // Guttural sub-rumble: 80Hz - 160Hz
                    // Core visceral scream: 700Hz - 1800Hz with fast vibrato/tremolo
                    // High-pitch screech: 2000Hz - 3400Hz
                    val fDeep = 110.0 + 50.0 * sin(2.0 * Math.PI * 3.2 * t)
                    val fMid = 1100.0 + 500.0 * sin(2.0 * Math.PI * 7.5 * t) + 250.0 * sin(2.0 * Math.PI * 23.0 * t)
                    val fHigh = 2600.0 + 800.0 * sin(2.0 * Math.PI * 14.0 * t)

                    phaseDeep += 2.0 * Math.PI * fDeep / sampleRate
                    phaseMid += 2.0 * Math.PI * fMid / sampleRate
                    phaseHigh += 2.0 * Math.PI * fHigh / sampleRate

                    // Harsh noisy waveform simulating a beast tearing vocal cords
                    val noise = Random.nextDouble() * 2.0 - 1.0
                    var v = (
                        0.45 * sin(phaseMid) +
                        0.30 * sin(phaseHigh) +
                        0.25 * sin(phaseDeep) +
                        0.25 * noise
                    )

                    // Extreme distortion clipping for max loudness
                    v = if (v > 0.5) 0.98 else if (v < -0.5) -0.98 else v * 1.8

                    buffer[i] = (v * 32760.0).toInt().coerceIn(-32767, 32767).toShort()
                }

                track.write(buffer, 0, chunkSize)
                generatedSamples += chunkSize
            }

            try {
                track.stop()
                track.release()
            } catch (_: Exception) {}
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
