package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpensesFlow()
    val allAllocations: Flow<List<BudgetAllocation>> = expenseDao.getAllAllocationsFlow()
    val allAttendance: Flow<List<Attendance>> = expenseDao.getAllAttendanceFlow()
    val allUsers: Flow<List<User>> = expenseDao.getAllUsersFlow()

    suspend fun getUserByUsername(username: String): User? {
        return expenseDao.getUserByUsername(username)
    }

    suspend fun getUserById(id: Long): User? {
        return expenseDao.getUserById(id)
    }

    suspend fun insertUser(user: User): Long {
        return expenseDao.insertUser(user)
    }

    suspend fun deleteUserById(id: Long) {
        expenseDao.deleteUserById(id)
    }

    suspend fun insertAttendance(attendance: Attendance): Long {
        return expenseDao.insertAttendance(attendance)
    }

    suspend fun loadAllAttendanceDirect(): List<Attendance> {
        return expenseDao.getAllAttendance()
    }

    suspend fun deleteAttendanceById(id: Long) {
        expenseDao.deleteAttendanceById(id)
    }

    fun getExpensesByStatus(status: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByStatusFlow(status)
    }

    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun insertAllocation(allocation: BudgetAllocation): Long {
        return expenseDao.insertAllocation(allocation)
    }

    suspend fun deleteAllocationById(id: Long) {
        expenseDao.deleteAllocationById(id)
    }

    suspend fun loadAllExpensesDirect(): List<Expense> {
        return expenseDao.getAllExpenses()
    }

    suspend fun loadAllAllocationsDirect(): List<BudgetAllocation> {
        return expenseDao.getAllAllocations()
    }
}
