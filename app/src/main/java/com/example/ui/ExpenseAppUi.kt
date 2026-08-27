package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.compose.ui.graphics.drawscope.Stroke
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAppUi(viewModel: ExpenseViewModel) {
    val context = LocalContext.current

    // Session control validation
    LaunchedEffect(Unit) {
        viewModel.loadPersistedUser(context)
    }

    val loggedInUser by viewModel.loggedInUser.collectAsState()
    if (loggedInUser == null) {
        LoginRegisterScreen(viewModel = viewModel)
        return
    }

    var currentTab by remember { mutableStateOf(0) }
    val currentRole by viewModel.currentUserRole.collectAsState()
    val systemNotifications by viewModel.systemNotifications.collectAsState()
    
    // Toggle notifications panel dialog
    var showNotifDialog by remember { mutableStateOf(false) }
    var showUserMgmtDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { showUserMgmtDialog = true },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Account avatar indicator displaying official company logo
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .size(width = 56.dp, height = 36.dp)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_uk_logo_1780051906949),
                                    contentDescription = "UK Security Logo",
                                    modifier = Modifier.fillMaxSize().padding(2.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "UK Security Solutions",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "ADMIN PORTAL & STAFF HAZIRI SYSTEM",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showNotifDialog = true },
                        modifier = Modifier.testTag("bell_notification_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (systemNotifications.isNotEmpty()) {
                                    Badge {
                                        Text(systemNotifications.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "System alerts",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    // User selector / Profile trigger
                    Box {
                        TextButton(
                            onClick = { showUserMgmtDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("role_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = loggedInUser?.fullName?.split(" ")?.firstOrNull() ?: "User",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Overview") },
                    label = { Text("Overview") },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Submit") },
                    label = { Text("Submit") },
                    modifier = Modifier.testTag("tab_submit")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.BackHand, contentDescription = "Haziri") },
                    label = { Text("Haziri") },
                    modifier = Modifier.testTag("tab_haziri")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Reports") },
                    label = { Text("Reports") },
                    modifier = Modifier.testTag("tab_reports")
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.CloudSync, contentDescription = "Sync Settings") },
                    label = { Text("Sync") },
                    modifier = Modifier.testTag("tab_sync")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (currentTab) {
                0 -> DashboardScreen(viewModel = viewModel)
                1 -> SubmitExpenseScreen(viewModel = viewModel)
                2 -> AttendanceScreen(viewModel = viewModel)
                3 -> ReportsScreen(viewModel = viewModel)
                4 -> SettingsSyncScreen(viewModel = viewModel)
            }

            // Notification Center Modal
            if (showNotifDialog) {
                NotificationCenterDialog(
                    notifications = systemNotifications,
                    onDismiss = { showNotifDialog = false },
                    onClear = { viewModel.clearNotifications() }
                )
            }

            // User Profile / Admin Management Dialog
            if (showUserMgmtDialog) {
                UserMgmtDialog(
                    viewModel = viewModel,
                    onDismiss = { showUserMgmtDialog = false }
                )
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(viewModel: ExpenseViewModel) {
    val expenses by viewModel.userScopedExpenses.collectAsState()
    val stats by viewModel.balanceSheetState.collectAsState()
    val currentRole by viewModel.currentUserRole.collectAsState()
    val context = LocalContext.current

    // Dialog for Admin decision (Approve/Reject) on click
    var selectedExpenseForAction by remember { mutableStateOf<Expense?>(null) }
    var showAllocationDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Balance Sheet Summary Card (Dynamic Updates)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BALANCE SHEET (میزانیہ)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Rs. ${String.format("%,.0f", stats.remainingBalance)}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Available Budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Starting & Inflows",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rs. ${String.format("%,.0f", stats.startingBalance + stats.totalAllocations)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Approved Debits",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rs. ${String.format("%,.0f", stats.approvedExpenses)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Admin only Allocation trigger
                    if (currentRole.contains("Admin")) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showAllocationDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("allocate_budget_button")
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Budget allocation", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Live Real-Time Dashboard Status Counters
        item {
            Text(
                text = "Live Approval Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardMetricCard(
                    title = "Pending",
                    value = expenses.count { it.status == "PENDING" }.toString(),
                    amount = expenses.filter { it.status == "PENDING" }.sumOf { it.amount },
                    color = Color(0xFFFFB300), // Amber
                    modifier = Modifier.weight(1f)
                )
                DashboardMetricCard(
                    title = "Approved",
                    value = expenses.count { it.status == "APPROVED" }.toString(),
                    amount = stats.approvedExpenses,
                    color = Color(0xFF4CAF50), // Green
                    modifier = Modifier.weight(1f)
                )
                DashboardMetricCard(
                    title = "Rejected",
                    value = expenses.count { it.status == "REJECTED" }.toString(),
                    amount = expenses.filter { it.status == "REJECTED" }.sumOf { it.amount },
                    color = Color(0xFFF44336), // Red
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Transactions List View
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Expense Filings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total ${expenses.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (expenses.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Expenses Logged Yet (کوئی اندراج نہیں ہے)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tap on the 'Submit' tab to enter a staff submittal",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(expenses) { expense ->
                ExpenseItemCard(
                    expense = expense,
                    onClick = {
                        // Admin can manage, staff can see details
                        selectedExpenseForAction = expense
                    }
                )
            }
        }
    }

    // Allocation Dialog Builder
    if (showAllocationDialog) {
        var allocationAmount by remember { mutableStateOf("") }
        var allocationDescription by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAllocationDialog = false },
            title = { Text("Allocate Budget Funds (میزانیہ اضافہ)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter dynamic allocation to increase the business balance sheet pool:")
                    OutlinedTextField(
                        value = allocationAmount,
                        onValueChange = { allocationAmount = it },
                        label = { Text("Injected Amount (Rs.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("allocation_amount_input")
                    )
                    OutlinedTextField(
                        value = allocationDescription,
                        onValueChange = { allocationDescription = it },
                        label = { Text("Allocation Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("allocation_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = allocationAmount.toDoubleOrNull()
                        if (amt != null && amt > 0 && allocationDescription.isNotBlank()) {
                            viewModel.addAllocation(amt, allocationDescription)
                            showAllocationDialog = false
                        } else {
                            Toast.makeText(context, "Please enter valid values!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("confirm_allocation_btn")
                ) {
                    Text("Inject Funds")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAllocationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Expense Detail & Approval Dialog
    selectedExpenseForAction?.let { expense ->
        val cat = ExpenseCategories.list.find { it.id == expense.category }
        var adminNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedExpenseForAction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(cat?.icon ?: Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${cat?.nameEnglish ?: "Expense"} - Detail")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Staff filer:", fontWeight = FontWeight.Bold)
                        Text(expense.staffName)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Amount (Rs.):", fontWeight = FontWeight.Bold)
                        Text("Rs. ${String.format("%,.2f", expense.amount)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Submitted date:", fontWeight = FontWeight.Bold)
                        Text(SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(expense.timestamp)))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Filer Category:", fontWeight = FontWeight.Bold)
                        Text(cat?.nameEnglish ?: expense.category)
                    }
                    Text("Filer Description:", fontWeight = FontWeight.Bold)
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Receipt Image Upload (Status):", fontWeight = FontWeight.Bold)
                    if (expense.receiptUri != null) {
                        Text("✓ Attached (Local Cached File: ${expense.receiptUri.substringAfterLast("/")})", color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                    } else {
                        Text("No Receipt Attached", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }

                    if (expense.status != "PENDING") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current status:", fontWeight = FontWeight.Bold)
                            FilingStatusBadge(status = expense.status)
                        }
                        if (!expense.adminNotes.isNullOrBlank()) {
                            Text("Management Remarks:", fontWeight = FontWeight.Bold)
                            Text(expense.adminNotes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        if (currentRole.contains("Admin")) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = adminNotes,
                                onValueChange = { adminNotes = it },
                                label = { Text("Management Notes / Remarks") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_notes_input")
                            )
                        } else {
                            Text("Pending approval by Management (Muntazim).", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFB300))
                        }
                    }
                }
            },
            confirmButton = {
                if (expense.status == "PENDING" && currentRole.contains("Admin")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                viewModel.rejectExpense(expense.id, adminNotes.ifBlank { "Rejected by Admin" })
                                selectedExpenseForAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("reject_btn")
                        ) {
                            Text("Reject (نا منظور)")
                        }
                        Button(
                            onClick = {
                                viewModel.approveExpense(expense.id, adminNotes.ifBlank { "Approved" })
                                selectedExpenseForAction = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.testTag("approve_btn")
                        ) {
                            Text("Approve (منظور)")
                        }
                    }
                } else {
                    Button(
                        onClick = { selectedExpenseForAction = null }
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }
}

// Helpers for Dashboard Metric Card
@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val containerColor = when (title) {
        "Pending" -> MaterialTheme.colorScheme.primaryContainer
        "Approved" -> com.example.ui.theme.GreenBg
        "Rejected" -> com.example.ui.theme.OrangeBg
        else -> color.copy(alpha = 0.12f)
    }
    
    val contentColor = when (title) {
        "Pending" -> MaterialTheme.colorScheme.onPrimaryContainer
        "Approved" -> com.example.ui.theme.GreenText
        "Rejected" -> com.example.ui.theme.OrangeText
        else -> color
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = contentColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = contentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rs. ${String.format("%,.0f", amount)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}

// Helpers for Single Expense Card Design
@Composable
fun ExpenseItemCard(expense: Expense, onClick: () -> Unit) {
    val cat = ExpenseCategories.list.find { it.id == expense.category }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category custom vector Icon badge in rounded squircle
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = cat?.icon ?: Icons.Default.Category,
                    contentDescription = cat?.nameEnglish,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = expense.staffName.split(" ")[0],
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("•", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                    Text(
                        text = cat?.nameEnglish ?: expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Rs. ${String.format("%,.0f", expense.amount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                FilingStatusBadge(status = expense.status)
            }
        }
    }
}

@Composable
fun FilingStatusBadge(status: String) {
    val (bCol, tCol, text) = when (status) {
        "APPROVED" -> Triple(com.example.ui.theme.GreenBg, com.example.ui.theme.GreenText, "APPROVED")
        "REJECTED" -> Triple(com.example.ui.theme.OrangeBg, com.example.ui.theme.OrangeText, "REJECTED")
        else -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, "PENDING")
    }

    Surface(
        color = bCol,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(0.5.dp, tCol.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontSize = 9.sp
            ),
            color = tCol,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

// Dialog for App notifications list
@Composable
fun NotificationCenterDialog(
    notifications: List<ExpenseViewModel.SystemNotification>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications Feed",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = onClear) {
                        Text("Clear All")
                    }
                }
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No new updates. All expenses and reports up-to-date.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications) { notif ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Text(
                                        text = notif.title,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(notif.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close Panel")
                }
            }
        }
    }
}


// ==========================================
// 2. STAFF EXPENSE SUBMIT PORTAL SCREEN (OLD)
// ==========================================
@Composable
fun SubmitExpenseScreenOld(viewModel: ExpenseViewModel) {
    val currentRole by viewModel.currentUserRole.collectAsState()
    val context = LocalContext.current

    // Fields
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var mockReceiptAttached by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "NEW STAFF EXPENSE SUBMITTAL",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter raw expense details below. Transactions are cached offline and synced automatically to the manager's approval feed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Expense Amount (Rs.) - رقم") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_amount_input")
            )
        }

        item {
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text("Filing Description (تفصیل)") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_desc_input")
            )
        }

        // Mock Receipt Capture attachment (Using Google Drive specifications)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Capture Image Receipt", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (mockReceiptAttached) "✓ Image Captured for Drive Cloud Storage" else "Offline capture & Backup",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (mockReceiptAttached) com.example.ui.theme.GreenText else Color.Gray
                        )
                    }
                    Button(
                        onClick = {
                            mockReceiptAttached = !mockReceiptAttached
                            val msg = if (mockReceiptAttached) "Receipt attached and cached for synchronization" else "Receipt detached"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        colors = if (mockReceiptAttached) ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.GreenBg, contentColor = com.example.ui.theme.GreenText) else ButtonDefaults.buttonColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("attach_receipt_btn")
                    ) {
                        Icon(imageVector = if (mockReceiptAttached) Icons.Default.CheckCircle else Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (mockReceiptAttached) "Attached" else "Capture")
                    }
                }
            }
        }

        // Category selection Grid (MUST show category names & Urdu translations)
        item {
            Text(
                text = "Select Expense Category (pant ki category)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            Box(modifier = Modifier.height(260.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(ExpenseCategories.list) { category ->
                        val isSelected = selectedCategory?.id == category.id
                        val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        val containerCol = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

                        Card(
                            onClick = { selectedCategory = category },
                            colors = CardDefaults.cardColors(containerColor = containerCol),
                            border = BorderStroke(2.dp, borderCol),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = category.nameEnglish,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = category.nameUrdu,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Submit Button
        item {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    val desc = descriptionText
                    val cat = selectedCategory
                    val staffName = currentRole

                    if (amt != null && amt > 0 && desc.isNotBlank() && cat != null) {
                        val receiptPath = if (mockReceiptAttached) "cached_receipt_drive_${System.currentTimeMillis()}.png" else null
                        viewModel.addExpense(
                            amount = amt,
                            category = cat.id,
                            description = desc,
                            staffName = staffName,
                            receiptUri = receiptPath
                        )
                        // Reset forms
                        amountText = ""
                        descriptionText = ""
                        selectedCategory = null
                        mockReceiptAttached = false

                        Toast.makeText(context, "Filing submitted! Recalculating dashboard...", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Mekari verify: Please fill amount, description, and choose one of the 20 categories!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_expense_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Submit Filing for Approval", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}


// ==========================================
// 2. STAFF EXPENSE SUBMIT PORTAL SCREEN
// ==========================================
data class Station(val name: String, val lat: Double, val lng: Double)

val simulatedStations = listOf(
    Station("Karachi Port Base - Port Area Guard", 24.8607, 67.0011),
    Station("Islamabad Headquarters - Diplomatic Post", 33.6844, 73.0479),
    Station("Lahore Gulberg Perimeter - Sector 4", 31.5204, 74.3587),
    Station("Rawalpindi Cantt Base - Depot 2", 33.5984, 73.0441),
    Station("Peshawar Khyber Gate Patrol Post", 34.0151, 71.5249)
)

val chartColors = listOf(
    Color(0xFF6750A4), Color(0xFF03A9F4), Color(0xFFE91E63), Color(0xFF4CAF50),
    Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFFE53935), Color(0xFF009688),
    Color(0xFF3F51B5), Color(0xFFCDDC39), Color(0xFF795548), Color(0xFF607D8B),
    Color(0xFFFFC107), Color(0xFF9E9E9E), Color(0xFF8BC34A), Color(0xFF00ABCD)
)

@Composable
fun SubmitExpenseScreen(viewModel: ExpenseViewModel) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val context = LocalContext.current

    // Fields
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var mockReceiptAttached by remember { mutableStateOf(false) }

    // Live GPS state variables
    var liveLat by remember { mutableStateOf(24.8607) } // fallback to Karachi
    var liveLng by remember { mutableStateOf(67.0011) }
    var liveAddress by remember { mutableStateOf("Locating device...") }
    var isLocating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLocating = true
        LocationHelper.fetchLiveLocation(context) { lat, lng, addr ->
            if (lat != 0.0 || lng != 0.0) {
                liveLat = lat
                liveLng = lng
                liveAddress = addr
            } else {
                liveAddress = "Lahore HQ Perimeter Gate (Simulated)"
            }
            isLocating = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "NEW STAFF EXPENSE SUBMITTAL",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter raw expense details below. Transactions are saved locally and live GPS coordinates are tracked to verify guards presence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Live GPS Tracking Block instead of dropdown
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text(
                                text = "Live Verified GPS Tracking (لائیو لوکیشن)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Device live GPS is query-verified at filing submission so administrators can track secure check-in presence.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = liveAddress,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Coordinates: $liveLat, $liveLng",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(
                            onClick = {
                                isLocating = true
                                LocationHelper.fetchLiveLocation(context) { lat, lng, addr ->
                                    if (lat != 0.0 || lng != 0.0) {
                                        liveLat = lat
                                        liveLng = lng
                                        liveAddress = addr
                                    }
                                    isLocating = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Live GPS", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Expense Amount (Rs.) - رقم") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_amount_input")
            )
        }

        item {
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text("Filing Description (تفصیل)") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_desc_input")
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Capture Image Receipt", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (mockReceiptAttached) "✓ Image Captured for Drive Cloud Storage" else "Offline capture & Backup",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (mockReceiptAttached) com.example.ui.theme.GreenText else Color.Gray
                        )
                    }
                    Button(
                        onClick = {
                            mockReceiptAttached = !mockReceiptAttached
                            val msg = if (mockReceiptAttached) "Receipt attached and cached for synchronization" else "Receipt detached"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        colors = if (mockReceiptAttached) ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.GreenBg, contentColor = com.example.ui.theme.GreenText) else ButtonDefaults.buttonColors(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("attach_receipt_btn")
                    ) {
                        Icon(imageVector = if (mockReceiptAttached) Icons.Default.CheckCircle else Icons.Default.CameraAlt, contentDescription = null)
                    }
                }
            }
        }

        // 20 Categories Selector Sheet Grid Selection
        item {
            Text(
                text = "Select Expense Category (زمرہ)",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                letterSpacing = 0.5.sp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ExpenseCategories.list) { category ->
                        val isSelected = selectedCategory?.id == category.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            shape = RoundedCornerShape(16.dp),
                            onClick = { selectedCategory = category },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(10.dp)
                                    .fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = category.nameEnglish,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = category.nameUrdu,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Submit Button
        item {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    val desc = descriptionText
                    val cat = selectedCategory
                    val staffName = loggedInUser?.fullName ?: "Staff Guard"

                    if (amt != null && amt > 0 && desc.isNotBlank() && cat != null) {
                        val receiptPath = if (mockReceiptAttached) "cached_receipt_drive_${System.currentTimeMillis()}.png" else null
                        viewModel.addExpense(
                            amount = amt,
                            category = cat.id,
                            description = desc,
                            staffName = staffName,
                            receiptUri = receiptPath,
                            latitude = liveLat,
                            longitude = liveLng,
                            locationAddress = liveAddress
                        )
                        // Reset forms
                        amountText = ""
                        descriptionText = ""
                        selectedCategory = null
                        mockReceiptAttached = false

                        Toast.makeText(context, "Filing submitted! Recalculating dashboard...", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Mekari verify: Please fill amount, description, and choose one of the 20 categories!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_expense_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Submit Filing for Approval", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}


// ==========================================
// 3. STAFF ATTENDANCE / HAZIRI REGISTER SCREEN
// ==========================================
@Composable
fun AttendanceScreen(viewModel: ExpenseViewModel) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val attendanceLogs by viewModel.userScopedAttendance.collectAsState()
    val context = LocalContext.current
    val isStaff = viewModel.isStaff()

    // Live GPS state variables
    var liveLat by remember { mutableStateOf(24.8607) } // fallback to Karachi
    var liveLng by remember { mutableStateOf(67.0011) }
    var liveAddress by remember { mutableStateOf("Locating device...") }
    var isLocating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLocating = true
        LocationHelper.fetchLiveLocation(context) { lat, lng, addr ->
            if (lat != 0.0 || lng != 0.0) {
                liveLat = lat
                liveLng = lng
                liveAddress = addr
            } else {
                liveAddress = "Lahore HQ Perimeter Gate (Simulated)"
            }
            isLocating = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECURITY ATTENDANCE MODULE",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelMedium,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "UK Security Solutions guards must mark Duty-In upon shift start and Duty-Out on completion. Local database stores coordinates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Punch Actions Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Mark Haziri (حاضری لگائیں)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "1. Verified Live GPS Location:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = liveAddress,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Coordinates: $liveLat, $liveLng",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        IconButton(
                            onClick = {
                                isLocating = true
                                LocationHelper.fetchLiveLocation(context) { lat, lng, addr ->
                                    if (lat != 0.0 || lng != 0.0) {
                                        liveLat = lat
                                        liveLng = lng
                                        liveAddress = addr
                                    }
                                    isLocating = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Live GPS", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "2. Punch Shift Action:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.markAttendance(
                                    type = "DUTY IN (Aamad)",
                                    staffName = loggedInUser?.fullName ?: "Staff Guard",
                                    lat = liveLat,
                                    lng = liveLng,
                                    address = liveAddress
                                )
                                Toast.makeText(context, "Shift IN Stamped successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.GreenBg, contentColor = com.example.ui.theme.GreenText),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("punch_in_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aamad (IN)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.markAttendance(
                                    type = "DUTY OUT (Rukhsat)",
                                    staffName = loggedInUser?.fullName ?: "Staff Guard",
                                    lat = liveLat,
                                    lng = liveLng,
                                    address = liveAddress
                                )
                                Toast.makeText(context, "Shift OUT Stamped successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp).testTag("punch_out_btn")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rukhsat (OUT)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Timeline History Section Header
        item {
            Text(
                text = "Attendance Ledger Logs (حاضری رجسٹر)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (attendanceLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No attendance logs found.", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Gray)
                        Text("Stamped entries will compile here sequentially.", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        } else {
            items(attendanceLogs) { log ->
                val isCheckIn = log.type.contains("IN")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    color = if (isCheckIn) com.example.ui.theme.GreenBg else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isCheckIn) "IN" else "OUT",
                                        color = if (isCheckIn) com.example.ui.theme.GreenText else Color(0xFFC62828),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = log.staffName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.locationAddress,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "GPS Coords: ${log.latitude}, ${log.longitude}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            if (!isStaff) {
                                IconButton(onClick = { viewModel.deleteAttendanceRecord(log.id) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Log", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. ADVANCED REPORTING SCREEN
// ==========================================
@Composable
fun CanvasSpendChart(nonZeroSummaries: List<com.example.ui.ExpenseViewModel.CategorySummary>) {
    val totalAmount = nonZeroSummaries.sumOf { it.amount }
    if (totalAmount <= 0) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Compose Native Circular Donut Gauge
        Canvas(
            modifier = Modifier
                .size(110.dp)
                .padding(6.dp)
        ) {
            var startAngle = -90f
            nonZeroSummaries.forEachIndexed { index, summary ->
                val sweepAngle = (summary.amount / totalAmount * 360f).toFloat()
                val color = chartColors[index % chartColors.size]
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx())
                )
                startAngle += sweepAngle
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Interative categories cost index
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            nonZeroSummaries.take(4).forEachIndexed { index, summary ->
                val color = chartColors[index % chartColors.size]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                    Text(
                        text = "${summary.categoryName}: ₨ ${String.format("%,.0f", summary.amount)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (nonZeroSummaries.size > 4) {
                Text(
                    text = "+ ${nonZeroSummaries.size - 4} other categories loaded",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ReportsScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val currentRole by viewModel.currentUserRole.collectAsState()
    val isStaff = viewModel.isStaff()

    // Filter properties Collect Flow States
    val selectedCat by viewModel.selectedFilterCategory.collectAsState()
    val selectedStatus by viewModel.selectedFilterStatus.collectAsState()
    val selectedTimeRange by viewModel.selectedFilterTimeRange.collectAsState()

    val filteredList by viewModel.filteredExpenses.collectAsState()

    // Dropdown toggle states
    var showCatDropdown by remember { mutableStateOf(false) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showTimeDropdown by remember { mutableStateOf(false) }

    // Access lock if Staff checks this tab (RBAC implementation)
    if (isStaff) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "🔒 Role Protected",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🔒 REGULATION ACCESS LOCKED",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You are currently Switched to Staff context ($currentRole). Advanced financial tables, donut spend graphs, and PDF audit records are only accessible to Manager (Mansoor) or Admin credentials.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "(Select 'Manager' or 'Admin' in the top account bar to audit report)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title block
        item {
            Text(
                text = "Advanced Cost Analysis (ماہانہ آڈٹ رپورٹ)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "UK Security Solutions Ltd. Corporate Expenses Summary",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Expanded Filter Options Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "FILTER AUDIT MATRIX",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Category filter
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showCatDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (selectedCat == "All") "All Categories" else "Cat: $selectedCat",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                            DropdownMenu(expanded = showCatDropdown, onDismissRequest = { showCatDropdown = false }) {
                                DropdownMenuItem(text = { Text("All Categories") }, onClick = { viewModel.selectedFilterCategory.value = "All"; showCatDropdown = false })
                                ExpenseCategories.list.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.nameEnglish) },
                                        onClick = {
                                            viewModel.selectedFilterCategory.value = category.id
                                            showCatDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Status filter
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showStatusDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Status: $selectedStatus",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                            DropdownMenu(expanded = showStatusDropdown, onDismissRequest = { showStatusDropdown = false }) {
                                DropdownMenuItem(text = { Text("All Statuses") }, onClick = { viewModel.selectedFilterStatus.value = "All"; showStatusDropdown = false })
                                DropdownMenuItem(text = { Text("PENDING") }, onClick = { viewModel.selectedFilterStatus.value = "PENDING"; showStatusDropdown = false })
                                DropdownMenuItem(text = { Text("APPROVED") }, onClick = { viewModel.selectedFilterStatus.value = "APPROVED"; showStatusDropdown = false })
                                DropdownMenuItem(text = { Text("REJECTED") }, onClick = { viewModel.selectedFilterStatus.value = "REJECTED"; showStatusDropdown = false })
                            }
                        }

                        // 3. Date range filter
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showTimeDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedTimeRange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                            DropdownMenu(expanded = showTimeDropdown, onDismissRequest = { showTimeDropdown = false }) {
                                DropdownMenuItem(text = { Text("All Time") }, onClick = { viewModel.selectedFilterTimeRange.value = "All Time"; showTimeDropdown = false })
                                DropdownMenuItem(text = { Text("Today") }, onClick = { viewModel.selectedFilterTimeRange.value = "Today"; showTimeDropdown = false })
                                DropdownMenuItem(text = { Text("This Week") }, onClick = { viewModel.selectedFilterTimeRange.value = "This Week"; showTimeDropdown = false })
                                DropdownMenuItem(text = { Text("This Month") }, onClick = { viewModel.selectedFilterTimeRange.value = "This Month"; showTimeDropdown = false })
                                DropdownMenuItem(text = { Text("Last 30 Days") }, onClick = { viewModel.selectedFilterTimeRange.value = "Last 30 Days"; showTimeDropdown = false })
                            }
                        }
                    }
                }
            }
        }

        // Summary Calculations Row Cards
        item {
            val totalFilteredSum = filteredList.sumOf { it.amount }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL MATCHED ACCRUALS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                        Text(
                            text = "Rs. ${String.format("%,.0f", totalFilteredSum)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Calculated from ${filteredList.size} filings found", fontSize = 11.sp, color = Color.Gray)
                    }

                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                }
            }
        }

        // Donut Spending graph block
        item {
            Text(
                text = "Spending Category Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                val groupMap = filteredList.groupBy { it.category }
                val graphData = ExpenseCategories.list.map { category ->
                    val sum = groupMap[category.id]?.sumOf { it.amount } ?: 0.0
                    val totalSum = groupMap.values.flatten().sumOf { it.amount }
                    val percent = if (totalSum > 0) (sum / totalSum * 100).toFloat() else 0f
                    ExpenseViewModel.CategorySummary(
                        categoryId = category.id,
                        categoryName = category.nameEnglish,
                        amount = sum,
                        percentage = percent
                    )
                }.filter { it.amount > 0 }.sortedByDescending { it.amount }

                if (graphData.isEmpty()) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.DonutLarge, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching category statistics found for graph.", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    Column(modifier = Modifier.padding(14.dp)) {
                        CanvasSpendChart(nonZeroSummaries = graphData)
                    }
                }
            }
        }

        // Export Actions Section (CSV & PDF Generation)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "EXPORT CORPORATE STATEMENT (حاصل رپورٹ)",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generate and download secure corporate audit summaries in official formats.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val path = viewModel.exportFilteredExpensesToCSV(context)
                                Toast.makeText(context, "Downloaded CSV: $path (Copied to file backup)", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp).testTag("export_csv_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val path = viewModel.exportFilteredExpensesToPDF(context)
                                Toast.makeText(context, "Downloaded PDF: $path (Branded secure copy)", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp).testTag("export_pdf_btn")
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // List loop matching filter matrices
        item {
            Text(
                text = "Matched Filings List (${filteredList.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FindInPage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active expenses match the selected filters.", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            items(filteredList) { expense ->
                val catDetails = ExpenseCategories.list.find { it.id == expense.category }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catDetails?.icon ?: Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = expense.description,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${expense.staffName} • ${SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH).format(Date(expense.timestamp))}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Text(
                                text = "₨ ${String.format("%,.0f", expense.amount)}",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (expense.locationAddress != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "${expense.locationAddress} (${expense.latitude}, ${expense.longitude})",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilingStatusBadge(status = expense.status)
                            if (expense.receiptUri != null) {
                                Text(
                                    text = "✓ Receipt Encrypted in Drive Folder",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = com.example.ui.theme.GreenText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 5. SETTINGS & DRIVE CLOUD SYNC SCREEN
// ==========================================
@Composable
fun SettingsSyncScreen(viewModel: ExpenseViewModel) {
    val syncMessage by viewModel.syncMessage.collectAsState()
    val driveToken by viewModel.driveAccessToken.collectAsState()
    val currentRole by viewModel.currentUserRole.collectAsState()
    val isAdmin = viewModel.isAdmin()
    val context = LocalContext.current

    // Access protection if not Administrator (RBAC implementation)
    if (!isAdmin) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = "🔒 Role Protected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🔒 ADMINISTRATOR CREDENTIAL REQUIRED",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The credentials switched currently: $currentRole do not have permissions to modify Google developers REST tokens or generate cold database imports. Please switch to Admin (Management) role to access sync panel.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Backup & Cloud Synchronization (کلاؤڈ سنک)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "To sync backups directly into your personal Google Drive account space, paste an authorized Google developers Bearer Access Token below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )

                    OutlinedTextField(
                        value = driveToken,
                        onValueChange = { viewModel.setDriveAccessToken(it) },
                        label = { Text("Google OAuth Token") },
                        placeholder = { Text("ya29.a0Ac...") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("oauth_token_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.performOfflineBackup() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("offline_backup_button")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Offline File", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.performGoogleDriveSync() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("gdrive_sync_button")
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Drive Sync", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Sync logs section (Displays logs)
        syncMessage?.let { msg ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sync Logger Active", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            IconButton(
                                onClick = { viewModel.dismissSyncMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = msg,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

