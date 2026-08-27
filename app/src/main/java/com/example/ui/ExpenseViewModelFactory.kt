package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.DriveSyncManager
import com.example.data.ExpenseRepository

class ExpenseViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = ExpenseRepository(database.expenseDao())
            val syncManager = DriveSyncManager(context, repository)
            return ExpenseViewModel(repository, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
