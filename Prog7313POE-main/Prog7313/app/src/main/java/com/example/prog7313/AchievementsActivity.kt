package com.example.prog7313

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

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


class AchievementsActivity : AppCompatActivity() {

    // Layout container that will hold dynamically added achievement views
    private lateinit var achievementsContainer: LinearLayout

    // Reference to the Firebase Realtime Database
    private lateinit var database: DatabaseReference

    // Get current user's UID from Firebase Auth
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        // Initialize views and database reference
        achievementsContainer = findViewById(R.id.achievementsContainer)
        database = FirebaseDatabase.getInstance().reference

        // Load the achievements from Firebase
        loadAchievements()
    }

    /**
     * Fetches the user's achievements from Firebase and populates the layout.
     */
    private fun loadAchievements() {
        // Skip if UID is not available
        if (uid.isEmpty()) return

        // Read user-specific achievements from the database
        database.child("achievements").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Clear the container before adding new views
                    achievementsContainer.removeAllViews()

                    // List of achievement keys, titles, and descriptions
                    val achievements = listOf(
                        Triple("first_login", "First Login", "You've successfully logged in!"),
                        Triple("entered_salary", "Entered Salary", "You entered your salary."),
                        Triple("added_expense", "Added First Expense", "You added your first expense."),
                        Triple("set_monthly_goals", "Set Monthly Goals", "You set monthly spending goals."),
                        Triple("set_category_goals", "Set Category Goals", "You set category-specific goals.")
                    )

                    // Loop through each achievement and display it
                    for ((key, title, description) in achievements) {
                        val earned = snapshot.child(key).getValue(Boolean::class.java) ?: false
                        val timestamp = snapshot.child("${key}_timestamp").getValue(Long::class.java)
                        addAchievementView(title, description, earned, timestamp)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Optional: handle database error (e.g., logging)
                }
            })
    }

    /**
     * Adds a single achievement item to the UI.
     *
     * @param title Title of the achievement
     * @param description Description of what the achievement represents
     * @param earned Whether the user has earned this achievement
     * @param timestamp When the achievement was earned (if applicable)
     */
    private fun addAchievementView(
        title: String,
        description: String,
        earned: Boolean,
        timestamp: Long?
    ) {
        // Inflate the custom layout for each achievement
        val view = layoutInflater.inflate(R.layout.item_achievement, achievementsContainer, false)

        // Find views within the inflated layout
        val icon = view.findViewById<ImageView>(R.id.achievementIcon)
        val titleView = view.findViewById<TextView>(R.id.achievementTitle)
        val descriptionView = view.findViewById<TextView>(R.id.achievementDescription)
        val timestampView = view.findViewById<TextView>(R.id.achievementTimestamp)

        // Set icon based on whether the achievement is earned
        icon.setImageResource(if (earned) R.drawable.ic_trophy else R.drawable.ic_trophy_gray)

        // Set title and description text
        titleView.text = title
        descriptionView.text = description

        // Display timestamp if the achievement is earned
        if (earned && timestamp != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val formattedDate = sdf.format(Date(timestamp))
            timestampView.text = "Unlocked on: $formattedDate"
            timestampView.visibility = View.VISIBLE
        } else {
            timestampView.visibility = View.GONE
        }

        // Add this view to the achievements container
        achievementsContainer.addView(view)
    }
}
