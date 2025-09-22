package com.example.goqiilogins

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
import android.text.InputType

class MainActivity : AppCompatActivity() {

    private val targetPackage = "com.betaout.GOQii"
    private lateinit var tableLayout: TableLayout
    private lateinit var btnAdd: Button
    private lateinit var btnLogin: Button
    private lateinit var btnSave: Button
    private lateinit var btnClear: Button
    private lateinit var btnScroll: Button

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

        // Initialize views
        tableLayout = findViewById(R.id.accountsTable)
        btnAdd = findViewById(R.id.btnAdd)
        btnLogin = findViewById(R.id.btnLogin)
        btnSave = findViewById(R.id.btnSave)
        btnClear = findViewById(R.id.btnClear)
        btnScroll = findViewById(R.id.btnScroll)

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
        val inputEditText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Enter text to scroll to"
        }

        AlertDialog.Builder(this)
            .setTitle("Scroll to Text")
            .setMessage("Enter the text you want to scroll to in GOQii app:")
            .setView(inputEditText)
            .setPositiveButton("OK") { dialog, _ ->
                val textToFind = inputEditText.text.toString().trim()
                if (textToFind.isNotEmpty()) {
                    startScrollProcess(textToFind)
                } else {
                    Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun startScrollProcess(textToFind: String) {
        MyAccessibilityServiceController.reset()

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        launchIntent?.let { startActivity(it) }

        Handler(Looper.getMainLooper()).postDelayed({
            MyAccessibilityServiceController.shouldScrollToText = true
            MyAccessibilityServiceController.textToScrollTo = textToFind
            Log.d("MainActivity", "Starting scroll for text: $textToFind")

            triggerAccessibilityService()
        }, 20)
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
        MyAccessibilityServiceController.reset()

        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        launchIntent?.let { startActivity(it) }

        Handler(Looper.getMainLooper()).postDelayed({
            MyAccessibilityServiceController.shouldClickSignIn = true
            MyAccessibilityServiceController.selectedAccount = account
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
}

object MyAccessibilityServiceController {
    var shouldClickSignIn = false
    var shouldClickBtnLogin = false
    var shouldSelectEmail = false
    var shouldScrollToText = false
    var selectedAccount: String? = null
    var textToScrollTo: String? = null  // Text to scroll to

    fun reset() {
        shouldClickSignIn = false
        shouldClickBtnLogin = false
        shouldSelectEmail = false
        shouldScrollToText = false
        selectedAccount = null
        textToScrollTo = null
    }
}