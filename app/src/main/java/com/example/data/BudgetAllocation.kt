package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "budget_allocations")
data class BudgetAllocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
) : Serializable
