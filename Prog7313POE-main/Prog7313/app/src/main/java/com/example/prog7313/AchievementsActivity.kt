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

class AchievementsActivity : AppCompatActivity() {

    private lateinit var achievementsContainer: LinearLayout
    private lateinit var database: DatabaseReference
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        achievementsContainer = findViewById(R.id.achievementsContainer)
        database = FirebaseDatabase.getInstance().reference

        loadAchievements()
    }

    private fun loadAchievements() {
        if (uid.isEmpty()) return

        database.child("achievements").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    achievementsContainer.removeAllViews()

                    val achievements = listOf(
                        Triple("first_login", "First Login", "You've successfully logged in!"),
                        Triple("entered_salary", "Entered Salary", "You entered your salary."),
                        Triple("added_expense", "Added First Expense", "You added your first expense."),
                        Triple("set_monthly_goals", "Set Monthly Goals", "You set monthly spending goals."),
                        Triple("set_category_goals", "Set Category Goals", "You set category-specific goals.")
                    )

                    for ((key, title, description) in achievements) {
                        val earned = snapshot.child(key).getValue(Boolean::class.java) ?: false
                        val timestamp = snapshot.child("${key}_timestamp").getValue(Long::class.java)
                        addAchievementView(title, description, earned, timestamp)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun addAchievementView(
        title: String,
        description: String,
        earned: Boolean,
        timestamp: Long?
    ) {
        val view = layoutInflater.inflate(R.layout.item_achievement, achievementsContainer, false)

        val icon = view.findViewById<ImageView>(R.id.achievementIcon)
        val titleView = view.findViewById<TextView>(R.id.achievementTitle)
        val descriptionView = view.findViewById<TextView>(R.id.achievementDescription)
        val timestampView = view.findViewById<TextView>(R.id.achievementTimestamp)

        icon.setImageResource(if (earned) R.drawable.ic_trophy else R.drawable.ic_trophy_gray)
        titleView.text = title
        descriptionView.text = description

        if (earned && timestamp != null) {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            val formattedDate = sdf.format(Date(timestamp))
            timestampView.text = "Unlocked on: $formattedDate"
            timestampView.visibility = View.VISIBLE
        } else {
            timestampView.visibility = View.GONE
        }

        achievementsContainer.addView(view)
    }
}
