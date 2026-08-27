package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class ExpenseCategory(
    val id: String,
    val nameEnglish: String,
    val nameUrdu: String,
    val description: String,
    val icon: ImageVector
)

object ExpenseCategories {
    val list = listOf(
        ExpenseCategory("travel", "Travel", "سفر", "Fuel, public transit & flights", Icons.Default.DirectionsCar),
        ExpenseCategory("food", "Food & Meals", "کھانا", "Staff meals & catering", Icons.Default.Restaurant),
        ExpenseCategory("supplies", "Office Supplies", "دفتر کا سامان", "Stationery, papers & accessories", Icons.Default.Create),
        Icons.Default.Home.let { ExpenseCategory("rent", "Rent", "کرایہ", "Monthly office rent", it) },
        ExpenseCategory("electricity", "Electricity", "بجلی کا بل", "Power utilities", Icons.Default.ElectricBolt),
        ExpenseCategory("water", "Water Utilities", "پانی کا بل", "Water services", Icons.Default.WaterDrop),
        ExpenseCategory("internet", "Internet & Wifi", "انٹرنیٹ", "Data packs & fiber lines", Icons.Default.Wifi),
        ExpenseCategory("marketing", "Marketing", "مشتہرین", "Ads & social media promotion", Icons.Default.Campaign),
        ExpenseCategory("software", "Software Licenses", "سافٹ ویئر", "Tools like Slack, Office, etc", Icons.Default.Terminal),
        ExpenseCategory("equipment", "Equipment", "سامان اور مشینری", "Hardware / Laptop purchases", Icons.Default.Laptop),
        ExpenseCategory("repairs", "Repairs", "مرمت", "A/C or furniture repairs", Icons.Default.Build),
        ExpenseCategory("salaries", "Staff Salaries", "تنخواہیں", "Contractors or minor staff pay", Icons.Default.Payments),
        ExpenseCategory("printing", "Printing", "چھپائی", "Brochures and paper prints", Icons.Default.Print),
        ExpenseCategory("consulting", "Professional Bills", "پیشہ ورانہ بل", "Legal or accounts consultancies", Icons.Default.SupervisorAccount),
        ExpenseCategory("events", "Events & Tafreeh", "تفریح", "Office parties and events", Icons.Default.Celebration),
        ExpenseCategory("postage", "Postage & Courier", "ڈاک", "Post charges", Icons.Default.LocalPostOffice),
        ExpenseCategory("taxes", "Taxes & Duties", "ٹیکس", "Filing and legal taxes", Icons.Default.Percent),
        ExpenseCategory("insurance", "Insurance", "بیمہ", "Health or asset coverage", Icons.Default.Security),
        ExpenseCategory("cleaning", "Cleaning & Hygiene", "صفائی", "Soaps, napkins and sanitizers", Icons.Default.CleanHands),
        ExpenseCategory("misc", "Miscellaneous", "دیگر اخراجات", "Unspecified micro-expenses", Icons.Default.Category)
    )
}
