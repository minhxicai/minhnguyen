package com.example.flappybird

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper

/**
 * Tạo tiếng "quạc quạc" bằng cách tổng hợp sóng âm.
 * Không cần file âm thanh trong thư mục raw.
 */
class QuackSound {

    private val sampleRate = 44100
    private val durationMs = 160
    private val numSamples = sampleRate * durationMs / 1000
    private val samples: ShortArray = generate()

    /** Sinh mẫu âm thanh tiếng vịt kêu */
    private fun generate(): ShortArray {
        val data = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            // Tần số giảm dần tạo âm "quạc" đặc trưng
            val freq = 750.0 - 450.0 * progress
            phase += 2.0 * Math.PI * freq / sampleRate
            val sine = Math.sin(phase)
            val harmonic = 0.35 * Math.sin(phase * 2.0)
            val noise = (Math.random() * 2 - 1) * 0.12
            // Bao hình lên/xuống mượt để tiếng ngắn gọn
            val env = Math.sin(Math.PI * progress)
            val value = (sine + harmonic + noise) * env * 0.55
            data[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        return data
    }

    /** Phát tiếng quạc */
    fun play() {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(numSamples * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, numSamples)
            track.play()
            // Giải phóng sau khi phát xong
            Handler(Looper.getMainLooper()).postDelayed({
                try { track.stop() } catch (_: Exception) {}
                try { track.release() } catch (_: Exception) {}
            }, durationMs.toLong() + 80)
        } catch (_: Exception) {
            // Bỏ qua nếu thiết bị không hỗ trợ
        }
    }
}
