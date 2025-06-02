package com.example.prog7313

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SetGoalActivity : AppCompatActivity() {

    private lateinit var minGoalEditText: EditText
    private lateinit var maxGoalEditText: EditText
    private lateinit var saveOverallGoalButton: Button

    private lateinit var categorySpinner: Spinner
    private lateinit var categoryMinGoalEditText: EditText
    private lateinit var categoryMaxGoalEditText: EditText
    private lateinit var saveCategoryGoalButton: Button

    private val categoriesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_goal)

        // Monthly goals UI
        minGoalEditText = findViewById(R.id.minGoalEditText)
        maxGoalEditText = findViewById(R.id.maxGoalEditText)
        saveOverallGoalButton = findViewById(R.id.saveOverallGoalButton)

        // Category goals UI
        categorySpinner = findViewById(R.id.categorySpinner)
        categoryMinGoalEditText = findViewById(R.id.categoryMinGoalEditText)
        categoryMaxGoalEditText = findViewById(R.id.categoryMaxGoalEditText)
        saveCategoryGoalButton = findViewById(R.id.saveCategoryGoalButton)

        // Load user's categories
        loadCategories()

        saveOverallGoalButton.setOnClickListener {
            saveOverallGoals()
        }

        saveCategoryGoalButton.setOnClickListener {
            saveCategoryGoals()
        }
    }

    private fun saveOverallGoals() {
        val minGoal = minGoalEditText.text.toString().toDoubleOrNull()
        val maxGoal = maxGoalEditText.text.toString().toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Enter valid numbers for monthly goals", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal > maxGoal) {
            Toast.makeText(this, "Minimum cannot be greater than maximum", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val goalRef = FirebaseDatabase.getInstance().reference.child("goals").child(uid)

        val goalData = mapOf(
            "minGoal" to minGoal,
            "maxGoal" to maxGoal
        )

        goalRef.setValue(goalData)
            .addOnSuccessListener {
                Toast.makeText(this, "Monthly goals saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save monthly goals", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveCategoryGoals() {
        val selectedCategory = categorySpinner.selectedItem as? String ?: return
        val minGoal = categoryMinGoalEditText.text.toString().toDoubleOrNull()
        val maxGoal = categoryMaxGoalEditText.text.toString().toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Enter valid numbers for category goals", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal > maxGoal) {
            Toast.makeText(this, "Minimum cannot be greater than maximum", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val categoryGoalRef = FirebaseDatabase.getInstance()
            .reference.child("goals").child(uid).child("categories").child(selectedCategory)

        val goalData = mapOf(
            "minGoal" to minGoal,
            "maxGoal" to maxGoal
        )

        categoryGoalRef.setValue(goalData)
            .addOnSuccessListener {
                Toast.makeText(this, "Goals saved for category: $selectedCategory", Toast.LENGTH_SHORT).show()
                categoryMinGoalEditText.text.clear()
                categoryMaxGoalEditText.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save category goals", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCategories() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val categoriesRef = FirebaseDatabase.getInstance()
            .reference.child("users").child(uid).child("categories")

        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoriesList.clear()
                for (categorySnapshot in snapshot.children) {
                    val categoryName = categorySnapshot.getValue(String::class.java)
                    if (categoryName != null) {
                        categoriesList.add(categoryName)
                    }
                }

                val adapter = ArrayAdapter(
                    this@SetGoalActivity,
                    android.R.layout.simple_spinner_item,
                    categoriesList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                categorySpinner.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SetGoalActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
