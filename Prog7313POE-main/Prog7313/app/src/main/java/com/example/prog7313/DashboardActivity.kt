package com.example.prog7313

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import java.util.Calendar

// Main dashboard activity that includes a navigation drawer and salary countdown
class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var salaryCountdownText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Setup toolbar and navigation drawer
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Configure hamburger menu toggle behavior
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set navigation item click listener
        navView.setNavigationItemSelectedListener(this)

        // Initialize salary countdown display
        salaryCountdownText = findViewById(R.id.salaryCountdownText)
        updateSalaryCountdown()
    }

    // Updates the salary countdown text based on stored preferences
    private fun updateSalaryCountdown() {
        val prefs = getSharedPreferences("SalaryPrefs", Context.MODE_PRIVATE)
        val salaryDay = prefs.getInt("salary_day", -1)

        // Show countdown only if salary day is set
        if (salaryDay in 1..31) {
            val daysLeft = calculateDaysUntilNextSalary(salaryDay)
            salaryCountdownText.text = if (daysLeft == 0) {
                "🎉 Salary Day is Today!"
            } else {
                "💰 $daysLeft day(s) until next salary day"
            }
        } else {
            salaryCountdownText.text = "Set your salary day to see countdown"
        }
    }

    // Calculates how many days are left until the next salary day
    private fun calculateDaysUntilNextSalary(salaryDay: Int): Int {
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        // Set up calendar object for next salary date
        val nextSalaryDate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, salaryDay)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)

            // If salary day already passed, move to the next month
            if (before(today) && salaryDay < currentDay) {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, salaryDay)
            }
        }

        // Calculate difference in milliseconds and convert to days
        val millisDiff = nextSalaryDate.timeInMillis - today.timeInMillis
        return (millisDiff / (1000 * 60 * 60 * 24)).toInt()
    }

    // Handles navigation drawer item selection
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_salary -> startActivity(Intent(this, SalaryInputActivity::class.java))
            R.id.nav_category -> startActivity(Intent(this, CategoryActivity::class.java))
            R.id.nav_expense -> startActivity(Intent(this, CreateExpenseActivity::class.java))
            R.id.nav_set_goal -> startActivity(Intent(this, SetGoalActivity::class.java))
            R.id.nav_expense_list -> startActivity(Intent(this, ExpenseListActivity::class.java))
            R.id.nav_category_totals -> startActivity(Intent(this, CategoryTotalsActivity::class.java))
            R.id.nav_monthly_summary -> startActivity(Intent(this, MonthlySummaryActivity::class.java))
            R.id.nav_category_graph -> startActivity(Intent(this, CategorySpendingChartActivity::class.java))
            R.id.activity_achievements -> startActivity(Intent(this, AchievementsActivity::class.java))
            R.id.nav_budget_game -> startActivity(Intent(this, BubbleGameActivity::class.java))
        }

        // Close drawer after selection
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    // Closes drawer if open on back press, otherwise behaves normally
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Refresh salary countdown every time dashboard is resumed
    override fun onResume() {
        super.onResume()
        updateSalaryCountdown()
    }
}
