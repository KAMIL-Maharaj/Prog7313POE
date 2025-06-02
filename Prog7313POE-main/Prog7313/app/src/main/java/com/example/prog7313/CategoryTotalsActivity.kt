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

    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var showTotalsButton: Button
    private lateinit var totalsContainer: LinearLayout

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_totals)

        startDateEditText = findViewById(R.id.startDateEditText)
        endDateEditText = findViewById(R.id.endDateEditText)
        showTotalsButton = findViewById(R.id.showTotalsButton)
        totalsContainer = findViewById(R.id.totalsContainer)

        startDateEditText.setOnClickListener {
            showDatePicker { date -> startDateEditText.setText(date) }
        }

        endDateEditText.setOnClickListener {
            showDatePicker { date -> endDateEditText.setText(date) }
        }

        showTotalsButton.setOnClickListener {
            val startStr = startDateEditText.text.toString()
            val endStr = endDateEditText.text.toString()

            if (startStr.isEmpty() || endStr.isEmpty()) {
                Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadTotals(startStr, endStr)
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            c.set(year, month, dayOfMonth)
            onDateSelected(dateFormat.format(c.time))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadTotals(startDateStr: String, endDateStr: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference.child("expenses").child(uid)

        val startDate = dateFormat.parse(startDateStr) ?: return
        val endDate = dateFormat.parse(endDateStr) ?: return

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totals = HashMap<String, Double>()
                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(ExpenseListActivity.Expense::class.java)
                    expense?.let {
                        val expenseDate = dateFormat.parse(it.date)
                        if (expenseDate != null && !expenseDate.before(startDate) && !expenseDate.after(endDate)) {
                            val cat = it.category.ifEmpty { "Uncategorized" }
                            totals[cat] = (totals[cat] ?: 0.0) + it.amount
                        }
                    }
                }

                // Update UI
                totalsContainer.removeAllViews()
                if (totals.isEmpty()) {
                    val noDataText = TextView(this@CategoryTotalsActivity).apply {
                        text = "No expenses found for this period."
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                    }
                    totalsContainer.addView(noDataText)
                } else {
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
