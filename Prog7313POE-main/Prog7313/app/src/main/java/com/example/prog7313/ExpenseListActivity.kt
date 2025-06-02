package com.example.prog7313

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ExpenseListActivity : AppCompatActivity() {

    // UI components
    private lateinit var startDateEditText: EditText
    private lateinit var endDateEditText: EditText
    private lateinit var filterButton: Button
    private lateinit var expensesRecyclerView: RecyclerView

    // Date handling
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Data list to hold expenses
    private val expensesList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        // Initialize UI elements
        startDateEditText = findViewById(R.id.startDateEditText)
        endDateEditText = findViewById(R.id.endDateEditText)
        filterButton = findViewById(R.id.filterButton)
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView)

        // Setup RecyclerView with layout and adapter
        expensesRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = ExpenseAdapter(expensesList)
        expensesRecyclerView.adapter = adapter

        // Show date picker on clicking date fields
        startDateEditText.setOnClickListener {
            showDatePickerDialog { date -> startDateEditText.setText(date) }
        }
        endDateEditText.setOnClickListener {
            showDatePickerDialog { date -> endDateEditText.setText(date) }
        }

        // Filter expenses when button is clicked
        filterButton.setOnClickListener {
            val startDateStr = startDateEditText.text.toString()
            val endDateStr = endDateEditText.text.toString()

            // Ensure both dates are entered
            if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
                Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Load and filter expenses
            loadExpenses(startDateStr, endDateStr, adapter)
        }
    }

    // Helper function to show a date picker and return the selected date
    private fun showDatePickerDialog(onDateSelected: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            c.set(year, month, dayOfMonth)
            onDateSelected(dateFormat.format(c.time))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    // Loads expenses from Firebase and filters them by date
    private fun loadExpenses(startDateStr: String, endDateStr: String, adapter: ExpenseAdapter) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().reference.child("expenses").child(uid)

        val startDate = dateFormat.parse(startDateStr) ?: return
        val endDate = dateFormat.parse(endDateStr) ?: return

        expensesList.clear() // Clear current list

        // Fetch data from Firebase
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (expenseSnapshot in snapshot.children) {
                    val expense = expenseSnapshot.getValue(Expense::class.java)
                    expense?.let {
                        // Only include expenses within the selected date range
                        val expenseDate = dateFormat.parse(it.date)
                        if (expenseDate != null && !expenseDate.before(startDate) && !expenseDate.after(endDate)) {
                            expensesList.add(it)
                        }
                    }
                }
                adapter.notifyDataSetChanged() // Refresh RecyclerView
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ExpenseListActivity, "Failed to load expenses", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Data model class for expense items
    data class Expense(
        val date: String = "",
        val startTime: String = "",
        val endTime: String = "",
        val description: String = "",
        val category: String = "",
        val amount: Double = 0.0,
        val photoUrl: String? = null
    )

    // Adapter class for RecyclerView to display list of expenses
    inner class ExpenseAdapter(private val items: List<Expense>) :
        RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

        // ViewHolder to hold each expense item view
        inner class ExpenseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
            val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
            val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
            val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
            val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
        }

        // Inflate item layout for each expense
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_expense, parent, false)
            return ExpenseViewHolder(view)
        }

        // Bind expense data to the view holder
        override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
            val expense = items[position]
            holder.dateTextView.text = expense.date
            holder.descriptionTextView.text = expense.description
            holder.categoryTextView.text = expense.category
            holder.amountTextView.text = String.format(Locale.getDefault(), "R%.2f", expense.amount)

            // Load expense photo with Glide or show placeholder
            Glide.with(this@ExpenseListActivity)
                .load(if (!expense.photoUrl.isNullOrEmpty()) expense.photoUrl else R.drawable.ic_photo_placeholder)
                .placeholder(R.drawable.ic_photo_placeholder)
                .error(R.drawable.ic_photo_placeholder)
                .into(holder.photoImageView)

            // Open image in browser if URL is available
            if (!expense.photoUrl.isNullOrEmpty()) {
                holder.photoImageView.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(expense.photoUrl))
                    startActivity(intent)
                }
            } else {
                holder.photoImageView.setOnClickListener(null)
            }
        }

        // Return total number of items
        override fun getItemCount(): Int = items.size
    }
}
