package com.example.flappybird

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import kotlin.random.Random

/**
 * Quản lý âm thanh tổng hợp (không cần file raw).
 *
 * - Toàn bộ AudioTrack được TẠO SẴN (pool) và tạo TRÊN LUỒNG AUDIO
 *   để không chặn UI thread lúc mở app hay lúc bấm.
 * - Phát lại dùng reloadStaticData() cho MODE_STATIC để luôn từ đầu mẫu.
 */
class SoundManager {

    var enabled = true

    private val sampleRate = 44100

    // Luồng riêng cho việc phát âm thanh: tránh chặn UI thread gây khựng khi bấm
    private val audioThread = HandlerThread("flappy-audio").apply { start() }
    private val audioHandler = Handler(audioThread.looper)

    @Volatile
    private var released = false

    // Tổng hợp sẵn các mẫu âm thanh (chỉ chạy 1 lần khi khởi tạo, rất nhẹ)
    private val jumpSamples = synthQuack()
    private val pointSamples = synthBeep(880.0, 1320.0, 130)
    private val hitSamples = synthNoiseBurst(240)
    private val gameOverSamples = synthDescend(660.0, 200.0, 450)

    // Pool track: nhiều track cho âm lặp lại nhanh (vỗ cánh)
    private val jumpPool = TrackPool(jumpSamples, 4)
    private val pointPool = TrackPool(pointSamples, 2)
    private val hitPool = TrackPool(hitSamples, 1)
    private val gameOverPool = TrackPool(gameOverSamples, 1)

    init {
        // Tạo sẵn AudioTrack trên luồng audio để UI thread không bị khựng
        audioHandler.post {
            if (released) return@post
            jumpPool.ensureBuilt()
            pointPool.ensureBuilt()
            hitPool.ensureBuilt()
            gameOverPool.ensureBuilt()
        }
    }

    fun playJump() { if (enabled) jumpPool.play() }
    fun playPoint() { if (enabled) pointPool.play() }
    fun playHit() { if (enabled) hitPool.play() }
    fun playGameOver() { if (enabled) gameOverPool.play() }

    /** Giải phóng toàn bộ AudioTrack */
    fun release() {
        released = true
        // Đẩy việc giải phóng vào cuối hàng đợi audio để không cắt ngang âm đang phát
        audioHandler.post {
            jumpPool.releaseTracks()
            pointPool.releaseTracks()
            hitPool.releaseTracks()
            gameOverPool.releaseTracks()
        }
        audioThread.quitSafely()
    }

    /** Nhóm AudioTrack dùng vòng xoay để có thể chồng âm mà không cần build lại */
    private inner class TrackPool(private val samples: ShortArray, private val count: Int) {
        private val tracks = ArrayList<AudioTrack>(count)
        private var idx = 0
        private var built = false

        /** Tạo track (chỉ gọi trên luồng audio) */
        fun ensureBuilt() {
            if (built) return
            built = true
            repeat(count) {
                val t = try { buildTrack(samples) } catch (_: Exception) { null }
                if (t != null) tracks.add(t)
            }
        }

        fun play() {
            if (released) return
            // Đẩy thao tác audio sang luồng riêng để không chặn UI thread
            audioHandler.post {
                if (released) return@post
                ensureBuilt()
                if (tracks.isEmpty()) return@post
                val t = tracks[idx]
                idx = (idx + 1) % tracks.size
                try {
                    // MODE_STATIC: stop() + reloadStaticData() để phát lại từ đầu mẫu
                    t.stop()
                    t.reloadStaticData()
                    t.play()
                } catch (_: Exception) {
                    // Bỏ qua nếu thiết bị không hỗ trợ
                }
            }
        }

        fun releaseTracks() {
            for (t in tracks) {
                try { t.stop() } catch (_: Exception) {}
                try { t.release() } catch (_: Exception) {}
            }
            tracks.clear()
        }
    }

    private fun buildTrack(samples: ShortArray): AudioTrack {
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
        return track
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
            val s = Math.sin(phase) + 0.35 * Math.sin(phase * 2.0) + 0.15 * (Random.nextDouble() * 2 - 1)
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
            val s = Math.sin(phase)
            data[i] = (s * env * 9000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return data
    }

    /** Tiếng va chạm (nhiễu trắng tắt dần) */
    private fun synthNoiseBurst(durationMs: Int): ShortArray {
        val n = sampleRate * durationMs / 1000
        val data = ShortArray(n)
        var last = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val white = Random.nextDouble() * 2 - 1
            // Lọc thông thấp nhẹ cho đỡ chói
            last = last * 0.55 + white * 0.45
            val env = Math.pow(1.0 - p, 2.0)
            data[i] = (last * env * 14000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return data
    }

    /** Tiếng tụt âm khi thua */
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