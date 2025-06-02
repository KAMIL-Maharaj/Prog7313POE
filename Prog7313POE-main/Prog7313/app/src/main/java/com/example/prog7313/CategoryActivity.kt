package com.example.prog7313

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CategoryActivity : AppCompatActivity() {

    private lateinit var categoryEditText: EditText
    private lateinit var addButton: Button
    private lateinit var categoryListView: ListView
    private lateinit var databaseRef: DatabaseReference
    private lateinit var adapter: ArrayAdapter<String>
    private val categories = mutableListOf<String>()
    private val categoryKeys = mutableListOf<String>() // Optional if you want to delete later

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category)

        categoryEditText = findViewById(R.id.categoryEditText)
        addButton = findViewById(R.id.addCategoryButton)
        categoryListView = findViewById(R.id.categoryListView)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId!!).child("categories")

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        categoryListView.adapter = adapter

        addButton.setOnClickListener {
            val category = categoryEditText.text.toString().trim()
            if (category.isNotEmpty()) {
                val newRef = databaseRef.push()
                newRef.setValue(category)
                categoryEditText.text.clear()
            } else {
                Toast.makeText(this, "Enter a category name", Toast.LENGTH_SHORT).show()
            }
        }

        // Load categories
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categories.clear()
                categoryKeys.clear()
                for (child in snapshot.children) {
                    child.getValue(String::class.java)?.let {
                        categories.add(it)
                        categoryKeys.add(child.key!!)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CategoryActivity, "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
