package com.example.prog7313

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class BubbleGameActivity : AppCompatActivity() {

    private var score = 0
    private val maxScore = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.actvity_bubble_game)

        // Optional: Reference the GameView if needed
        val gameView = findViewById<GameView>(R.id.gameView)

        // Optional: Set a listener from GameView when a bubble is popped
        gameView.setOnBubblePoppedListener {
            score++
            if (score == maxScore) {
                showPerfectScoreMessage()
            }
        }
    }

    private fun showPerfectScoreMessage() {
        AlertDialog.Builder(this)
            .setTitle("🎉 Perfect Score!")
            .setMessage("You scored 10/10 bubbles! You’ve earned the Bubble Champ badge!")
            .setPositiveButton("Awesome") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
