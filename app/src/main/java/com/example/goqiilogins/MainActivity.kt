package com.example.goqiilogins

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.appcompat.app.AlertDialog
import java.util.Calendar
import android.text.InputType

class MainActivity : AppCompatActivity() {

    private val targetPackage = "com.betaout.GOQii"
    private lateinit var tableLayout: TableLayout
    private lateinit var btnAdd: Button
    private lateinit var btnLogin: Button
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button
    private lateinit var btnScroll: Button
    private lateinit var btnStop: Button

    private var selectedAccount: String? = null
    private var selectedRowPosition: Int = -1
    private val accountsList = mutableListOf<Account>()
    private val sharedPref by lazy { getSharedPreferences("AppPrefs", MODE_PRIVATE) }

    data class Account(
        val sno: Int,
        var email: String,
        var lastDate: String,
        var isSelected: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MyAccessibilityServiceController.loadState(this)

        // Initialize views
        tableLayout = findViewById(R.id.accountsTable)
        btnAdd = findViewById(R.id.btnAdd)
        btnLogin = findViewById(R.id.btnLogin)
        btnSave = findViewById(R.id.btnSave)
        btnClear = findViewById(R.id.btnClear)
        btnScroll = findViewById(R.id.btnScroll)
        btnStop = findViewById(R.id.btnStop)

        // Load saved data
        loadSavedData()

        // Setup table with loaded data or default - ensure no selection
        if (accountsList.isEmpty()) {
            // Add default row (NOT selected)
            accountsList.add(Account(1, "chinni4help@gmail.com", "08/09/2025", false))
        } else {
            // Ensure no existing rows are selected
            accountsList.forEachIndexed { index, account ->
                accountsList[index] = account.copy(isSelected = false)
            }
        }

        selectedAccount = null
        selectedRowPosition = -1

        refreshTable()

        btnAdd.setOnClickListener {
            addNewAccountRow()
        }

        btnLogin.setOnClickListener {
            if (selectedAccount != null) {
                startLoginProcess(selectedAccount!!)
            } else {
                Toast.makeText(this, "Please select an account first", Toast.LENGTH_SHORT).show()
            }
        }

        btnSave.setOnClickListener {
            // FIRST update the data model from the UI, THEN save
            updateDataModelFromUI()
            saveData()
            Toast.makeText(this, "Data saved successfully!", Toast.LENGTH_SHORT).show()
            unhighlightAllRows()
        }

        btnClear.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$targetPackage")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        btnScroll.setOnClickListener {
            showScrollDialog()
        }

