package com.example.flappybird

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Game Flappy Bird vẽ hoàn toàn bằng Canvas (không dùng ảnh).
 * Trạng thái: MENU - PLAYING - GAME_OVER.
 * Tính năng: 3 độ khó, đồng xu, hạt hiệu ứng, rung, huy chương,
 * chủ đề ngày/đêm, âm thanh tổng hợp, lưu điểm cao.
 */
class GameView(context: Context) : View(context) {

    private enum class GameState { MENU, SHOP, PLAYING, GAME_OVER }

    /** Ba mức độ khó: hệ số khoảng trống ống và tốc độ cuộn */
    private enum class Difficulty(val label: String, val gapMul: Float, val speedMul: Float) {
        EASY("DỄ", 1.35f, 0.82f),
        NORMAL("THƯỜNG", 1.0f, 1.0f),
        HARD("KHÓ", 0.78f, 1.25f)
    }

    private enum class Theme { DAY, NIGHT }

    /** Skin cho chim: giá bằng xu tích luỹ, màu vẽ trực tiếp bằng Canvas */
    private enum class Skin(val label: String, val price: Int, val body: Int, val wing: Int, val belly: Int) {
        CLASSIC("CỔ ĐIỂN", 0, 0xFFFFD73C.toInt(), 0xFFF5B41E.toInt(), 0xFFFFF0AA.toInt()),
        RUBY("HỒNG NGỌC", 20, 0xFFFF6B6B.toInt(), 0xFFD93F3F.toInt(), 0xFFFFC2C2.toInt()),
        OCEAN("ĐẠI DƯƠNG", 30, 0xFF4FC3F7.toInt(), 0xFF2196C9.toInt(), 0xFFC8EEFF.toInt()),
        FOREST("RỪNG XANH", 40, 0xFF7ED957.toInt(), 0xFF4CAF35.toInt(), 0xFFD8F5C4.toInt()),
        GALAXY("VŨ TRỤ", 50, 0xFF9B6BFF.toInt(), 0xFF6A3FD1.toInt(), 0xFFDCC8FF.toInt())
    }

    private var state = GameState.MENU

    // ---- Kích thước màn hình ----
    private val d = resources.displayMetrics.density
    private var screenW = 0f
    private var screenH = 0f
    private var groundY = 0f
    private val groundHeight = 130f * d

    private val prefs = context.getSharedPreferences("flappy_prefs", Context.MODE_PRIVATE)
    private val soundManager = SoundManager()

    // Luồng nền cho rung/âm thanh: tránh chặn UI thread gây khựng
    private val effectThread = HandlerThread("flappy-effects").apply { start() }
    private val audioHandler = Handler(effectThread.looper)

    // ---- Cài đặt người chơi ----
    private var soundEnabled = true
    private var vibrateEnabled = true
    private var difficulty = Difficulty.NORMAL
    private var theme = Theme.DAY

    // ---- Điểm và thống kê ----
    private var score = 0
    private var coins = 0
    private var best = 0
    private var totalGames = 0
    private var totalScore = 0

    // ---- Ví xu & skin ----
    private var wallet = 0                          // Tổng xu tích luỹ để mua skin
    private val ownedSkins = mutableSetOf<Skin>()    // Skin đã sở hữu
    private var currentSkin = Skin.CLASSIC

    // ---- Chim ----
    private var birdX = 0f
    private var birdY = 0f
    private var birdVy = 0f
    private var birdAngle = 0f
    private var birdFlap = 0f
    private val birdR = 30f * d

    // ---- Ống ----
    private class Pipe(var x: Float, var gapY: Float, val gapH: Float) {
        var scored = false
    }
    private val pipes = mutableListOf<Pipe>()
    private val pipeW = 120f * d
    private var pipeSpacing = 0f
    private var baseGap = 0f
    private var scrollSpeed = 0f
    private var groundOffset = 0f

    // ---- Đồng xu ----
    private class Coin(var x: Float, var y: Float) {
        var taken = false
    }
    private val coinList = mutableListOf<Coin>()
    private val coinR = 18f * d

