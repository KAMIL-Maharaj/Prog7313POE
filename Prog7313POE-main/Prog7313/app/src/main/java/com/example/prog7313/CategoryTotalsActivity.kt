package com.example.prog7313

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class CategoryTotalsActivity : AppCompatActivity() {

    // UI elements
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var showTotalsButton: Button
    private lateinit var totalsContainer: LinearLayout

    // Date format used throughout the activity
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_totals)

        // Initialize UI components
        startDateEditText = findViewById(R.id.startDateEditText)
        endDateEditText = findViewById(R.id.endDateEditText)
        showTotalsButton = findViewById(R.id.showTotalsButton)
        totalsContainer = findViewById(R.id.totalsContainer)

        // Open date picker dialog when user clicks on start date field
        startDateEditText.setOnClickListener {
            showDatePicker { date -> startDateEditText.setText(date) }
        }

        // Open date picker dialog when user clicks on end date field
        endDateEditText.setOnClickListener {
            showDatePicker { date -> endDateEditText.setText(date) }
        }

        // Handle button click to load totals for selected date range
        showTotalsButton.setOnClickListener {
            val startStr = startDateEditText.text.toString()
            val endStr = endDateEditText.text.toString()

            // Validate date inputs
            if (startStr.isEmpty() || endStr.isEmpty()) {
                Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadTotals(startStr, endStr) // Load category totals between selected dates
        }
    }

    // Shows a date picker dialog and returns the selected date as a string
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            c.set(year, month, dayOfMonth)
            onDateSelected(dateFormat.format(c.time))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    // Loads and calculates total spending per category within the selected date range
    private fun loadTotals(startDateStr: String, endDateStr: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference.child("expenses").child(uid)

        // Convert string dates to Date objects
        val startDate = dateFormat.parse(startDateStr) ?: return
        val endDate = dateFormat.parse(endDateStr) ?: return

        // Retrieve expense data from Firebase
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totals = HashMap<String, Double>()

                // Iterate over all expenses
                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(ExpenseListActivity.Expense::class.java)
                    expense?.let {
                        val expenseDate = dateFormat.parse(it.date)

                        // Include expense only if it's within the selected date range
                        if (expenseDate != null && !expenseDate.before(startDate) && !expenseDate.after(endDate)) {
                            val cat = it.category.ifEmpty { "Uncategorized" }
                            totals[cat] = (totals[cat] ?: 0.0) + it.amount
                        }
                    }
                }

                // Clear previous results in UI
                totalsContainer.removeAllViews()

                // Show message if no expenses found
                if (totals.isEmpty()) {
                    val noDataText = TextView(this@CategoryTotalsActivity).apply {
                        text = "No expenses found for this period."
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                    }
                    totalsContainer.addView(noDataText)
                } else {
                    // Display totals in styled CardViews for each category
                    totals.forEach { (category, total) ->
                        val card = CardView(this@CategoryTotalsActivity).apply {
                            val params = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            params.setMargins(0, 0, 0, 20)
                            layoutParams = params
                            radius = 16f
                            cardElevation = 8f
                            setContentPadding(32, 24, 32, 24)
                            setCardBackgroundColor(getColor(R.color.white))
                        }

                        val textView = TextView(this@CategoryTotalsActivity).apply {
                            text = "$category: R${"%.2f".format(total)}"
                            textSize = 18f
                            setTextColor(getColor(R.color.black))
                        }

                        card.addView(textView)
                        totalsContainer.addView(card)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CategoryTotalsActivity, "Failed to load totals", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
