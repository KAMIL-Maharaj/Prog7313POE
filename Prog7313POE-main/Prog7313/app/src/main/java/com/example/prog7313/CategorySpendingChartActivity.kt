package com.example.prog7313

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class CategorySpendingChartActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var barChart: BarChart

    // Date formatting
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Default start date = 1st of current month, end date = today
    private var startDate: Date = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.time
    private var endDate: Date = Date()

    // Stores total spending per category
    private val categorySpendMap = mutableMapOf<String, Double>()

    // User-defined goal limits
    private var minGoal = 0.0
    private var maxGoal = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_spending_chart)

        // Initialize UI
        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
        barChart = findViewById(R.id.categoryBarChart)

        updateDateButtons()

        // Handle start date selection
        startDateButton.setOnClickListener {
            showDatePicker(startDate) { date ->
                startDate = date
                if (startDate.after(endDate)) {
                    Toast.makeText(this, "Start date can't be after end date", Toast.LENGTH_SHORT).show()
                } else {
                    updateDateButtons()
                    loadDataAndDisplayChart()
                }
            }
        }

        // Handle end date selection
        endDateButton.setOnClickListener {
            showDatePicker(endDate) { date ->
                endDate = date
                if (endDate.before(startDate)) {
                    Toast.makeText(this, "End date can't be before start date", Toast.LENGTH_SHORT).show()
                } else {
                    updateDateButtons()
                    loadDataAndDisplayChart()
                }
            }
        }

        // Load goals and then fetch & display data
        loadGoalsAndData()
    }

    // Update the text on date buttons
    private fun updateDateButtons() {
        startDateButton.text = "Start: ${dateFormat.format(startDate)}"
        endDateButton.text = "End: ${dateFormat.format(endDate)}"
    }

    // Opens a date picker dialog
    private fun showDatePicker(initialDate: Date, onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.time = initialDate

        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance()
            selectedCal.set(year, month, dayOfMonth)
            onDateSelected(selectedCal.time)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    // Loads the user's min and max goals from Firebase
    private fun loadGoalsAndData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val goalRef = FirebaseDatabase.getInstance().reference.child("goals").child(uid)

        goalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Get min and max goals; default to 0.0 if not set
                minGoal = snapshot.child("minGoal").getValue(Double::class.java) ?: 0.0
                maxGoal = snapshot.child("maxGoal").getValue(Double::class.java) ?: 0.0
                loadDataAndDisplayChart()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CategorySpendingChartActivity, "Failed to load goals", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Loads user's expenses from Firebase and filters them by date range
    private fun loadDataAndDisplayChart() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val expensesRef = FirebaseDatabase.getInstance().reference.child("expenses").child(uid)

        expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categorySpendMap.clear()

                for (expenseSnap in snapshot.children) {
                    val dateStr = expenseSnap.child("date").getValue(String::class.java) ?: continue
                    val category = expenseSnap.child("category").getValue(String::class.java) ?: "Uncategorized"
                    val amount = expenseSnap.child("amount").getValue(Double::class.java) ?: continue

                    // Parse date and filter by range
                    val expenseDate = try {
                        dateFormat.parse(dateStr)
                    } catch (e: Exception) {
                        null
                    }

                    if (expenseDate != null && !expenseDate.before(startDate) && !expenseDate.after(endDate)) {
                        categorySpendMap[category] = categorySpendMap.getOrDefault(category, 0.0) + amount
                    }
                }

                displayBarChart()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CategorySpendingChartActivity, "Failed to load expenses", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Renders a bar chart with spending data and goal limit lines
    private fun displayBarChart() {
        // Clear chart if no data
        if (categorySpendMap.isEmpty()) {
            barChart.clear()
            barChart.invalidate()
            return
        }

        val entries = mutableListOf<BarEntry>()
        val barColors = mutableListOf<Int>()
        val categories = categorySpendMap.keys.toList()

        // Build data entries and assign colors based on goal thresholds
        categorySpendMap.entries.forEachIndexed { index, entry ->
            val value = entry.value
            val color = when {
                value < minGoal -> Color.YELLOW
                value > maxGoal -> Color.RED
                else -> Color.GREEN
            }

            entries.add(BarEntry(index.toFloat(), value.toFloat()))
            barColors.add(color)
        }

        // Create the data set
        val dataSet = BarDataSet(entries, "Spending per Category").apply {
            colors = barColors
            valueTextSize = 12f
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.8f
        }

        barChart.data = barData

        // Configure X-axis (category labels)
        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = IndexAxisValueFormatter(categories)
            granularity = 1f
            setDrawGridLines(false)
            labelRotationAngle = -45f
            setAvoidFirstLastClipping(true)
        }

        // Configure Y-axis goal lines
        barChart.axisRight.isEnabled = false
        val leftAxis = barChart.axisLeft
        leftAxis.removeAllLimitLines()

        // Add min goal line
        if (minGoal > 0) {
            val minLine = LimitLine(minGoal.toFloat(), "Min Goal")
            minLine.lineWidth = 2f
            minLine.lineColor = Color.GREEN
            minLine.textColor = Color.GREEN
            minLine.textSize = 12f
            leftAxis.addLimitLine(minLine)
        }

        // Add max goal line
        if (maxGoal > 0) {
            val maxLine = LimitLine(maxGoal.toFloat(), "Max Goal")
            maxLine.lineWidth = 2f
            maxLine.lineColor = Color.RED
            maxLine.textColor = Color.RED
            maxLine.textSize = 12f
            leftAxis.addLimitLine(maxLine)
        }

        // Start Y-axis from 0
        leftAxis.axisMinimum = 0f

        // Other chart settings
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setExtraBottomOffset(20f)

        // Animate and render the chart
        barChart.animateY(1000)
        barChart.invalidate()
    }
}