    // ---- Hạt hiệu ứng ----
    private class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var life: Float, val maxLife: Float, val radius: Float, val color: Int
    )
    private val particles = mutableListOf<Particle>()

    // ---- Mây nền ----
    private class Cloud(var x: Float, var y: Float, val r: Float, val speed: Float)
    private val clouds = mutableListOf<Cloud>()

    // ---- Hiệu ứng khác ----
    private var shakeTime = 0f
    private var flashTime = 0f
    private var lastFrameTime = 0L
    private var menuTime = 0L
    private var gameOverAt = 0L

    // ---- Paints ----
    private val skyPaint = Paint()
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val pipePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(106, 190, 74) }
    private val pipeDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(76, 150, 52) }
    private val pipeCapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(126, 210, 92) }
    private val pipeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(60, 130, 44); style = Paint.Style.STROKE; strokeWidth = 3f * d
    }

    private val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(222, 202, 141) }
    private val groundTopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(120, 190, 80) }
    private val groundDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(196, 176, 118) }

    private val birdBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 215, 60) }
    private val birdWingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245, 180, 30) }
    private val birdEyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val birdPupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 40, 40) }
    private val birdBeakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 120, 30) }
    private val birdOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(180, 130, 20); style = Paint.Style.STROKE; strokeWidth = 3f * d
    }
    // Các lớp phụ trợ để tạo khối và độ bóng cho chim
    private val birdHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val birdBlushPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val coinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 205, 45) }
    private val coinBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(200, 140, 0); style = Paint.Style.STROKE; strokeWidth = 4f * d
    }
    // Ruột đồng xu dùng paint riêng, tránh đổi màu paint viền giữa các frame
    private val coinInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(200, 140, 0) }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        textSize = 46f * d; setShadowLayer(6f * d, 0f, 3f * d, Color.argb(150, 0, 0, 0))
    }
    private val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT
        textSize = 34f * d; setShadowLayer(5f * d, 0f, 2f * d, Color.argb(150, 0, 0, 0))
    }
    private val bigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        textSize = 84f * d; setShadowLayer(8f * d, 0f, 4f * d, Color.argb(160, 0, 0, 0))
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 236, 120); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; textSize = 72f * d
        setShadowLayer(10f * d, 0f, 5f * d, Color.argb(170, 0, 0, 0))
    }

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(252, 243, 214) }
    private val panelBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(160, 130, 70); style = Paint.Style.STROKE; strokeWidth = 6f * d
    }
    private val panelTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(90, 70, 30); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; textSize = 44f * d
    }
    private val panelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(120, 95, 45); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; textSize = 38f * d
    }
    private val panelSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(140, 115, 60); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT; textSize = 28f * d
    }

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 196, 60) }
    private val buttonBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(190, 130, 20); style = Paint.Style.STROKE; strokeWidth = 5f * d
    }
    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(90, 60, 10); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; textSize = 36f * d
    }

    // Paint cho dòng credit góc phải
    private val creditPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(170, 255, 255, 255); textAlign = Paint.Align.RIGHT
        typeface = Typeface.DEFAULT; textSize = 22f * d
        setShadowLayer(4f * d, 0f, 2f * d, Color.argb(130, 0, 0, 0))
    }

    private val medalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    init {
        isFocusable = true
        loadSettings()
    }

    // ---------- Kích thước ----------
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenW = w.toFloat()
        screenH = h.toFloat()
        groundY = screenH - groundHeight
        birdX = screenW * 0.28f
        if (birdY == 0f) birdY = screenH * 0.42f
        baseGap = screenH * 0.24f
        pipeSpacing = screenW * 0.62f
        scrollSpeed = screenW * 0.0042f

        // Tạo mây nền
        if (clouds.isEmpty()) {
            repeat(6) {
                clouds.add(
                    Cloud(
                        x = Random.nextFloat() * screenW,
                        y = screenH * (0.08f + Random.nextFloat() * 0.30f),
                        r = (36f + Random.nextFloat() * 30f) * d,
                        speed = (0.4f + Random.nextFloat() * 0.5f) * d
                    )
                )
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Giải phóng AudioTrack khi view bị huỷ để tránh rò rỉ
        try { soundManager.release() } catch (_: Exception) {}
        // Dừng luồng nền hiệu ứng
        try { effectThread.quitSafely() } catch (_: Exception) {}
    }

    // ---------- Cài đặt ----------
    private fun loadSettings() {
        soundEnabled = prefs.getBoolean("sound", true)
        vibrateEnabled = prefs.getBoolean("vibrate", true)
        theme = if (prefs.getString("theme", "DAY") == "NIGHT") Theme.NIGHT else Theme.DAY
        difficulty = when (prefs.getString("difficulty", "NORMAL")) {
            "EASY" -> Difficulty.EASY
            "HARD" -> Difficulty.HARD
            else -> Difficulty.NORMAL
        }
        best = prefs.getInt("best", 0)
        totalGames = prefs.getInt("totalGames", 0)
        totalScore = prefs.getInt("totalScore", 0)
        wallet = prefs.getInt("wallet", 0)
        // Nạp danh sách skin đã mua
        ownedSkins.clear()
        ownedSkins.add(Skin.CLASSIC)
        prefs.getStringSet("ownedSkins", emptySet())?.forEach { name ->
            Skin.values().firstOrNull { it.name == name }?.let { ownedSkins.add(it) }
        }
        currentSkin = Skin.values().firstOrNull { it.name == prefs.getString("skin", "CLASSIC") } ?: Skin.CLASSIC
        soundManager.enabled = soundEnabled
    }

    private fun saveSettings() {
        prefs.edit()
            .putBoolean("sound", soundEnabled)
            .putBoolean("vibrate", vibrateEnabled)
            .putString("theme", theme.name)
            .putString("difficulty", difficulty.name)
            .putInt("best", best)
            .putInt("totalGames", totalGames)
            .putInt("totalScore", totalScore)
            .putInt("wallet", wallet)
            .putStringSet("ownedSkins", ownedSkins.map { it.name }.toSet())
            .putString("skin", currentSkin.name)
            .apply()
    }

    private fun playSound(action: () -> Unit) {
        if (soundEnabled) action()
    }

    private fun vibrate(ms: Long) {
        if (!vibrateEnabled) return
        // Đẩy rung sang luồng riêng để không chặn UI thread khi bấm
        audioHandler.post { doVibrate(ms) }
    }

    private fun doVibrate(ms: Long) {
        try {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }

    // ---------- Điều khiển trận ----------
    private fun startGame() {
        state = GameState.PLAYING
        score = 0
        coins = 0
        birdY = screenH * 0.42f
        birdVy = 0f
        birdAngle = 0f
        birdFlap = 0f
        pipes.clear()
        coinList.clear()
        particles.clear()
        shakeTime = 0f
        flashTime = 0f
        spawnPipe(screenW + pipeW * 0.5f)
    }

    private fun gameOver() {
        if (state == GameState.GAME_OVER) return
        state = GameState.GAME_OVER
        gameOverAt = System.currentTimeMillis()
        shakeTime = 0.20f
        flashTime = 0.12f
        vibrate(140)
        playSound { soundManager.playHit() }
        spawnParticles(birdX, birdY, 22, Color.rgb(255, 220, 90), 90f * d, 320f * d, 5f * d)
        spawnParticles(birdX, birdY, 12, Color.WHITE, 60f * d, 220f * d, 4f * d)
        totalGames++
        totalScore += score
        if (score > best) best = score
        saveSettings()   // lưu cả wallet để xu nhặt được không mất
    }

    private fun flap() {
        birdVy = -screenH * 0.0125f
        birdFlap = 1f
        playSound { soundManager.playJump() }
        spawnParticles(birdX - birdR * 0.5f, birdY + birdR * 0.3f, 4, Color.argb(180, 255, 255, 255), 40f * d, 120f * d, 3f * d)
    }

    // ---------- Vòng lặp vật lý ----------
    private fun update(dtMs: Float) {
        val dt = (dtMs / 16.6667f).coerceIn(0f, 3f)

        if (shakeTime > 0f) shakeTime = (shakeTime - dtMs / 1000f).coerceAtLeast(0f)
        if (flashTime > 0f) flashTime = (flashTime - dtMs / 1000f).coerceAtLeast(0f)
        if (birdFlap > 0f) birdFlap = (birdFlap - dt * 0.10f).coerceAtLeast(0f)

        // Hạt hiệu ứng
        val pit = particles.iterator()
        while (pit.hasNext()) {
            val p = pit.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += 260f * d * dt * 0.016f
            p.life -= dt
            if (p.life <= 0f) pit.remove()
        }

        // Mây trôi (luôn chạy)
        for (c in clouds) {
            c.x -= c.speed * dt
            if (c.x + c.r < 0f) {
                c.x = screenW + c.r
                c.y = screenH * (0.08f + Random.nextFloat() * 0.30f)
            }
        }

        if (state != GameState.PLAYING) return

        // Trọng lực và góc nghiêng
        birdVy += screenH * 0.00075f * dt
        birdY += birdVy * dt
        birdAngle = (birdVy / (screenH * 0.012f)).coerceIn(-0.6f, 1.2f)

        // Chạm trần
        if (birdY - birdR < 0f) {
            birdY = birdR
            birdVy = 0f
        }

        // Chạm đất
        if (birdY + birdR >= groundY) {
            birdY = groundY - birdR
            gameOver()
            return
        }

        // Cuộn ống
        val speed = scrollSpeed * difficulty.speedMul
        val oit = pipes.iterator()
        while (oit.hasNext()) {
            val p = oit.next()
            p.x -= speed * dt

            // Tính điểm khi vượt qua ống
            if (!p.scored && p.x + pipeW < birdX - birdR * 0.5f) {
                p.scored = true
                score++
                playSound { soundManager.playPoint() }
                spawnParticles(birdX + birdR, birdY, 6, Color.rgb(255, 255, 255), 50f * d, 150f * d, 3f * d)
            }

            // Va chạm ống
            if (collides(p)) {
                gameOver()
                return
            }

            if (p.x + pipeW < -pipeW) oit.remove()
        }

        // Đồng xu
        val cit = coinList.iterator()
        while (cit.hasNext()) {
            val c = cit.next()
            c.x -= speed * dt
            val dx = c.x - birdX
            val dy = c.y - birdY
            if (!c.taken && dx * dx + dy * dy < (coinR + birdR * 0.7f) * (coinR + birdR * 0.7f)) {
                coins++
                wallet++          // Tích xu vào ví để mua skin
                score += 2
                playSound { soundManager.playPoint() }
                spawnParticles(c.x, c.y, 12, Color.rgb(255, 210, 60), 70f * d, 260f * d, 4f * d)
                cit.remove()
                continue
            }
            if (c.x + coinR < 0f) cit.remove()
        }

        // Sinh ống mới
        val lastPipe = pipes.maxByOrNull { it.x }
        if (lastPipe == null || lastPipe.x < screenW - pipeSpacing) {
            spawnPipe(screenW + pipeW)
        }

        // Cập nhật cuộn mặt đất
        groundOffset = (groundOffset + speed * dt) % (40f * d)
    }

    private fun spawnPipe(x: Float) {
        val gapH = baseGap * difficulty.gapMul
        val margin = gapH * 0.5f + 60f * d
        val minCenter = margin
        val maxCenter = (groundY - margin).coerceAtLeast(minCenter + 1f)
        val gapY = minCenter + Random.nextFloat() * (maxCenter - minCenter)
        val pipe = Pipe(x, gapY, gapH)
        pipes.add(pipe)

        // Thỉnh thoảng đặt đồng xu giữa khoảng trống
        if (Random.nextFloat() < 0.65f) {
            coinList.add(Coin(x + pipeW * 0.5f, gapY))
        }
    }

    private fun collides(p: Pipe): Boolean {
        val gapTop = p.gapY - p.gapH * 0.5f
        val gapBottom = p.gapY + p.gapH * 0.5f
        val birdLeft = birdX - birdR * 0.85f
        val birdRight = birdX + birdR * 0.85f
        val birdTop = birdY - birdR * 0.85f
        val birdBottom = birdY + birdR * 0.85f
        if (birdRight < p.x || birdLeft > p.x + pipeW) return false
        if (birdTop < gapTop) return true
        if (birdBottom > gapBottom) return true
        return false
    }

    private fun spawnParticles(
        x: Float, y: Float, count: Int, color: Int,
        minSpeed: Float, maxSpeed: Float, radius: Float
    ) {
        repeat(count) {
            val ang = Random.nextFloat() * (Math.PI * 2).toFloat()
            val sp = minSpeed + Random.nextFloat() * (maxSpeed - minSpeed)
            val life = 0.35f + Random.nextFloat() * 0.45f
            particles.add(
                Particle(
                    x = x, y = y,
                    vx = kotlin.math.cos(ang) * sp * 0.016f,
                    vy = sin(ang) * sp * 0.016f,
                    life = life, maxLife = life,
                    radius = radius * (0.6f + Random.nextFloat() * 0.8f),
                    color = color
                )
            )
        }
    }

    // ---------- Bố cục nút ----------
    private fun diffRect(i: Int): RectF {
        val w = screenW * 0.27f
        val h = 60f * d
        val gap = screenW * 0.03f
        val total = w * 3 + gap * 2
        val left = (screenW - total) / 2f + i * (w + gap)
        val top = screenH * 0.40f
        return RectF(left, top, left + w, top + h)
    }

    private fun toggleRect(i: Int): RectF {
        val w = screenW * 0.27f
        val h = 60f * d
        val gap = screenW * 0.03f
        val total = w * 3 + gap * 2
        val left = (screenW - total) / 2f + i * (w + gap)
        val top = screenH * 0.54f
        return RectF(left, top, left + w, top + h)
    }

    private fun startRect(): RectF {
        val w = screenW * 0.64f
        val h = 84f * d
        val left = (screenW - w) / 2f
        val top = screenH * 0.645f
        return RectF(left, top, left + w, top + h)
    }

    /** Nút mở cửa hàng skin ở sảnh chờ */
    private fun shopBtnRect(): RectF {
        val w = screenW * 0.52f
        val h = 72f * d
        val left = (screenW - w) / 2f
        val top = screenH * 0.795f
        return RectF(left, top, left + w, top + h)
    }

    /** Ô chọn skin trong cửa hàng (2 cột) */
    private fun skinRect(i: Int): RectF {
        val cols = 2
        val w = screenW * 0.40f
        val h = 96f * d
        val gapX = screenW * 0.04f
        val gapY = 14f * d
        val totalW = w * cols + gapX
        val left0 = (screenW - totalW) / 2f
        val top0 = screenH * 0.24f
        val col = i % cols
        val row = i / cols
        val left = left0 + col * (w + gapX)
        val top = top0 + row * (h + gapY)
        return RectF(left, top, left + w, top + h)
    }

    /** Nút quay lại sảnh từ cửa hàng */
    private fun shopBackRect(): RectF {
        val w = screenW * 0.52f
        val h = 74f * d
        val left = (screenW - w) / 2f
        val top = screenH * 0.845f
        return RectF(left, top, left + w, top + h)
    }

    private fun panelRect(): RectF {
        val w = screenW * 0.86f
        val h = screenH * 0.48f
        val left = (screenW - w) / 2f
        val top = screenH * 0.20f
        return RectF(left, top, left + w, top + h)
    }

    private fun retryRect(): RectF {
        val p = panelRect()
        val w = p.width() * 0.42f
        val h = 78f * d
        val top = p.bottom - h - 28f * d
        return RectF(p.left + p.width() * 0.05f, top, p.left + p.width() * 0.05f + w, top + h)
    }

    private fun menuBtnRect(): RectF {
        val p = panelRect()
        val w = p.width() * 0.42f
        val h = 78f * d
        val top = p.bottom - h - 28f * d
        return RectF(p.right - p.width() * 0.05f - w, top, p.right - p.width() * 0.05f, top + h)
    }

    // ---------- Chạm ----------
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        val x = event.x
        val y = event.y

        when (state) {
            GameState.MENU -> {
                // Chọn độ khó
                for (i in 0 until 3) {
                    if (diffRect(i).contains(x, y)) {
                        difficulty = Difficulty.values()[i]
                        saveSettings()
                        playSound { soundManager.playPoint() }
                        return true
                    }
                }
                // Bật/tắt âm thanh, rung, chủ đề
                for (i in 0 until 3) {
                    if (toggleRect(i).contains(x, y)) {
                        when (i) {
                            0 -> { soundEnabled = !soundEnabled; soundManager.enabled = soundEnabled }
                            1 -> vibrateEnabled = !vibrateEnabled
                            else -> theme = if (theme == Theme.DAY) Theme.NIGHT else Theme.DAY
                        }
                        saveSettings()
                        return true
                    }
                }
                if (startRect().contains(x, y)) { startGame(); return true }
                if (shopBtnRect().contains(x, y)) {
                    state = GameState.SHOP
                    playSound { soundManager.playPoint() }
                    return true
                }
            }

            GameState.SHOP -> {
                // Chọn/mua skin
                for (i in Skin.values().indices) {
                    if (!skinRect(i).contains(x, y)) continue
                    val sk = Skin.values()[i]
                    if (ownedSkins.contains(sk)) {
                        // Đã sở hữu -> trang bị
                        currentSkin = sk
                        saveSettings()
                        playSound { soundManager.playPoint() }
                    } else if (wallet >= sk.price) {
                        // Đủ xu -> mua và trang bị luôn
                        wallet -= sk.price
                        ownedSkins.add(sk)
                        currentSkin = sk
                        saveSettings()
                        playSound { soundManager.playPoint() }
                        vibrate(40)
                    } else {
                        // Không đủ xu
                        vibrate(25)
                    }
                    return true
                }
                if (shopBackRect().contains(x, y)) {
                    state = GameState.MENU
                    lastFrameTime = 0L
                    return true
                }
            }

            GameState.PLAYING -> flap()

            GameState.GAME_OVER -> {
                // Tránh chạm nhầm ngay khi vừa thua
                if (System.currentTimeMillis() - gameOverAt < 400L) return true
                if (retryRect().contains(x, y)) {
                    startGame()
                } else if (menuBtnRect().contains(x, y)) {
                    state = GameState.MENU
                    lastFrameTime = 0L
                }
            }
        }
        return true
    }

    // ---------- Vẽ khung hình ----------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.currentTimeMillis()
        val dtMs = if (lastFrameTime == 0L) 16f else (now - lastFrameTime).toFloat().coerceIn(0f, 50f)
        lastFrameTime = now

        when (state) {
            GameState.PLAYING -> update(dtMs)
            GameState.MENU -> updateMenu(dtMs)
            GameState.SHOP -> updateMenu(dtMs)
            GameState.GAME_OVER -> updateFallingBird(dtMs)
        }

        canvas.save()
        if (shakeTime > 0f) {
            val power = shakeTime * 22f * d
            canvas.translate((Random.nextFloat() - 0.5f) * power, (Random.nextFloat() - 0.5f) * power)
        }

        drawBackground(canvas)
        drawPipes(canvas)
        drawCoins(canvas)
        drawParticles(canvas)
        drawGround(canvas)
        drawBird(canvas)

        canvas.restore()

        when (state) {
            GameState.MENU -> drawMenu(canvas)
            GameState.SHOP -> drawShop(canvas)
            GameState.PLAYING -> drawHud(canvas)
            GameState.GAME_OVER -> {
                drawHud(canvas)
                drawGameOver(canvas)
            }
        }

        // Credit chỉ hiển thị ở trang chủ
        if (state == GameState.MENU) drawCredit(canvas)

        if (flashTime > 0f) {
            flashPaint.alpha = ((flashTime / 0.4f) * 190f).toInt().coerceIn(0, 190)
            canvas.drawRect(0f, 0f, screenW, screenH, flashPaint)
        }

        postInvalidateOnAnimation()
    }

    /** Cập nhật cảnh nền ở màn hình menu (chim bay lơ lửng) */
    private fun updateMenu(dtMs: Float) {
        val dt = dtMs / 16.6667f
        menuTime += dtMs.toLong()
        for (cl in clouds) {
            cl.x -= cl.speed * dt * 0.4f
            if (cl.x + cl.r < 0f) {
                cl.x = screenW + cl.r
                cl.y = screenH * (0.08f + Random.nextFloat() * 0.26f)
            }
        }
        groundOffset = (groundOffset + 2f * d * dt) % (40f * d)
        birdY = screenH * 0.48f + sin(menuTime / 320.0).toFloat() * 16f * d
        birdAngle = 0f
        birdFlap = ((sin(menuTime / 190.0).toFloat() + 1f) / 2f)
    }

    /** Chim rơi tự do sau khi thua */
    private fun updateFallingBird(dtMs: Float) {
        val dt = dtMs / 16.6667f
        if (birdY + birdR < groundY) {
            birdVy += screenH * 0.00085f * dt
            birdY += birdVy * dt
            birdAngle = (birdAngle + 0.06f * dt).coerceAtMost(1.5f)
        } else {
            birdY = groundY - birdR
            birdVy = 0f
        }
    }

    // ---------- Nền trời ----------
    private fun drawBackground(c: Canvas) {
        if (theme == Theme.DAY) {
            skyPaint.color = Color.rgb(110, 197, 236)
            c.drawRect(0f, 0f, screenW, screenH, skyPaint)
            skyPaint.color = Color.rgb(160, 219, 245)
            c.drawRect(0f, screenH * 0.45f, screenW, screenH, skyPaint)

            // Mặt trời
            sunPaint.color = Color.argb(70, 255, 245, 170)
            c.drawCircle(screenW * 0.80f, screenH * 0.14f, 78f * d, sunPaint)
            sunPaint.color = Color.rgb(255, 238, 150)
            c.drawCircle(screenW * 0.80f, screenH * 0.14f, 48f * d, sunPaint)
        } else {
            skyPaint.color = Color.rgb(16, 24, 54)
            c.drawRect(0f, 0f, screenW, screenH, skyPaint)
            skyPaint.color = Color.rgb(28, 40, 82)
            c.drawRect(0f, screenH * 0.45f, screenW, screenH, skyPaint)

            // Sao
            starPaint.color = Color.argb(220, 255, 255, 230)
            for (i in 0 until 30) {
                val sx = (((i * 137) % 100) / 100f) * screenW
                val sy = (((i * 61) % 55) / 100f) * screenH
                val r = (1.2f + (i % 3) * 0.8f) * d
                starPaint.alpha = 130 + (i % 5) * 25
                c.drawCircle(sx, sy, r, starPaint)
            }

            // Trăng
            sunPaint.color = Color.argb(60, 220, 230, 255)
            c.drawCircle(screenW * 0.80f, screenH * 0.14f, 74f * d, sunPaint)
            sunPaint.color = Color.rgb(238, 242, 255)
            c.drawCircle(screenW * 0.80f, screenH * 0.14f, 44f * d, sunPaint)
            skyPaint.color = Color.rgb(16, 24, 54)
            c.drawCircle(screenW * 0.80f + 20f * d, screenH * 0.14f - 12f * d, 40f * d, skyPaint)
        }

        // Mây
        cloudPaint.color = if (theme == Theme.DAY) Color.argb(210, 255, 255, 255)
        else Color.argb(70, 200, 214, 255)
        for (cl in clouds) {
            c.drawCircle(cl.x, cl.y, cl.r, cloudPaint)
            c.drawCircle(cl.x + cl.r * 0.85f, cl.y + cl.r * 0.18f, cl.r * 0.78f, cloudPaint)
            c.drawCircle(cl.x - cl.r * 0.85f, cl.y + cl.r * 0.22f, cl.r * 0.68f, cloudPaint)
        }
    }

    // ---------- Ống ----------
    private fun pipeGapTop(p: Pipe) = p.gapY - p.gapH / 2f
    private fun pipeGapBottom(p: Pipe) = p.gapY + p.gapH / 2f

    private fun drawPipes(c: Canvas) {
        val capH = 36f * d
        val capOver = 9f * d
        for (p in pipes) {
            val gt = pipeGapTop(p)
            val gb = pipeGapBottom(p)

            // Thân ống trên
            c.drawRect(p.x, -20f * d, p.x + pipeW, gt - capH, pipePaint)
            // Sọc sáng
            c.drawRect(p.x + pipeW * 0.16f, -20f * d, p.x + pipeW * 0.30f, gt - capH, pipeCapPaint)
            c.drawRect(p.x + pipeW * 0.74f, -20f * d, p.x + pipeW * 0.88f, gt - capH, pipeDarkPaint)
            // Nắp ống trên
            c.drawRect(p.x - capOver, gt - capH, p.x + pipeW + capOver, gt, pipeCapPaint)
            c.drawRect(p.x - capOver, gt - capH, p.x + pipeW + capOver, gt, pipeLinePaint)

            // Thân ống dưới
            c.drawRect(p.x, gb + capH, p.x + pipeW, groundY, pipePaint)
            c.drawRect(p.x + pipeW * 0.16f, gb + capH, p.x + pipeW * 0.30f, groundY, pipeCapPaint)
            c.drawRect(p.x + pipeW * 0.74f, gb + capH, p.x + pipeW * 0.88f, groundY, pipeDarkPaint)
            // Nắp ống dưới
            c.drawRect(p.x - capOver, gb, p.x + pipeW + capOver, gb + capH, pipeCapPaint)
            c.drawRect(p.x - capOver, gb, p.x + pipeW + capOver, gb + capH, pipeLinePaint)
        }
    }

    // ---------- Đồng xu ----------
    private fun drawCoins(c: Canvas) {
        for (coin in coinList) {
            // Hiệu ứng xoay: co giãn theo chiều ngang
            val k = (sin(menuTime / 180.0 + coin.x * 0.02).toFloat() * 0.5f + 0.5f)
            val wScale = 0.35f + 0.65f * k
            c.save()
            c.scale(wScale, 1f, coin.x, coin.y)
            c.drawCircle(coin.x, coin.y, coinR, coinPaint)
            c.drawCircle(coin.x, coin.y, coinR, coinBorderPaint)
            c.drawCircle(coin.x, coin.y, coinR * 0.45f, coinInnerPaint)
            c.restore()
        }
    }

    // ---------- Hạt hiệu ứng ----------
    private fun drawParticles(c: Canvas) {
        for (p in particles) {
            val alpha = ((p.life / p.maxLife) * 255f).toInt().coerceIn(0, 255)
            particlePaint.color = p.color
            particlePaint.alpha = alpha
            c.drawCircle(p.x, p.y, p.radius, particlePaint)
        }
    }

    // ---------- Mặt đất ----------
    private fun drawGround(c: Canvas) {
        c.drawRect(0f, groundY, screenW, screenH, groundPaint)
        // Dải cỏ phía trên
        c.drawRect(0f, groundY, screenW, groundY + 18f * d, groundTopPaint)
        // Vạch trang trí chạy theo mặt đất
        val step = 40f * d
        var x = -groundOffset
        while (x < screenW) {
            c.drawRect(x, groundY + 30f * d, x + step * 0.5f, groundY + 38f * d, groundDarkPaint)
            x += step
        }
    }

    // ---------- Chim ----------
    // Object tái sử dụng: tránh cấp phát mỗi frame (nguồn gây GC -> khựng)
    private val tailPath = android.graphics.Path()
    private val wingPath = android.graphics.Path()
    private val wingInnerPath = android.graphics.Path()
    private val beakTopPath = android.graphics.Path()
    private val beakBottomPath = android.graphics.Path()
    private val miniBeakPath = android.graphics.Path()
    private val birdOvalRect = RectF()
    private val birdHighlightRect = RectF()

    private fun drawBird(c: Canvas) {
        val r = birdR
        c.save()
        c.rotate(birdAngle * 32f, birdX, birdY)

        // Áp màu skin hiện tại
        birdBodyPaint.color = currentSkin.body
        birdWingPaint.color = currentSkin.wing

        // Màu phụ trợ cho khối bóng và má hồng
        birdHighlightPaint.color = Color.argb(115, 255, 255, 255)
        birdShadowPaint.color = Color.argb(45, 0, 0, 0)
        birdBlushPaint.color = Color.argb(85, 255, 120, 120)

        // Đuôi chim phía sau thân
        val tail = tailPath
        tail.rewind()
        tail.moveTo(birdX - r * 0.70f, birdY - r * 0.16f)
        tail.lineTo(birdX - r * 1.46f, birdY - r * 0.52f)
        tail.lineTo(birdX - r * 1.26f, birdY + r * 0.02f)
        tail.lineTo(birdX - r * 1.50f, birdY + r * 0.48f)
        tail.lineTo(birdX - r * 0.64f, birdY + r * 0.34f)
        tail.close()
        c.drawPath(tail, birdWingPaint)
        c.drawPath(tail, birdOutlinePaint)

        // Thân kèm bóng đổ nhẹ phía dưới
        birdOvalRect.set(birdX - r * 0.92f, birdY - r * 0.08f, birdX + r * 0.92f, birdY + r * 1.04f)
        c.drawOval(birdOvalRect, birdShadowPaint)
        c.drawCircle(birdX, birdY, r, birdBodyPaint)
        // Vùng sáng phía trên tạo khối bóng tròn
        birdHighlightRect.set(birdX - r * 0.74f, birdY - r * 0.93f, birdX + r * 0.34f, birdY - r * 0.08f)
        c.drawOval(birdHighlightRect, birdHighlightPaint)
        c.drawCircle(birdX, birdY, r, birdOutlinePaint)

        // Cánh vỗ: xoay quanh khớp cánh theo nhịp vỗ
        val wingAngle = -38f + birdFlap * 78f
        c.save()
        c.rotate(wingAngle, birdX - r * 0.10f, birdY + r * 0.10f)
        val wing = wingPath
        wing.rewind()
        wing.moveTo(birdX - r * 0.02f, birdY - r * 0.10f)
        wing.quadTo(birdX - r * 0.90f, birdY - r * 0.42f, birdX - r * 1.24f, birdY + r * 0.10f)
        wing.quadTo(birdX - r * 0.86f, birdY + r * 0.46f, birdX - r * 0.02f, birdY + r * 0.34f)
        wing.close()
        c.drawPath(wing, birdWingPaint)
        c.drawPath(wing, birdOutlinePaint)
        // Vệt sáng trên cánh
        val wingInner = wingInnerPath
        wingInner.rewind()
        wingInner.moveTo(birdX - r * 0.12f, birdY + r * 0.02f)
        wingInner.quadTo(birdX - r * 0.72f, birdY - r * 0.20f, birdX - r * 0.96f, birdY + r * 0.14f)
        wingInner.quadTo(birdX - r * 0.70f, birdY + r * 0.30f, birdX - r * 0.12f, birdY + r * 0.22f)
        wingInner.close()
        c.drawPath(wingInner, birdHighlightPaint)
        c.restore()

        // Mắt
        c.drawCircle(birdX + r * 0.40f, birdY - r * 0.34f, r * 0.36f, birdEyePaint)
        c.drawCircle(birdX + r * 0.40f, birdY - r * 0.34f, r * 0.36f, birdOutlinePaint)
        c.drawCircle(birdX + r * 0.52f, birdY - r * 0.30f, r * 0.17f, birdPupilPaint)
        c.drawCircle(birdX + r * 0.46f, birdY - r * 0.37f, r * 0.06f, birdEyePaint)

        // Mỏ trên và mỏ dưới tách rời, hở ra theo nhịp vỗ cánh
        val mouthOpen = (1f - birdFlap) * r * 0.10f
        val beakTop = beakTopPath
        beakTop.rewind()
        beakTop.moveTo(birdX + r * 0.72f, birdY - r * 0.04f)
        beakTop.lineTo(birdX + r * 1.42f, birdY + r * 0.14f)
        beakTop.lineTo(birdX + r * 0.74f, birdY + r * 0.16f)
        beakTop.close()
        c.drawPath(beakTop, birdBeakPaint)
        c.drawPath(beakTop, birdOutlinePaint)

        val beakBottom = beakBottomPath
        beakBottom.rewind()
        beakBottom.moveTo(birdX + r * 0.74f, birdY + r * 0.18f)
        beakBottom.lineTo(birdX + r * 1.30f, birdY + r * 0.26f + mouthOpen)
        beakBottom.lineTo(birdX + r * 0.74f, birdY + r * 0.46f)
        beakBottom.close()
        c.drawPath(beakBottom, birdBeakPaint)

        // Má hồng cho dễ thương
        c.drawCircle(birdX + r * 0.26f, birdY + r * 0.30f, r * 0.14f, birdBlushPaint)

        c.restore()
    }

    // ---------- HUD ----------
    private fun drawHud(c: Canvas) {
        bigPaint.textSize = 84f * d
        c.drawText(score.toString(), screenW / 2f, screenH * 0.14f, bigPaint)

        // Đồng xu đã ăn
        smallPaint.textAlign = Paint.Align.LEFT
        coinPaint.color = Color.rgb(255, 205, 45)
        c.drawCircle(30f * d, screenH * 0.055f, 13f * d, coinPaint)
        c.drawText("x$coins", 52f * d, screenH * 0.075f, smallPaint)

        // Điểm cao
        smallPaint.textAlign = Paint.Align.RIGHT
        c.drawText("BEST $best", screenW - 26f * d, screenH * 0.075f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER
    }

    // ---------- Menu ----------

    private fun drawMenu(c: Canvas) {
        // Tiêu đề
        titlePaint.textSize = 76f * d
        c.drawText("FLAPPY", screenW / 2f, screenH * 0.15f, titlePaint)
        titlePaint.textSize = 60f * d
        c.drawText("BIRD", screenW / 2f, screenH * 0.235f, titlePaint)

        // Nhãn độ khó
        smallPaint.textSize = 30f * d
        c.drawText("ĐỘ KHÓ", screenW / 2f, screenH * 0.375f, smallPaint)

        for (i in 0 until 3) {
            val r = diffRect(i)
            val df = Difficulty.values()[i]
            val selected = df == difficulty
            buttonPaint.color = if (selected) Color.rgb(255, 196, 60) else Color.argb(170, 255, 255, 255)
            c.drawRoundRect(r, 14f * d, 14f * d, buttonPaint)
            c.drawRoundRect(r, 14f * d, 14f * d, buttonBorderPaint)
            buttonTextPaint.textSize = 28f * d
            c.drawText(df.label, r.centerX(), r.centerY() + 10f * d, buttonTextPaint)
        }

        // Ba nút bật/tắt
        val labels = arrayOf(
            if (soundEnabled) "ÂM: BẬT" else "ÂM: TẮT",
            if (vibrateEnabled) "RUNG: BẬT" else "RUNG: TẮT",
            if (theme == Theme.DAY) "NGÀY" else "ĐÊM"
        )
        for (i in 0 until 3) {
            val r = toggleRect(i)
            buttonPaint.color = Color.argb(180, 255, 255, 255)
            c.drawRoundRect(r, 14f * d, 14f * d, buttonPaint)
            c.drawRoundRect(r, 14f * d, 14f * d, buttonBorderPaint)
            buttonTextPaint.textSize = 22f * d
            c.drawText(labels[i], r.centerX(), r.centerY() + 8f * d, buttonTextPaint)
        }

        // Nút bắt đầu
        val sr = startRect()
        buttonPaint.color = Color.rgb(255, 196, 60)
        c.drawRoundRect(sr, 24f * d, 24f * d, buttonPaint)
        c.drawRoundRect(sr, 24f * d, 24f * d, buttonBorderPaint)
        buttonTextPaint.textSize = 44f * d
        c.drawText("CHƠI", sr.centerX(), sr.centerY() + 16f * d, buttonTextPaint)

        // Nút cửa hàng skin
        val shr = shopBtnRect()
        buttonPaint.color = Color.argb(215, 255, 255, 255)
        c.drawRoundRect(shr, 20f * d, 20f * d, buttonPaint)
        c.drawRoundRect(shr, 20f * d, 20f * d, buttonBorderPaint)
        buttonTextPaint.textSize = 30f * d
        c.drawText("CỬA HÀNG", shr.centerX(), shr.centerY() + 10f * d, buttonTextPaint)

        // Chim mini xem trước skin đang dùng, đặt bên trái nút cửa hàng
        val pr = 17f * d
        val px = shr.left - pr - 12f * d
        val py = shr.centerY()
        birdBodyPaint.color = currentSkin.body
        birdWingPaint.color = currentSkin.wing
        c.drawCircle(px, py, pr, birdBodyPaint)
        c.drawCircle(px, py, pr, birdOutlinePaint)
        c.drawCircle(px + pr * 0.42f, py - pr * 0.32f, pr * 0.32f, birdEyePaint)
        c.drawCircle(px + pr * 0.50f, py - pr * 0.30f, pr * 0.15f, birdPupilPaint)
        val mb = miniBeakPath
        mb.rewind()
        mb.moveTo(px + pr * 0.82f, py + pr * 0.02f)
        mb.lineTo(px + pr * 1.42f, py + pr * 0.20f)
        mb.lineTo(px + pr * 0.82f, py + pr * 0.40f)
        mb.close()
        c.drawPath(mb, birdBeakPaint)

        // Thành tích
        smallPaint.textSize = 27f * d
        c.drawText("KỶ LỤC: $best", screenW / 2f, screenH * 0.905f, smallPaint)
        smallPaint.textSize = 22f * d
        c.drawText("Chạm để vỗ cánh khi đang chơi", screenW / 2f, screenH * 0.955f, smallPaint)
    }

    // ---------- Cửa hàng skin ----------
    private fun drawShop(c: Canvas) {
        // Lớp phủ tối
        overlayPaint.color = Color.argb(150, 0, 0, 0)
        c.drawRect(0f, 0f, screenW, screenH, overlayPaint)

        // Tiêu đề
        titlePaint.textSize = 58f * d
        c.drawText("CỬA HÀNG", screenW / 2f, screenH * 0.11f, titlePaint)

        // Ví xu
        smallPaint.textAlign = Paint.Align.LEFT
        smallPaint.textSize = 32f * d
        coinPaint.color = Color.rgb(255, 205, 45)
        c.drawCircle(34f * d, screenH * 0.165f, 14f * d, coinPaint)
        c.drawText("VÍ: $wallet xu", 58f * d, screenH * 0.178f, smallPaint)
        smallPaint.textAlign = Paint.Align.CENTER

        // Danh sách skin
        for (i in Skin.values().indices) {
            val skin = Skin.values()[i]
            val r = skinRect(i)
            val owned = ownedSkins.contains(skin)
            val selected = currentSkin == skin

            // Nền ô
            buttonPaint.color = if (selected) Color.argb(235, 255, 214, 90)
                                else Color.argb(215, 250, 250, 250)
            c.drawRoundRect(r, 16f * d, 16f * d, buttonPaint)
            c.drawRoundRect(r, 16f * d, 16f * d, buttonBorderPaint)

            // Chim mini xem trước
            birdBodyPaint.color = skin.body
            birdWingPaint.color = skin.wing
            c.drawCircle(r.left + 34f * d, r.centerY(), 20f * d, birdBodyPaint)
            c.drawCircle(r.left + 34f * d, r.centerY(), 20f * d, birdOutlinePaint)
            c.drawCircle(r.left + 42f * d, r.centerY() - 6f * d, 7f * d, birdEyePaint)
            c.drawCircle(r.left + 44f * d, r.centerY() - 6f * d, 3f * d, birdPupilPaint)

            // Tên skin
            smallPaint.textAlign = Paint.Align.LEFT
            smallPaint.textSize = 22f * d
            smallPaint.color = Color.rgb(50, 50, 50)
            c.drawText(skin.label, r.left + 62f * d, r.centerY() - 4f * d, smallPaint)

            // Trạng thái
            smallPaint.textSize = 19f * d
            smallPaint.color = if (selected) Color.rgb(30, 110, 30) else Color.rgb(110, 110, 110)
            val status = when {
                selected -> "ĐANG DÙNG"
                owned -> "CHỌN"
                else -> "${skin.price} xu"
            }
            c.drawText(status, r.left + 62f * d, r.centerY() + 22f * d, smallPaint)
            smallPaint.color = Color.WHITE
            smallPaint.textAlign = Paint.Align.CENTER
        }

        // Nút quay lại
        val br = shopBackRect()
        buttonPaint.color = Color.argb(220, 245, 245, 245)
        c.drawRoundRect(br, 20f * d, 20f * d, buttonPaint)
        c.drawRoundRect(br, 20f * d, 20f * d, buttonBorderPaint)
        buttonTextPaint.textSize = 30f * d
        c.drawText("QUAY LẠI", br.centerX(), br.centerY() + 10f * d, buttonTextPaint)
    }

    // ---------- Màn hình kết thúc ----------
    private fun drawGameOver(c: Canvas) {
        overlayPaint.color = Color.argb(125, 0, 0, 0)
        c.drawRect(0f, 0f, screenW, screenH, overlayPaint)

        val p = panelRect()
        c.drawRoundRect(p, 26f * d, 26f * d, panelPaint)
        c.drawRoundRect(p, 26f * d, 26f * d, panelBorderPaint)

        panelTitlePaint.textSize = 44f * d
        c.drawText("KẾT THÚC", p.centerX(), p.top + 58f * d, panelTitlePaint)

        // Huy chương theo điểm
        val medal = when {
            score >= 40 -> 3
            score >= 20 -> 2
            score >= 10 -> 1
            else -> 0
        }
        val medalColors = intArrayOf(
            Color.rgb(190, 190, 190), Color.rgb(205, 170, 90), Color.rgb(255, 215, 60)
        )
        val medalNames = arrayOf("CHƯA CÓ", "ĐỒNG", "BẠC", "VÀNG")
        val mx = p.left + p.width() * 0.25f
        val my = p.top + p.height() * 0.45f
        val mr = 38f * d

        medalPaint.style = Paint.Style.FILL
        medalPaint.color = if (medal > 0) medalColors[medal - 1] else Color.rgb(175, 175, 175)
        c.drawCircle(mx, my, mr, medalPaint)
        medalPaint.style = Paint.Style.STROKE
        medalPaint.strokeWidth = 6f * d
        medalPaint.color = Color.rgb(120, 100, 50)
        c.drawCircle(mx, my, mr, medalPaint)
        medalPaint.style = Paint.Style.FILL

        panelSmallPaint.textSize = 24f * d
        c.drawText(medalNames[medal], mx, my + mr + 28f * d, panelSmallPaint)

        // Điểm và kỷ lục
        panelSmallPaint.textAlign = Paint.Align.LEFT
        panelSmallPaint.textSize = 26f * d
        val tx = p.left + p.width() * 0.52f
        c.drawText("ĐIỂM", tx, p.top + p.height() * 0.28f, panelSmallPaint)

        panelTextPaint.textAlign = Paint.Align.LEFT
        panelTextPaint.textSize = 46f * d
        c.drawText(score.toString(), tx, p.top + p.height() * 0.39f, panelTextPaint)

        panelSmallPaint.textSize = 26f * d
        c.drawText("KỶ LỤC", tx, p.top + p.height() * 0.52f, panelSmallPaint)

        panelTextPaint.textSize = 38f * d
        c.drawText(best.toString(), tx, p.top + p.height() * 0.62f, panelTextPaint)

        panelSmallPaint.textSize = 25f * d
        c.drawText("Xu: $coins", tx, p.top + p.height() * 0.72f, panelSmallPaint)

        panelSmallPaint.textAlign = Paint.Align.CENTER
        panelTextPaint.textAlign = Paint.Align.CENTER

        // Nút chơi lại
        val rr = retryRect()
        buttonPaint.color = Color.rgb(255, 196, 60)
        c.drawRoundRect(rr, 18f * d, 18f * d, buttonPaint)
        c.drawRoundRect(rr, 18f * d, 18f * d, buttonBorderPaint)
        buttonTextPaint.textSize = 28f * d
        c.drawText("CHƠI LẠI", rr.centerX(), rr.centerY() + 10f * d, buttonTextPaint)

        // Nút về menu
        val mbr = menuBtnRect()
        buttonPaint.color = Color.argb(215, 245, 245, 245)
        c.drawRoundRect(mbr, 18f * d, 18f * d, buttonPaint)
        c.drawRoundRect(mbr, 18f * d, 18f * d, buttonBorderPaint)
        buttonTextPaint.textSize = 28f * d
        c.drawText("MENU", mbr.centerX(), mbr.centerY() + 10f * d, buttonTextPaint)
    }

    /** Dòng credit góc phải màn hình */
    private fun drawCredit(c: Canvas) {
        creditPaint.textSize = 22f * d
        val x = screenW - 16f * d
        val y = groundY - 14f * d
        c.drawText("cre: minhnguyen2009", x, y, creditPaint)
    }
}