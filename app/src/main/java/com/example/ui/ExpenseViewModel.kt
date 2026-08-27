package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.io.File

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val syncManager: DriveSyncManager
) : ViewModel() {

    val userRoles = listOf("Staff", "Admin")
    private val _currentUserRole = MutableStateFlow("Guest")
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    fun setUserRole(role: String) {
        _currentUserRole.value = role
    }

    // Role detection helpers based on currently logged in user
    fun isStaff(): Boolean {
        val user = _loggedInUser.value ?: return false
        return user.role.equals("Staff", ignoreCase = true)
    }

    fun isManager(): Boolean {
        val user = _loggedInUser.value ?: return false
        return user.role.equals("Admin", ignoreCase = true)
    }

    fun isAdmin(): Boolean {
        val user = _loggedInUser.value ?: return false
        return user.role.equals("Admin", ignoreCase = true)
    }

    // Live Tracker Filter states for Advanced Reporting
    val selectedFilterCategory = MutableStateFlow("All")
    val selectedFilterStatus = MutableStateFlow("All")
    val selectedFilterTimeRange = MutableStateFlow("All Time")

    // Google Drive access token for backup upload
    private val _driveAccessToken = MutableStateFlow("")
    val driveAccessToken: StateFlow<String> = _driveAccessToken.asStateFlow()

    fun setDriveAccessToken(token: String) {
        _driveAccessToken.value = token
    }

    // Backup status messages
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    fun dismissSyncMessage() {
        _syncMessage.value = null
    }

    // Notification center messages (e.g. system notification alerts upon report or approval)
    private val _systemNotifications = MutableStateFlow<List<SystemNotification>>(emptyList())
    val systemNotifications: StateFlow<List<SystemNotification>> = _systemNotifications.asStateFlow()

    data class SystemNotification(
        val id: String = UUID.randomUUID().toString(),
        val title: String,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    fun postNotification(title: String, message: String) {
        _systemNotifications.update { current ->
            listOf(SystemNotification(title = title, message = message)) + current
        }
    }

    fun clearNotifications() {
        _systemNotifications.value = emptyList()
    }

    // Database state flows
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAllocations: StateFlow<List<BudgetAllocation>> = repository.allAllocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<Attendance>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Unified Live User Auth & Security States ---
    val allUsers: StateFlow<List<User>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _loggedInUser = MutableStateFlow<User?>(null)
    val loggedInUser: StateFlow<User?> = _loggedInUser.asStateFlow()

    val userScopedExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        _loggedInUser
    ) { expenses, user ->
        if (user == null) {
            emptyList()
        } else if (user.role.equals("Admin", ignoreCase = true)) {
            expenses
        } else {
            expenses.filter {
                it.staffName.equals(user.fullName, ignoreCase = true) ||
                it.staffName.equals(user.username, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userScopedAttendance: StateFlow<List<Attendance>> = combine(
        allAttendance,
        _loggedInUser
    ) { attendances, user ->
        if (user == null) {
            emptyList()
        } else if (user.role.equals("Admin", ignoreCase = true)) {
            attendances
        } else {
            attendances.filter {
                it.staffName.equals(user.fullName, ignoreCase = true) ||
                it.staffName.equals(user.username, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        userScopedExpenses,
        selectedFilterCategory,
        selectedFilterStatus,
        selectedFilterTimeRange
    ) { expenses, category, status, timeRange ->
        expenses.filter { expense ->
            val matchCategory = if (category == "All") true else expense.category == category
            val matchStatus = if (status == "All") true else expense.status == status
            val matchTime = when (timeRange) {
                "Today" -> isWithinDaysCount(expense.timestamp, 1)
                "This Week" -> isWithinDaysCount(expense.timestamp, 7)
                "This Month" -> isWithinDaysCount(expense.timestamp, 30)
                "Last 30 Days" -> isWithinDaysCount(expense.timestamp, 30)
                else -> true // All Time
            }
            matchCategory && matchStatus && matchTime
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun isWithinDaysCount(timestamp: Long, numDays: Int): Boolean {
        val now = System.currentTimeMillis()
        val elapsed = now - timestamp
        val margin = numDays * 24L * 60L * 60L * 1000L
        return elapsed in 0..margin
    }

    // Calculated fields (state) for Balance Sheet
    val startingBalance = 150000.0 // Base starting balance of Urdu business

    val balanceSheetState = combine(userScopedExpenses, allAllocations) { expenses, allocations ->
        val totalAllocations = allocations.sumOf { it.amount }
        val approvedExpenses = expenses.filter { it.status == "APPROVED" }.sumOf { it.amount }
        val pendingExpenses = expenses.filter { it.status == "PENDING" }.sumOf { it.amount }
        val remainingBalance = startingBalance + totalAllocations - approvedExpenses

        BalanceSheetStats(
            startingBalance = startingBalance,
            totalAllocations = totalAllocations,
            approvedExpenses = approvedExpenses,
            pendingExpenses = pendingExpenses,
            remainingBalance = remainingBalance
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BalanceSheetStats())

    data class BalanceSheetStats(
        val startingBalance: Double = 0.0,
        val totalAllocations: Double = 0.0,
        val approvedExpenses: Double = 0.0,
        val pendingExpenses: Double = 0.0,
        val remainingBalance: Double = 0.0
    )

    // User session persistence methods
    fun loadPersistedUser(context: Context) {
        val sp = context.getSharedPreferences("uk_security_prefs", Context.MODE_PRIVATE)
        val token = sp.getString("auth_token", "") ?: ""
        val username = sp.getString("logged_in_username", "") ?: ""
        if (token.isNotEmpty()) {
            viewModelScope.launch {
                val restored = repository.restoreSession(token)
                if (restored != null) {
                    _loggedInUser.value = restored
                    _currentUserRole.value = "${restored.role} (${restored.fullName})"
                    return@launch
                }
                if (username.isNotEmpty()) {
                    val user = repository.getUserByUsername(username)
                    if (user != null) {
                        _loggedInUser.value = user
                        _currentUserRole.value = "${user.role} (${user.fullName})"
                    }
                }
            }
        } else if (username.isNotEmpty()) {
            viewModelScope.launch {
                val user = repository.getUserByUsername(username)
                if (user != null) {
                    _loggedInUser.value = user
                    _currentUserRole.value = "${user.role} (${user.fullName})"
                }
            }
        }
    }

    fun loginUser(context: Context, usernameText: String, passwordText: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.loginRemoteOrLocal(usernameText.trim(), passwordText)
            result.fold(
                onSuccess = { user ->
                    _loggedInUser.value = user
                    _currentUserRole.value = "${user.role} (${user.fullName})"
                    val sp = context.getSharedPreferences("uk_security_prefs", Context.MODE_PRIVATE)
                    sp.edit()
                        .putString("logged_in_username", user.username)
                        .putString("auth_token", com.example.data.remote.ApiClient.bearer()?.removePrefix("Bearer ") ?: "")
                        .apply()
                    onResult(true, "Logged in via exp.ukssolution.com as ${user.fullName}!")
                    postNotification(
                        title = "Session Started",
                        message = "${user.fullName} logged in successfully."
                    )
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "Login failed")
                }
            )
        }
    }

    fun registerUser(context: Context, usernameText: String, passwordText: String, fullNameText: String, roleText: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val trimmedUsername = usernameText.trim()
            if (trimmedUsername.isEmpty() || passwordText.isEmpty() || fullNameText.trim().isEmpty()) {
                onResult(false, "All fields are required!")
                return@launch
            }
            val result = repository.registerRemoteOrLocal(
                username = trimmedUsername,
                password = passwordText,
                fullName = fullNameText.trim(),
                role = roleText
            )
            result.fold(
                onSuccess = { newUser ->
                    val sp = context.getSharedPreferences("uk_security_prefs", Context.MODE_PRIVATE)
                    sp.edit()
                        .putString("logged_in_username", newUser.username)
                        .putString("auth_token", com.example.data.remote.ApiClient.bearer()?.removePrefix("Bearer ") ?: "")
                        .apply()
                    onResult(true, "Registration successful for ${newUser.fullName}!")
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "Registration failed")
                }
            )
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            repository.deleteUserById(id)
            postNotification(title = "Account Removed", message = "Successfully deleted registered user ID $id.")
        }
    }

    fun logoutUser(context: Context) {
        _loggedInUser.value = null
        _currentUserRole.value = "Guest"
        repository.clearRemoteSession()
        val sp = context.getSharedPreferences("uk_security_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .remove("logged_in_username")
            .remove("auth_token")
            .apply()
        postNotification(
            title = "Session Terminated",
            message = "Signed out successfully."
        )
    }

    init {
        viewModelScope.launch {
            try {
                repository.allUsers.firstOrNull()?.let { users ->
                    if (users.isEmpty()) {
                        // Local offline seeds only — production users live in server MySQL
                        repository.insertUser(User(username = "admin", password = "123", fullName = "UK Admin", role = "Admin"))
                        repository.insertUser(User(username = "staff", password = "123", fullName = "Staff Ahmed", role = "Staff"))
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        postNotification(
            title = "Khush Amdeed! (Welcome)",
            message = "Connected to exp.ukssolution.com when online. Offline Room cache remains available."
        )
    }

    // Actions for Expenses
    fun addExpense(
        amount: Double,
        category: String,
        description: String,
        staffName: String,
        receiptUri: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        locationAddress: String? = null
    ) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                category = category,
                description = description,
                staffName = staffName,
                status = "PENDING",
                receiptUri = receiptUri,
                latitude = latitude,
                longitude = longitude,
                locationAddress = locationAddress
            )
            repository.insertExpense(expense)
            postNotification(
                title = "New Expense Submitted",
                message = "Staff $staffName submitted Rs. ${String.format("%,.0f", amount)} for $category of UK Security at $locationAddress ($latitude, $longitude)."
            )
        }
    }

    // Actions for Attendance (Haziri Register)
    fun markAttendance(type: String, staffName: String, lat: Double, lng: Double, address: String) {
        viewModelScope.launch {
            val attendance = Attendance(
                staffName = staffName,
                type = type,
                latitude = lat,
                longitude = lng,
                locationAddress = address
            )
            repository.insertAttendance(attendance)
            postNotification(
                title = "Attendance Marked ($type)",
                message = "$staffName successfully marked attendance status: $type at $address ($lat, $lng)."
            )
        }
    }

    fun deleteAttendanceRecord(id: Long) {
        viewModelScope.launch {
            repository.deleteAttendanceById(id)
            postNotification(
                title = "Attendance Entry Removed",
                message = "An attendance logging record has been purged."
            )
        }
    }

    // Advanced CSV & PDF formatting and local file export
    fun exportFilteredExpensesToCSV(context: Context): String {
        val list = filteredExpenses.value
        if (list.isEmpty()) {
            return "No data to export based on current filters!"
        }
        val csv = StringBuilder()
        csv.append("ID,Staff Name,Category,Description,Amount (Rs.),Status,Date,Latitude,Longitude,Address\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        for (item in list) {
            val dateStr = sdf.format(Date(item.timestamp))
            val safeDesc = item.description.replace(",", ";")
            val safeAddr = (item.locationAddress ?: "Unknown").replace(",", ";")
            csv.append("${item.id},${item.staffName},${item.category},$safeDesc,${item.amount},${item.status},$dateStr,${item.latitude ?: 0.0},${item.longitude ?: 0.0},$safeAddr\n")
        }
        
        try {
            val file = File(context.filesDir, "uk_security_expenses_export.csv")
            file.writeText(csv.toString())
            postNotification(
                title = "CSV Export Downloaded",
                message = "CSV report saved containing ${list.size} records to: ${file.name}"
            )
            return file.absolutePath
        } catch (e: Exception) {
            return "Export failed: ${e.localizedMessage}"
        }
    }

    fun exportFilteredExpensesToPDF(context: Context): String {
        val list = filteredExpenses.value
        if (list.isEmpty()) {
            return "No data to export!"
        }
        val pdfMock = StringBuilder()
        pdfMock.append("=========================================\n")
        pdfMock.append("       UK SECURITY SOLUTIONS LTD.        \n")
        pdfMock.append("         EXPENSE MONITOR REPORT          \n")
        pdfMock.append("=========================================\n\n")
        pdfMock.append("Date of Export: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())}\n")
        pdfMock.append("Filters:\n")
        pdfMock.append("  - Category: ${selectedFilterCategory.value}\n")
        pdfMock.append("  - Status: ${selectedFilterStatus.value}\n")
        pdfMock.append("  - Range: ${selectedFilterTimeRange.value}\n\n")
        pdfMock.append("-----------------------------------------\n")
        var grandTotal = 0.0
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH)
        for ((idx, item) in list.withIndex()) {
            val dateStr = sdf.format(Date(item.timestamp))
            pdfMock.append("${idx + 1}. [${item.status}] ${item.staffName} - Rs. ${String.format("%,.0f", item.amount)}\n")
            pdfMock.append("   Cat: ${item.category} | Desc: ${item.description} | Date: $dateStr\n")
            if (item.locationAddress != null) {
                pdfMock.append("   GPS: ${item.locationAddress} (${item.latitude}, ${item.longitude})\n")
            }
            pdfMock.append("\n")
            grandTotal += item.amount
        }
        pdfMock.append("-----------------------------------------\n")
        pdfMock.append("GRAND TOTAL EXPENSES: Rs. ${String.format("%,.0f", grandTotal)}\n")
        pdfMock.append("=========================================\n")
        pdfMock.append("CONFIDENTIAL PORTAL EXPORT\n")

        try {
            val file = File(context.filesDir, "uk_security_report.pdf")
            file.writeText(pdfMock.toString())
            postNotification(
                title = "PDF Documents Exported",
                message = "High-fidelity text PDF statement saved: ${file.name}"
            )
            return file.absolutePath
        } catch (e: Exception) {
            return "Export failed: ${e.localizedMessage}"
        }
    }

    fun approveExpense(expenseId: Long, adminNotes: String? = null) {
        viewModelScope.launch {
            val expense = allExpenses.value.find { it.id == expenseId }
            if (expense != null) {
                val updated = expense.copy(
                    status = "APPROVED",
                    adminNotes = adminNotes,
                    approvalTimestamp = System.currentTimeMillis()
                )
                repository.updateExpense(updated)
                
                // Automatically generate system notification and update Balance sheet
                postNotification(
                    title = "Expense Approved (Mansoor)",
                    message = "Approved Rs. ${String.format("%,.2f", expense.amount)} for category text: ${expense.category}. Balance Sheet recalculated automatically."
                )
            }
        }
    }

    fun rejectExpense(expenseId: Long, adminNotes: String) {
        viewModelScope.launch {
            val expense = allExpenses.value.find { it.id == expenseId }
            if (expense != null) {
                val updated = expense.copy(
                    status = "REJECTED",
                    adminNotes = adminNotes,
                    approvalTimestamp = System.currentTimeMillis()
                )
                repository.updateExpense(updated)
                postNotification(
                    title = "Expense Rejected (Na-manzoor)",
                    message = "Submited expense of Rs. ${String.format("%,.2f", expense.amount)} by ${expense.staffName} was rejected: $adminNotes"
                )
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    // Actions for Budget Allocations
    fun addAllocation(amount: Double, description: String) {
        viewModelScope.launch {
            val allocation = BudgetAllocation(
                amount = amount,
                description = description
            )
            repository.insertAllocation(allocation)
            postNotification(
                title = "Budget Addition Added",
                message = "Admin allocated Rs. ${String.format("%,.2f", amount)} to balance sheet: $description"
            )
        }
    }

    // Monthly Report Generator with Category summaries
    data class MonthlyReport(
        val monthString: String, // e.g., "May 2026"
        val totalApprovedAmount: Double,
        val totalRejectedAmount: Double,
        val expensesByCategory: List<CategorySummary>,
        val size: Int
    )

    data class CategorySummary(
        val categoryId: String,
        val categoryName: String,
        val amount: Double,
        val percentage: Float
    )

    fun generateMonthlyReport(monthOffset: Int): MonthlyReport {
        // Filter approved/rejected expenses matching the queried month
        val format = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthOffset) // 0 for current, 1 for last month, etc.
        val targetMonthString = format.format(cal.time)

        val matchingExpenses = allExpenses.value.filter {
            val expenseMonth = format.format(Date(it.timestamp))
            expenseMonth == targetMonthString
        }

        val approvedExpenses = matchingExpenses.filter { it.status == "APPROVED" }
        val totalApproved = approvedExpenses.sumOf { it.amount }
        val totalRejected = matchingExpenses.filter { it.status == "REJECTED" }.sumOf { it.amount }

        // Group by category (20 Categories)
        val categorySummaryMap = approvedExpenses.groupBy { it.category }
        val summaryList = ExpenseCategories.list.map { category ->
            val sum = categorySummaryMap[category.id]?.sumOf { it.amount } ?: 0.0
            val percentage = if (totalApproved > 0) (sum / totalApproved * 100).toFloat() else 0f
            CategorySummary(
                categoryId = category.id,
                categoryName = category.nameEnglish,
                amount = sum,
                percentage = percentage
            )
        }.sortedByDescending { it.amount }

        return MonthlyReport(
            monthString = targetMonthString,
            totalApprovedAmount = totalApproved,
            totalRejectedAmount = totalRejected,
            expensesByCategory = summaryList,
            size = matchingExpenses.size
        )
    }

    // Google Drive Sync Actions
    fun performOfflineBackup() {
        viewModelScope.launch {
            try {
                val file = syncManager.saveBackupLocally()
                _syncMessage.value = "Offline Backup Created Successfully:\n${file.name}\nSize: ${file.length()} bytes"
                postNotification(
                    title = "Offline Backup Complete",
                    message = "Saved a local JSON copy containing ${allExpenses.value.size} transactions to cache."
                )
            } catch (e: Exception) {
                _syncMessage.value = "Local Backup failed: ${e.localizedMessage}"
            }
        }
    }

    fun performGoogleDriveSync() {
        val token = _driveAccessToken.value
        if (token.isBlank()) {
            _syncMessage.value = "Please enter or request a Google OAuth Bearer Token in the sync settings first!"
            return
        }
        viewModelScope.launch {
            _syncMessage.value = "Uploading backup JSON snapshot to Google Drive appDataFolder..."
            val result = syncManager.syncWithGoogleDrive(token)
            result.onSuccess { message ->
                _syncMessage.value = message
                postNotification(title = "Google Drive Backed-Up", message = "Offline transactions uploaded to Google Cloud space.")
            }.onFailure { error ->
                _syncMessage.value = "Upload Error: ${error.localizedMessage}\n(Check if your developer OAuth access token is valid and active)"
            }
        }
    }
}