        // Add click listener
        btnStop.setOnClickListener {
            stopAllAutomation()
        }
    }

    override fun onResume() {
        super.onResume()
        MyAccessibilityServiceController.loadState(this)
    }

    override fun onPause() {
        super.onPause()
        MyAccessibilityServiceController.saveState(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        MyAccessibilityServiceController.saveState(this)
    }

    private fun addNewAccountRow() {
        val newSno = accountsList.size + 1
        val newAccount = Account(newSno, "new_account@gmail.com", "Not logged in", false)
        accountsList.add(newAccount)
        refreshTable()
    }

    private fun refreshTable() {
        tableLayout.removeAllViews()

        // Add header row
        val headerRow = TableRow(this).apply {
            addView(createHeaderTextView("SNo", 1f))
            addView(createHeaderTextView("Account", 3f))
            addView(createHeaderTextView("Last Date", 2f))
        }
        tableLayout.addView(headerRow)

        // Add data rows
        accountsList.forEachIndexed { index, account ->
            val row = TableRow(this).apply {
                // SNo column - CLICKABLE
                val snoView = createDataTextView(account.sno.toString(), 1f).apply {
                    setOnClickListener {
                        selectRow(index)
                    }
                }
                addView(snoView)

                // Account column - EDITABLE
                val accountEditText = createEditableAccountView(account.email, 3f).apply {
                    setOnClickListener {
                        selectRow(index)
                    }
                }
                addView(accountEditText)

                // Last Date column - EDITABLE
                val dateEditText = createEditableDateView(account.lastDate, 2f)
                addView(dateEditText)

                // Set background based on selection
                if (account.isSelected) {
                    setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.selected_row))
                } else {
                    setBackgroundColor(ContextCompat.getColor(this@MainActivity, android.R.color.transparent))
                }
            }
            tableLayout.addView(row)
        }
    }

    // Update data model from UI fields before saving
    private fun updateDataModelFromUI() {
        for (i in 0 until tableLayout.childCount - 1) { // -1 to skip header
            val tableRow = tableLayout.getChildAt(i + 1) as? TableRow ?: continue

            val accountEditText = tableRow.getChildAt(1) as? EditText
            val dateEditText = tableRow.getChildAt(2) as? EditText

            if (accountEditText != null && dateEditText != null) {
                val newEmail = accountEditText.text.toString()
                val newDate = dateEditText.text.toString()

                // Update the data model
                if (i < accountsList.size) {
                    accountsList[i] = accountsList[i].copy(
                        email = newEmail,
                        lastDate = newDate
                    )

                    // Update selected account if this was the selected row
                    if (accountsList[i].isSelected) {
                        selectedAccount = newEmail
                    }
                }
            }
        }
    }

    private fun showScrollDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_date_range, null)

        val etStartDate = dialogView.findViewById<EditText>(R.id.etStartDate)
        val etEndDate = dialogView.findViewById<EditText>(R.id.etEndDate)
        val radioDays = dialogView.findViewById<RadioGroup>(R.id.radioDays)

        // Get the selected row's last date
        val selectedLastDate = if (selectedRowPosition != -1) {
            accountsList[selectedRowPosition].lastDate
        } else {
            "Not logged in"
        }

        // Convert end date to display format
        val endDateDisplay = convertToDisplayFormat(selectedLastDate)

        // Auto-calculate start date based on selected days
        fun updateStartDate() {
            val selectedDays = when (radioDays.checkedRadioButtonId) {
                R.id.radio1Day -> 1
                R.id.radio2Days -> 2
                R.id.radio3Days -> 3
                else -> 1 // Default
            }
            val startDateDisplay = calculateStartDate(endDateDisplay, selectedDays)
            etStartDate.setText(startDateDisplay)
        }

        // Set initial values
        etEndDate.setText(endDateDisplay)
        updateStartDate()

        // Update start date when days selection changes
        radioDays.setOnCheckedChangeListener { _, _ ->
            updateStartDate()
        }

        AlertDialog.Builder(this)
            .setTitle("Scroll and Like by Date Range")
            .setMessage("Select days and dates will auto-calculate:")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                val startDate = etStartDate.text.toString().trim()
                val endDate = etEndDate.text.toString().trim()

                if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                    startScrollAndLikeProcess(startDate, endDate)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun startScrollAndLikeProcess(startDate: String, endDate: String) {
        Log.d("MainActivity", "startScrollAndLikeProcess - selectedAccount: $selectedAccount")
        Log.d("MainActivity", "Controller.selectedAccount: ${MyAccessibilityServiceController.selectedAccount}")
        MyAccessibilityServiceController.selectedAccount = this.selectedAccount
        // Update the LastDate for the selected account
        if (selectedRowPosition != -1) {
            // Extract date from "| 18 Sep" format to "18/09/2025" format
            val newLastDate = convertToStorageFormat(startDate)
            accountsList[selectedRowPosition] = accountsList[selectedRowPosition].copy(lastDate = newLastDate)
            refreshTable()

            MyAccessibilityServiceController.selectedAccount = selectedAccount
        }

        MyAccessibilityServiceController.reset()

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        launchIntent?.let { startActivity(it) }

        Handler(Looper.getMainLooper()).postDelayed({
            MyAccessibilityServiceController.shouldScrollAndLike = true
            MyAccessibilityServiceController.startDateText = startDate
            MyAccessibilityServiceController.endDateText = endDate
            MyAccessibilityServiceController.isProcessingRange = false
            MyAccessibilityServiceController.foundStartDate = false
            MyAccessibilityServiceController.foundEndDate = false

            Log.d("MainActivity", "Starting scroll and like from '$startDate' to '$endDate'")

            triggerAccessibilityService()
        }, 20)
    }

    private fun stopAllAutomation() {
        MyAccessibilityServiceController.saveState(this)
        MyAccessibilityServiceController.reset()
        MyAccessibilityServiceController.clickedPostKeys.clear()  // Clear tracked posts
        Toast.makeText(this, "🛑 Automation stopped!", Toast.LENGTH_LONG).show()
        Log.d("MainActivity", "User requested stop - all automation reset")
    }

    private fun createHeaderTextView(text: String, weight: Float): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = 16f
            setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.white))
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.table_header))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(16, 8, 16, 8)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    private fun createDataTextView(text: String, weight: Float): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = 14f
            setPadding(16, 12, 16, 12)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    private fun createEditableAccountView(text: String, weight: Float): EditText {
        return EditText(this).apply {
            this.setText(text)
            this.textSize = 14f
            setPadding(16, 12, 16, 12)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    private fun createEditableDateView(text: String, weight: Float): EditText {
        return EditText(this).apply {
            this.setText(text)
            this.textSize = 14f
            setPadding(16, 12, 16, 12)
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, weight)
        }
    }

    private fun selectRow(position: Int) {
        // Deselect all rows
        accountsList.forEachIndexed { index, account ->
            accountsList[index] = account.copy(isSelected = false)
        }

        // Select the clicked row
        accountsList[position] = accountsList[position].copy(isSelected = true)
        selectedAccount = accountsList[position].email
        selectedRowPosition = position

        // SAVE THE SELECTED ACCOUNT IMMEDIATELY
        MyAccessibilityServiceController.selectedAccount = selectedAccount
        MyAccessibilityServiceController.saveState(this)

        refreshTable()
    }

    private fun unhighlightAllRows() {
        // Deselect all rows
        accountsList.forEachIndexed { index, account ->
            accountsList[index] = account.copy(isSelected = false)
        }
        selectedAccount = null
        selectedRowPosition = -1
        refreshTable()
    }

    private fun startLoginProcess(account: String) {
        Log.d("MainActivity", "startLoginProcess called with account: $account")
        Log.d("MainActivity", "selectedAccount in MainActivity: $selectedAccount")
        MyAccessibilityServiceController.reset()
        MyAccessibilityServiceController.selectedAccount = account

        Log.d("MainActivity", "Controller.selectedAccount after set: ${MyAccessibilityServiceController.selectedAccount}")

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        launchIntent?.let { startActivity(it) }

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("MainActivity", "Delayed - Controller.selectedAccount: ${MyAccessibilityServiceController.selectedAccount}")

            MyAccessibilityServiceController.shouldClickSignIn = true
            Log.d("MainActivity", "Starting login for: $account")

            triggerAccessibilityService()
        }, 10)
    }

    private fun triggerAccessibilityService() {
        val intent = Intent(this, MyAccessibilityService::class.java)
        startService(intent)
    }

    private fun saveData() {
        val json = Gson().toJson(accountsList)
        sharedPref.edit().putString("accounts_data", json).apply()
    }

    private fun convertToStorageFormat(displayDate: String): String {
        return try {
            if (displayDate == "| Today" || displayDate == "| Yesterday") {
                // For today/yesterday, use current date
                val calendar = Calendar.getInstance()
                if (displayDate == "| Yesterday") {
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                val day = calendar.get(Calendar.DAY_OF_MONTH)
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                String.format("%02d/%02d/%04d", day, month, year)
            } else {
                // Parse "| 18 Sep" format
                val cleanDate = displayDate.removePrefix("| ").trim()
                val parts = cleanDate.split(" ")
                if (parts.size == 2) {
                    val day = parts[0].toInt()
                    val monthStr = parts[1]
                    val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    val month = monthNames.indexOf(monthStr) + 1
                    val year = Calendar.getInstance().get(Calendar.YEAR)
                    String.format("%02d/%02d/%04d", day, month, year)
                } else {
                    "Not logged in"
                }
            }
        } catch (e: Exception) {
            "Not logged in"
        }
    }

    private fun loadSavedData() {
        val json = sharedPref.getString("accounts_data", null)
        if (json != null) {
            val type = object : TypeToken<List<Account>>() {}.type
            val savedList: List<Account> = Gson().fromJson(json, type)
            accountsList.clear()
            accountsList.addAll(savedList)

            // Ensure no row is selected when app starts
            accountsList.forEachIndexed { index, account ->
                accountsList[index] = account.copy(isSelected = false)
            }
            selectedAccount = null
            selectedRowPosition = -1
        }
    }

    // Date conversion and calculation functions
    private fun convertToDisplayFormat(dateStr: String): String {
        return try {
            when {
                dateStr == "Not logged in" -> "| Yesterday"
                dateStr.contains("/") -> {
                    val parts = dateStr.split("/")
                    if (parts.size == 3) {
                        val day = parts[0].toInt()
                        val month = parts[1].toInt()
                        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        "| $day ${monthNames.getOrNull(month - 1) ?: "Unknown"}"
                    } else {
                        "| Yesterday"
                    }
                }
                else -> "| Yesterday"
            }
        } catch (e: Exception) {
            "| Yesterday"
        }
    }

    private fun calculateStartDate(endDateDisplay: String, days: Int = 2): String {
        return try {
            val today = getTodayDateString()
            val yesterday = getYesterdayDateString()

            when (endDateDisplay) {
                yesterday -> when (days) {
                    1 -> "| Today"
                    2 -> "| Today" // Yesterday + 2 days doesn't make sense, so default to Today
                    3 -> "| Today"
                    else -> "| Today"
                }
                else -> {
                    val calculatedDate = parseAndAddDays(endDateDisplay, days)
                    when (calculatedDate) {
                        today -> "| Today"
                        yesterday -> "| Yesterday"
                        else -> calculatedDate
                    }
                }
            }
        } catch (e: Exception) {
            "| Today"
        }
    }

    private fun parseAndAddDays(dateStr: String, daysToAdd: Int): String {
        return try {
            val cleanDate = dateStr.removePrefix("| ").trim()
            val parts = cleanDate.split(" ")
            if (parts.size == 2) {
                val day = parts[0].toInt()
                val monthStr = parts[1]

                val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val month = monthNames.indexOf(monthStr) + 1

                if (month != 0) {
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.MONTH, month - 1)
                    calendar.set(Calendar.DAY_OF_MONTH, day)
                    calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)

                    val newDay = calendar.get(Calendar.DAY_OF_MONTH)
                    val newMonth = monthNames[calendar.get(Calendar.MONTH)]
                    "| $newDay $newMonth"
                } else {
                    "| Yesterday"
                }
            } else {
                "| Yesterday"
            }
        } catch (e: Exception) {
            "| Yesterday"
        }
    }

    private fun getTodayDateString(): String {
        val calendar = Calendar.getInstance()
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = monthNames[calendar.get(Calendar.MONTH)]
        return "| $day $month"
    }

    private fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = monthNames[calendar.get(Calendar.MONTH)]
        return "| $day $month"
    }
}

