package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpensesFlow(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpenses(): List<Expense>

    @Query("SELECT * FROM expenses WHERE status = :status ORDER BY timestamp DESC")
    fun getExpensesByStatusFlow(status: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("SELECT * FROM budget_allocations ORDER BY timestamp DESC")
    fun getAllAllocationsFlow(): Flow<List<BudgetAllocation>>

    @Query("SELECT * FROM budget_allocations ORDER BY timestamp DESC")
    suspend fun getAllAllocations(): List<BudgetAllocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: BudgetAllocation): Long

    @Query("DELETE FROM budget_allocations WHERE id = :id")
    suspend fun deleteAllocationById(id: Long)

    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    fun getAllAttendanceFlow(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    suspend fun getAllAttendance(): List<Attendance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteAttendanceById(id: Long)

    @Query("SELECT * FROM users ORDER BY timestamp DESC")
    fun getAllUsersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: Long)
}
