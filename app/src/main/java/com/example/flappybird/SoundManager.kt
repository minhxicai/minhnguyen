package com.example.flappybird

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper

/**
 * Quản lý âm thanh tổng hợp (không cần file raw).
 * Tạo nhiều hiệu ứng: vỗ cánh, ăn điểm, va chạm, game over.
 */
class SoundManager {

    var enabled = true

    private val sampleRate = 44100
    private val handler = Handler(Looper.getMainLooper())

    // Tổng hợp sẵn các mẫu âm thanh
    private val jumpSamples = synthQuack()
    private val pointSamples = synthBeep(880.0, 1320.0, 130)
    private val hitSamples = synthNoiseBurst(240)
    private val gameOverSamples = synthDescend(660.0, 200.0, 450)

    fun playJump() { if (enabled) play(jumpSamples, 160) }
    fun playPoint() { if (enabled) play(pointSamples, 130) }
    fun playHit() { if (enabled) play(hitSamples, 240) }
    fun playGameOver() { if (enabled) play(gameOverSamples, 450) }

    /** Phát một mẫu âm thanh rồi tự giải phóng bộ nhớ */
    private fun play(samples: ShortArray, durationMs: Int) {
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
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            handler.postDelayed({
                try { track.stop() } catch (_: Exception) {}
                try { track.release() } catch (_: Exception) {}
            }, durationMs.toLong() + 80)
        } catch (_: Exception) {
            // Bỏ qua nếu thiết bị không hỗ trợ
        }
    }

    /** Tiếng vịt kêu "quạc quạc" */
    private fun synthQuack(): ShortArray {
        val n = sampleRate * 160 / 1000
        val data = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val freq = 750.0 - 450.0 * p
            phase += 2.0 * Math.PI * freq / sampleRate
            val env = Math.sin(Math.PI * p)
            val s = Math.sin(phase) + 0.35 * Math.sin(phase * 2.0) + 0.15 * (Math.random() * 2 - 1)
            data[i] = (s * env * 8000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return data
    }

    /** Tiếng "tinh" ăn điểm */
    private fun synthBeep(f1: Double, f2: Double, durationMs: Int): ShortArray {
        val n = sampleRate * durationMs / 1000
        val data = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val freq = f1 + (f2 - f1) * p
            phase += 2.0 * Math.PI * freq / sampleRate
            val env = Math.sin(Math.PI * p)
            val s = Math.sin(phase) + 0.25 * Math.sin(phase * 2)
            data[i] = (s * env * 9000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return data
    }

    /** Tiếng va chạm (nhiễu) */
    private fun synthNoiseBurst(durationMs: Int): ShortArray {
        val n = sampleRate * durationMs / 1000
        val data = ShortArray(n)
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val env = Math.sin(Math.PI * p)
            val s = (Math.random() * 2 - 1) * env
            data[i] = (s * 12000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return data
    }

    /** Tiếng game over (giai điệu đi xuống) */
    private fun synthDescend(f1: Double, f2: Double, durationMs: Int): ShortArray {
        val n = sampleRate * durationMs / 1000
        val data = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val freq = f1 * Math.pow(f2 / f1, p)
            phase += 2.0 * Math.PI * freq / sampleRate
            val env = 1.0 - p
            val s = Math.sin(phase) + 0.3 * Math.sin(phase * 2)
            data[i] = (s * env * 9000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return data
    }
}