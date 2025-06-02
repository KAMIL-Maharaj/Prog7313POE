package com.example.prog7313

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val bubbles = mutableListOf<Bubble>()
    private val paint = Paint()
    private val handler = Handler(Looper.getMainLooper())
    private val updateDelay = 30L

    private var score = 0
    private val sharedPrefs = context.getSharedPreferences("BubbleGamePrefs", Context.MODE_PRIVATE)
    private var bubblePoppedListener: (() -> Unit)? = null

    fun setOnBubblePoppedListener(listener: () -> Unit) {
        bubblePoppedListener = listener
    }

    init {
        post {
            generateBubbles()
        }
        score = sharedPrefs.getInt("lastScore", 0)
        startGameLoop()
    }

    private fun generateBubbles() {
        bubbles.clear()
        val xMin = 100
        val xMax = width - 100
        for (i in 1..10) {
            val radius = 60f
            val x = (xMin..xMax).random().toFloat()
            val y = height + (0..300).random()
            bubbles.add(Bubble(x, y.toFloat(), radius))
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw bubbles
        paint.color = Color.BLUE
        for (bubble in bubbles) {
            canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
        }

        // Draw score
        paint.color = Color.BLACK
        paint.textSize = 50f
        canvas.drawText("Score: $score", 50f, 100f, paint)
    }

    private fun startGameLoop() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateBubbles()
                invalidate()
                handler.postDelayed(this, updateDelay)
            }
        }, updateDelay)
    }

    private fun updateBubbles() {
        for (bubble in bubbles) {
            bubble.y -= 5f // move up
        }

        // Remove off-screen bubbles
        bubbles.removeAll { it.y + it.radius < 0 }

        // Regenerate if empty
        if (bubbles.isEmpty()) {
            sharedPrefs.edit().putInt("lastScore", score).apply()
            generateBubbles()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            val iterator = bubbles.iterator()
            while (iterator.hasNext()) {
                val bubble = iterator.next()
                if (bubble.contains(x, y)) {
                    iterator.remove()
                    score++
                    break
                }
            }
            invalidate()
        }
        return true
    }
}
