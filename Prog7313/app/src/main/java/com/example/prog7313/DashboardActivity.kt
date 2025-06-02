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

class DashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var salaryCountdownText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Setup navigation drawer
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(this)

        // Setup salary countdown
        salaryCountdownText = findViewById(R.id.salaryCountdownText)
        updateSalaryCountdown()
    }

    private fun updateSalaryCountdown() {
        val prefs = getSharedPreferences("SalaryPrefs", Context.MODE_PRIVATE)
        val salaryDay = prefs.getInt("salary_day", -1)

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

    private fun calculateDaysUntilNextSalary(salaryDay: Int): Int {
        val today = Calendar.getInstance()
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        val currentMonth = today.get(Calendar.MONTH)
        val currentYear = today.get(Calendar.YEAR)

        val nextSalaryDate = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, salaryDay)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.YEAR, currentYear)

            // If salary day already passed this month, move to next month
            if (before(today) && salaryDay < currentDay) {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, salaryDay)
            }
        }

        val millisDiff = nextSalaryDate.timeInMillis - today.timeInMillis
        return (millisDiff / (1000 * 60 * 60 * 24)).toInt()
    }

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
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh countdown when returning to dashboard
        updateSalaryCountdown()
    }
}