package com.example.prog7313

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class MonthlySummaryActivity : AppCompatActivity() {

    private lateinit var salaryTextView: TextView
    private lateinit var salaryProgressBar: ProgressBar
    private lateinit var salaryRemainingTextView: TextView

    private lateinit var totalSpentTextView: TextView
    private lateinit var minGoalTextView: TextView
    private lateinit var maxGoalTextView: TextView
    private lateinit var overallProgressBar: ProgressBar
    private lateinit var categoryContainer: LinearLayout

    private var salary = 0.0
    private var minGoal = 0.0
    private var maxGoal = 0.0
    private var totalSpent = 0.0

    private val categorySpendMap = mutableMapOf<String, Double>()
    private val categoryGoalMap = mutableMapOf<String, Pair<Double, Double>>() // min, max

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_summary)

        salaryTextView = findViewById(R.id.salaryTextView)
        salaryProgressBar = findViewById(R.id.salaryProgressBar)
        salaryRemainingTextView = findViewById(R.id.salaryRemainingTextView)

        totalSpentTextView = findViewById(R.id.totalSpentTextView)
        minGoalTextView = findViewById(R.id.minGoalTextView)
        maxGoalTextView = findViewById(R.id.maxGoalTextView)
        overallProgressBar = findViewById(R.id.spendingProgressBar)
        categoryContainer = findViewById(R.id.categoryContainer)

        loadSalary()
    }

    private fun loadSalary() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().reference

        database.child("users").child(uid).child("salary")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    salary = snapshot.getValue(String::class.java)?.toDoubleOrNull() ?: 0.0
                    salaryTextView.text = "Salary: R%.2f".format(salary)
                    loadGoals()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MonthlySummaryActivity, "Failed to load salary", Toast.LENGTH_SHORT).show()
                    loadGoals() // Proceed to load goals anyway
                }
            })
    }

    private fun loadGoals() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val goalRef = FirebaseDatabase.getInstance().reference.child("goals").child(uid)

        goalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                minGoal = snapshot.child("minGoal").getValue(Double::class.java) ?: 0.0
                maxGoal = snapshot.child("maxGoal").getValue(Double::class.java) ?: 0.0

                minGoalTextView.text = "Minimum Goal: R%.2f".format(minGoal)
                maxGoalTextView.text = "Maximum Goal: R%.2f".format(maxGoal)

                // Load category goals
                val categoryGoalsSnapshot = snapshot.child("categories")
                for (categorySnap in categoryGoalsSnapshot.children) {
                    val name = categorySnap.key ?: continue
                    val min = categorySnap.child("minGoal").getValue(Double::class.java) ?: 0.0
                    val max = categorySnap.child("maxGoal").getValue(Double::class.java) ?: 0.0
                    categoryGoalMap[name] = Pair(min, max)
                }

                loadMonthlyExpenses()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MonthlySummaryActivity, "Failed to load goals", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadMonthlyExpenses() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val expensesRef = FirebaseDatabase.getInstance().reference.child("expenses").child(uid)
        val currentMonth = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(Date())

        expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                totalSpent = 0.0
                categorySpendMap.clear()

                for (expenseSnapshot in snapshot.children) {
                    val dateStr = expenseSnapshot.child("date").getValue(String::class.java) ?: continue
                    val amount = expenseSnapshot.child("amount").getValue(Double::class.java) ?: continue
                    val category = expenseSnapshot.child("category").getValue(String::class.java) ?: "Uncategorized"

                    if (isInCurrentMonth(dateStr, currentMonth)) {
                        totalSpent += amount
                        categorySpendMap[category] = categorySpendMap.getOrDefault(category, 0.0) + amount
                    }
                }

                totalSpentTextView.text = "Total Spent: R%.2f".format(totalSpent)
                updateProgressBar()
                updateSalaryProgress()
                displayCategoryProgress()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MonthlySummaryActivity, "Failed to load expenses", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun isInCurrentMonth(dateStr: String, currentMonth: String): Boolean {
        return try {
            val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            val entryMonth = SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(parsedDate!!)
            entryMonth == currentMonth
        } catch (e: Exception) {
            false
        }
    }

    private fun updateProgressBar() {
        if (maxGoal > 0) {
            val percent = ((totalSpent / maxGoal) * 100).coerceAtMost(100.0)
            overallProgressBar.progress = percent.toInt()
        }
    }

    private fun updateSalaryProgress() {
        if (salary > 0) {
            val percentSpent = ((totalSpent / salary) * 100).coerceAtMost(100.0)
            salaryProgressBar.progress = percentSpent.toInt()

            val remaining = salary - totalSpent
            salaryRemainingTextView.text = if (remaining >= 0) {
                "Remaining Salary: R%.2f".format(remaining)
            } else {
                "Overspent by: R%.2f".format(-remaining)
            }
        } else {
            salaryProgressBar.progress = 0
            salaryRemainingTextView.text = "Salary not set"
        }
    }

    private fun displayCategoryProgress() {
        categoryContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        for ((category, spent) in categorySpendMap) {
            val view = inflater.inflate(R.layout.item_category_progress, categoryContainer, false)

            val nameText = view.findViewById<TextView>(R.id.categoryNameTextView)
            val progressBar = view.findViewById<ProgressBar>(R.id.categoryProgressBar)
            val goalText = view.findViewById<TextView>(R.id.categoryGoalTextView)
            val spentText = view.findViewById<TextView>(R.id.categorySpentTextView)

            nameText.text = category
            spentText.text = "Spent: R%.2f".format(spent)

            val (min, max) = categoryGoalMap[category] ?: Pair(0.0, 0.0)
            goalText.text = "Goal: R%.2f - R%.2f".format(min, max)

            if (max > 0) {
                progressBar.progress = ((spent / max) * 100).coerceAtMost(100.0).toInt()
            }

            categoryContainer.addView(view)
        }
    }
}
