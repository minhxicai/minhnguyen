package com.example.flappybird

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

/**
 * Game Flappy Bird vẽ bằng Canvas.
 * 3 trạng thái: MENU (sảnh) - PLAYING - GAME_OVER
 */
class GameView(context: Context) : View(context) {

    private enum class GameState { MENU, PLAYING, GAME_OVER }
    private var state = GameState.MENU

    private val d = resources.displayMetrics.density
    private var screenW = 0f
    private var screenH = 0f
    private var groundY = 0f
    private val groundHeight = 130f * d

    // Chim
    private var birdX = 0f
    private var birdY = 0f
    private var birdVy = 0f
    private var birdAngle = 0f
    private val birdR = 30f * d
    private val gravity = 0.55f * d
    private val jumpPower = -11.5f * d

    // Ống
    private class Pipe(var x: Float, var gapY: Float) { var scored = false }
    private val pipes = mutableListOf<Pipe>()
    private val pipeW = 130f * d
    private val gapH = 430f * d
    private val pipeSpacing = 560f * d
    private val pipeSpeed = 5.5f * d

    private var score = 0
    private var best = 0
    private var lastFrameTime = 0L

    // ===== Bút vẽ =====
    private val skyPaint = Paint().apply { color = Color.rgb(112, 197, 250) }
    private val groundPaint = Paint().apply { color = Color.rgb(222, 202, 141) }
    private val groundTopPaint = Paint().apply { color = Color.rgb(120, 190, 80) }
    private val birdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 214, 0) }
    private val bellyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 240, 150) }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val beakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 130, 0) }
    private val wingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 190, 0) }
    private val pipePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(70, 180, 70) }
    private val pipeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(40, 120, 40); style = Paint.Style.STROKE; strokeWidth = 6f * d
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; textSize = 64f * d
        setShadowLayer(8f, 0f, 4f, Color.BLACK)
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; textSize = 32f * d
        setShadowLayer(6f, 0f, 3f, Color.BLACK)
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; textSize = 80f * d
        setShadowLayer(8f, 0f, 4f, Color.BLACK)
    }
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 152, 0) }
    private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD; textSize = 44f * d
    }

    init {
        // Bật vòng lặp vẽ liên tục
        isFocusable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenW = w.toFloat()
        screenH = h.toFloat()
        groundY = screenH - groundHeight
        birdX = screenW * 0.28f
        resetBird()
    }

    private fun resetBird() {
        birdY = screenH * 0.4f
        birdVy = 0f
        birdAngle = 0f
    }

    private fun startGame() {
        score = 0
        pipes.clear()
        resetBird()
        // Tạo sẵn vài ống phía trước
        var x = screenW + 200f * d
        repeat(3) {
            spawnPipe(x)
            x += pipeSpacing
        }
        state = GameState.PLAYING
    }

    private fun spawnPipe(x: Float) {
        // Khoảng trống ngẫu nhiên trong vùng cho phép
        val margin = 220f * d
        val minGap = margin + gapH / 2f
        val maxGap = groundY - margin - gapH / 2f
        val gapY = Random.nextFloat() * (maxGap - minGap) + minGap
        pipes.add(Pipe(x, gapY))
    }

    private fun gameOver() {
        state = GameState.GAME_OVER
        if (score > best) best = score
    }

    private fun jump() {
        birdVy = jumpPower
        birdAngle = -25f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            when (state) {
                GameState.MENU -> startGame()
                GameState.PLAYING -> jump()
                GameState.GAME_OVER -> {
                    // Chỉ cho chơi lại khi chạm vào nút
                    val cx = screenW / 2f
                    val cy = screenH / 2f + 120f * d
                    val dx = event.x - cx
                    val dy = event.y - cy
                    if (dx * dx + dy * dy < (140f * d) * (140f * d)) {
                        state = GameState.MENU
                    }
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val now = System.currentTimeMillis()
        val dt = if (lastFrameTime == 0L) 16f else (now - lastFrameTime).coerceAtMost(40L).toFloat()
        lastFrameTime = now

        if (state == GameState.PLAYING) {
            update(dt)
        } else if (state == GameState.MENU) {
            // Chim nhấp nhô ở sảnh
            birdY = screenH * 0.4f + kotlin.math.sin(now / 300.0).toFloat() * 20f * d
        }

        drawBackground(canvas)
        drawPipes(canvas)
        drawBird(canvas)
        drawGround(canvas)

        when (state) {
            GameState.MENU -> drawMenu(canvas)
            GameState.PLAYING -> drawScore(canvas)
            GameState.GAME_OVER -> drawGameOver(canvas)
        }

        // Vẽ lại liên tục
        invalidate()
    }

    private fun update(dt: Float) {
        val step = dt / 16f

        // Cập nhật chim
        birdVy += gravity * step
        birdY += birdVy * step
        birdAngle = (birdVy / (12f * d) * 30f).coerceIn(-35f, 90f)

        // Cập nhật ống
        val move = pipeSpeed * step
        for (p in pipes) p.x -= move

        // Di chuyển ống đã qua sang cuối
        if (pipes.isNotEmpty() && pipes.first().x + pipeW < -50f) {
            val first = pipes.removeAt(0)
            val lastX = pipes.lastOrNull()?.x ?: (screenW)
            first.x = lastX + pipeSpacing
            // Tạo lại khoảng trống mới
            val margin = 220f * d
            val minGap = margin + gapH / 2f
            val maxGap = groundY - margin - gapH / 2f
            first.gapY = Random.nextFloat() * (maxGap - minGap) + minGap
            first.scored = false
            pipes.add(first)
        }

        // Tính điểm + va chạm
        for (p in pipes) {
            // Cộng điểm khi chim vượt qua tâm ống
            if (!p.scored && p.x + pipeW < birdX) {
                p.scored = true
                score++
            }
            if (collides(p)) {
                gameOver()
                return
            }
        }

        // Va chạm mặt đất hoặc trần
        if (birdY + birdR >= groundY) {
            birdY = groundY - birdR
            gameOver()
        }
        if (birdY - birdR < 0f) {
            birdY = birdR
            birdVy = 0f
        }
    }

    private fun collides(p: Pipe): Boolean {
        // Kiểm tra chạm cột trên / dưới
        val withinX = birdX + birdR > p.x && birdX - birdR < p.x + pipeW
        if (!withinX) return false
        val gapTop = p.gapY - gapH / 2f
        val gapBottom = p.gapY + gapH / 2f
        // Chạm cột trên nếu chim cao hơn đáy cột trên
        if (birdY - birdR < gapTop) return true
        // Chạm cột dưới nếu chim thấp hơn đỉnh cột dưới
        if (birdY + birdR > gapBottom) return true
        return false
    }

    // ===== Các hàm vẽ =====
    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(skyPaint.color)
        // Mây đơn giản
        val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawCircle(screenW * 0.2f, screenH * 0.15f, 55f * d, cloudPaint)
        canvas.drawCircle(screenW * 0.32f, screenH * 0.13f, 70f * d, cloudPaint)
        canvas.drawCircle(screenW * 0.45f, screenH * 0.16f, 50f * d, cloudPaint)
    }

    private fun drawPipes(canvas: Canvas) {
        for (p in pipes) {
            val gapTop = p.gapY - gapH / 2f
            val gapBottom = p.gapY + gapH / 2f
            // Cột trên
            canvas.drawRect(p.x, 0f, p.x + pipeW, gapTop, pipePaint)
            canvas.drawRect(p.x, 0f, p.x + pipeW, gapTop, pipeBorderPaint)
            // Cột dưới
            canvas.drawRect(p.x, gapBottom, p.x + pipeW, groundY, pipePaint)
            canvas.drawRect(p.x, gapBottom, p.x + pipeW, groundY, pipeBorderPaint)
            // Nắp ống
            val capH = 40f * d
            canvas.drawRect(p.x - 8f * d, gapTop - capH, p.x + pipeW + 8f * d, gapTop, pipePaint)
            canvas.drawRect(p.x - 8f * d, gapTop - capH, p.x + pipeW + 8f * d, gapTop, pipeBorderPaint)
            canvas.drawRect(p.x - 8f * d, gapBottom, p.x + pipeW + 8f * d, gapBottom + capH, pipePaint)
            canvas.drawRect(p.x - 8f * d, gapBottom, p.x + pipeW + 8f * d, gapBottom + capH, pipeBorderPaint)
        }
    }

    private fun drawBird(canvas: Canvas) {
        canvas.save()
        canvas.rotate(birdAngle, birdX, birdY)
        // Thân
        canvas.drawCircle(birdX, birdY, birdR, birdPaint)
        // Bụng
        canvas.drawCircle(birdX - birdR * 0.15f, birdY + birdR * 0.35f, birdR * 0.6f, bellyPaint)
        // Mắt
        canvas.drawCircle(birdX + birdR * 0.35f, birdY - birdR * 0.35f, birdR * 0.32f, eyePaint)
        canvas.drawCircle(birdX + birdR * 0.45f, birdY - birdR * 0.35f, birdR * 0.14f, pupilPaint)
        // Mỏ
        val beak = android.graphics.Path().apply {
            moveTo(birdX + birdR * 0.7f, birdY - birdR * 0.05f)
            lineTo(birdX + birdR * 1.35f, birdY + birdR * 0.1f)
            lineTo(birdX + birdR * 0.7f, birdY + birdR * 0.35f)
            close()
        }
        canvas.drawPath(beak, beakPaint)
        // Cánh
        canvas.drawOval(birdX - birdR * 0.7f, birdY - birdR * 0.1f, birdX + birdR * 0.2f, birdY + birdR * 0.5f, wingPaint)
        canvas.restore()
    }

    private fun drawGround(canvas: Canvas) {
        canvas.drawRect(0f, groundY, screenW, screenH, groundPaint)
        canvas.drawRect(0f, groundY, screenW, groundY + 18f * d, groundTopPaint)
    }

    private fun drawMenu(canvas: Canvas) {
        // Tiêu đề
        canvas.drawText("FLAPPY BIRD", screenW / 2f, screenH * 0.22f, titlePaint)
        // Hướng dẫn
        canvas.drawText("Chạm để bay!", screenW / 2f, screenH * 0.62f, subPaint)
        // Nút BẮT ĐẦU
        val cx = screenW / 2f
        val cy = screenH * 0.72f
        val rx = 220f * d
        val ry = 80f * d
        val rect = android.graphics.RectF(cx - rx, cy - ry, cx + rx, cy + ry)
        canvas.drawRoundRect(rect, 40f * d, 40f * d, btnPaint)
        canvas.drawText("BẮT ĐẦU", cx, cy + 16f * d, btnTextPaint)
    }

    private fun drawScore(canvas: Canvas) {
        canvas.drawText(score.toString(), screenW / 2f, screenH * 0.15f, scorePaint)
    }

    private fun drawGameOver(canvas: Canvas) {
        // Nền mờ
        val overlay = Paint().apply { color = Color.argb(150, 0, 0, 0) }
        canvas.drawRect(0f, 0f, screenW, screenH, overlay)

        canvas.drawText("GAME OVER", screenW / 2f, screenH * 0.32f, titlePaint)
        canvas.drawText("Điểm: $score", screenW / 2f, screenH * 0.44f, subPaint)
        canvas.drawText("Kỷ lục: $best", screenW / 2f, screenH * 0.50f, subPaint)

        // Nút CHƠI LẠI
        val cx = screenW / 2f
        val cy = screenH / 2f + 120f * d
        val rx = 220f * d
        val ry = 80f * d
        val rect = android.graphics.RectF(cx - rx, cy - ry, cx + rx, cy + ry)
        canvas.drawRoundRect(rect, 40f * d, 40f * d, btnPaint)
        canvas.drawText("CHƠI LẠI", cx, cy + 16f * d, btnTextPaint)
    }
}
