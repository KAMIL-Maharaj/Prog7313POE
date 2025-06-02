package com.example.prog7313

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SalaryInputActivity : AppCompatActivity() {

    private lateinit var salaryEditText: EditText
    private lateinit var salaryDayEditText: EditText
    private lateinit var saveSalaryButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salary_input)

        salaryEditText = findViewById(R.id.salaryEditText)
        salaryDayEditText = findViewById(R.id.salaryDayEditText)
        saveSalaryButton = findViewById(R.id.saveSalaryButton)

        saveSalaryButton.setOnClickListener {
            val salary = salaryEditText.text.toString().trim()
            val salaryDay = salaryDayEditText.text.toString().trim().toIntOrNull()

            if (salary.isEmpty()) {
                Toast.makeText(this, "Please enter your salary", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (salaryDay == null || salaryDay !in 1..31) {
                Toast.makeText(this, "Please enter a valid salary day (1–31)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = auth.currentUser?.uid
            if (uid != null) {
                // Save salary to Firebase
                database.child("users").child(uid).child("salary").setValue(salary)
                    .addOnSuccessListener {
                        // Save salary day locally using SharedPreferences
                        val prefs = getSharedPreferences("SalaryPrefs", MODE_PRIVATE)
                        prefs.edit().putInt("salary_day", salaryDay).apply()

                        Toast.makeText(this, "Salary and salary day saved!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to save salary", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