object MyAccessibilityServiceController {
    var shouldClickSignIn = false
    var shouldClickBtnLogin = false
    var shouldSelectEmail = false
    var shouldScrollToText = false
    var shouldScrollAndLike = false
    var selectedAccount: String? = null
    var textToScrollTo: String? = null
    var startDateText: String? = null  // Start date pattern
    var endDateText: String? = null    // End date pattern
    var isProcessingRange = false      // Whether we're in the date range
    var foundStartDate = false         // Whether start date was found
    var foundEndDate = false           // Whether end date was found
    val clickedPostKeys = mutableSetOf<String>() // Track liked posts by their content key

    fun reset() {
        shouldClickSignIn = false
        shouldClickBtnLogin = false
        shouldSelectEmail = false
        shouldScrollToText = false
        shouldScrollAndLike = false
        //selectedAccount = null
        textToScrollTo = null
        startDateText = null
        endDateText = null
        isProcessingRange = false
        foundStartDate = false
        foundEndDate = false
        //clickedPostKeys.clear()  // Clear tracked posts
    }

    fun saveState(context: Context) {
        val prefs = context.getSharedPreferences("AutomationPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("liked_posts", clickedPostKeys)
            .putString("selected_account", selectedAccount)
            .apply()
        Log.d("Controller", "Saved state - account: $selectedAccount, liked: ${clickedPostKeys.size}")
    }

    fun loadState(context: Context) {
        val prefs = context.getSharedPreferences("AutomationPrefs", Context.MODE_PRIVATE)
        // Load liked posts
        val savedPosts = prefs.getStringSet("liked_posts", mutableSetOf()) ?: mutableSetOf()
        clickedPostKeys.clear()
        clickedPostKeys.addAll(savedPosts)

        // Load selected account
        selectedAccount = prefs.getString("selected_account", null)
        Log.d("Controller", "Loaded state - account: $selectedAccount, liked: ${clickedPostKeys.size}")
    }
}