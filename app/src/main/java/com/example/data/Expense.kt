package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val staffName: String,
    val status: String, // "PENDING", "APPROVED", "REJECTED"
    val receiptUri: String? = null, // Path to local cached image
    val approvalTimestamp: Long? = null,
    val adminNotes: String? = null,
    val isSynced: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAddress: String? = null
) : Serializable
