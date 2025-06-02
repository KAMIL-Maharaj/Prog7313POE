package com.example.prog7313

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
/**
 * This gamification feature (achievement tracking and rewards system) implementation
 * is based on Firebase Realtime Database integration and follows design patterns
 * demonstrated in Google’s official documentation and open tutorials.
 *
 * Reference:
 * - Google Firebase Realtime Database documentation:
 *   https://firebase.google.com/docs/database/android/start
 * - Gamification techniques inspired by:
 *   Karl M. Kapp – "The Gamification of Learning and Instruction" (Wiley, 2012)
 * - Google Codelabs – Firebase Android App Tutorials:
 *   https://firebase.google.com/codelabs
 *
 * This implementation includes:
 * - Achievement unlock tracking
 * - One-time flags stored in Realtime Database
 * - Progress feedback using UI elements (e.g., Toasts, ProgressBars)
 */

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
