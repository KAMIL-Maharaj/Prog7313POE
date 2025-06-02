package com.example.prog7313

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.*

class CreateExpenseActivity : AppCompatActivity() {

    private lateinit var dateEditText: EditText
    private lateinit var startTimeEditText: EditText
    private lateinit var endTimeEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var selectPhotoButton: Button
    private lateinit var photoPreview: ImageView

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val categoriesList = mutableListOf<String>()

    private val PICK_IMAGE_REQUEST = 1
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_expense)

        dateEditText = findViewById(R.id.dateEditText)
        startTimeEditText = findViewById(R.id.startTimeEditText)
        endTimeEditText = findViewById(R.id.endTimeEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        amountEditText = findViewById(R.id.amountEditText) // NEW: Find amount input
        categorySpinner = findViewById(R.id.categorySpinner)
        saveButton = findViewById(R.id.saveButton)
        selectPhotoButton = findViewById(R.id.selectPhotoButton)
        photoPreview = findViewById(R.id.photoPreview)

        loadCategories()

        dateEditText.setOnClickListener {
            DatePickerDialog(this,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        startTimeEditText.setOnClickListener {
            TimePickerDialog(this,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    startTimeEditText.setText(timeFormat.format(calendar.time))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        endTimeEditText.setOnClickListener {
            TimePickerDialog(this,
                { _, hour, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    endTimeEditText.setText(timeFormat.format(calendar.time))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        selectPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveButton.setOnClickListener {
            if (validateInputs()) {
                saveExpenseToFirebase()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            photoUri = data.data
            photoPreview.setImageURI(photoUri)
            photoPreview.visibility = View.VISIBLE
        }
    }

    private fun loadCategories() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
            .child("users")
            .child(uid)
            .child("categories")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    categoriesList.clear()
                    for (categorySnapshot in snapshot.children) {
                        val categoryName = categorySnapshot.getValue(String::class.java)
                        categoryName?.let { categoriesList.add(it) }
                    }
                    if (categoriesList.isEmpty()) {
                        categoriesList.add("No categories available")
                    }
                    val adapter = ArrayAdapter(this@CreateExpenseActivity, android.R.layout.simple_spinner_item, categoriesList)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    categorySpinner.adapter = adapter
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@CreateExpenseActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun validateInputs(): Boolean {
        if (dateEditText.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return false
        }
        if (startTimeEditText.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select a start time", Toast.LENGTH_SHORT).show()
            return false
        }
        if (endTimeEditText.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please select an end time", Toast.LENGTH_SHORT).show()
            return false
        }
        if (descriptionEditText.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please enter a description", Toast.LENGTH_SHORT).show()
            return false
        }
        if (amountEditText.text.isNullOrEmpty() || amountEditText.text.toString().toDoubleOrNull() == null) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return false
        }
        val selectedCategory = categorySpinner.selectedItem?.toString() ?: ""
        if (selectedCategory == "No categories available" || selectedCategory.isEmpty()) {
            Toast.makeText(this, "Please create a category first", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveExpenseToFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val selectedCategory = categorySpinner.selectedItem.toString()
        val expenseRef = FirebaseDatabase.getInstance().reference
            .child("expenses")
            .child(uid)
            .push()

        if (photoUri != null) {
            val storageRef = FirebaseStorage.getInstance().reference
                .child("expense_photos/$uid/${expenseRef.key}.jpg")

            storageRef.putFile(photoUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        saveExpense(expenseRef, selectedCategory, downloadUri.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload photo", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveExpense(expenseRef, selectedCategory, null)
        }
    }

    private fun saveExpense(
        expenseRef: DatabaseReference,
        category: String,
        photoUrl: String?
    ) {
        val amountValue = amountEditText.text.toString().toDoubleOrNull() ?: 0.0

        val expense = mapOf(
            "date" to dateEditText.text.toString(),
            "startTime" to startTimeEditText.text.toString(),
            "endTime" to endTimeEditText.text.toString(),
            "description" to descriptionEditText.text.toString(),
            "category" to category,
            "amount" to amountValue,
            "photoUrl" to photoUrl
        )

        expenseRef.setValue(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save expense", Toast.LENGTH_SHORT).show()
            }
    }
}
