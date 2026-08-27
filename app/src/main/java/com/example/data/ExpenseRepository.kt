package com.example.data

import com.example.data.remote.ApiClient
import com.example.data.remote.AuthRequest
import com.example.data.remote.toLocal
import com.example.data.remote.toRemote
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
        val bearer = ApiClient.bearer()
        if (bearer != null) {
            runCatching { ApiClient.service.deleteUser(bearer, id) }
        }
        expenseDao.deleteUserById(id)
    }

    suspend fun insertAttendance(attendance: Attendance): Long {
        val bearer = ApiClient.bearer()
        if (bearer != null) {
            runCatching {
                val remote = ApiClient.service.createAttendance(bearer, attendance.toRemote())
                return expenseDao.insertAttendance(remote.toLocal())
            }
        }
        return expenseDao.insertAttendance(attendance)
    }

    suspend fun loadAllAttendanceDirect(): List<Attendance> {
        return expenseDao.getAllAttendance()
    }

    suspend fun deleteAttendanceById(id: Long) {
        val bearer = ApiClient.bearer()
        if (bearer != null) {
            runCatching { ApiClient.service.deleteAttendance(bearer, id) }
        }
        expenseDao.deleteAttendanceById(id)
    }

    fun getExpensesByStatus(status: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByStatusFlow(status)
    }

    suspend fun insertExpense(expense: Expense): Long {
        val bearer = ApiClient.bearer()
        if (bearer != null) {
            runCatching {
                val remote = ApiClient.service.createExpense(bearer, expense.toRemote())
                return expenseDao.insertExpense(remote.toLocal())
            }
        }
        return expenseDao.insertExpense(expense.copy(isSynced = false))
    }

    suspend fun updateExpense(expense: Expense) {
        val bearer = ApiClient.bearer()
        if (bearer != null) {
            runCatching {
                val remote = ApiClient.service.updateExpense(bearer, expense.id, expense.toRemote())
                expenseDao.updateExpense(remote.toLocal())
                return
            }
        }
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        val bearer = ApiClient.bearer()
        if (bearer != null) {
            runCatching { ApiClient.service.deleteExpense(bearer, id) }
        }
        expenseDao.deleteExpenseById(id)
    }

    suspend fun insertAllocation(allocation: BudgetAllocation): Long {
        val bearer = ApiClient.bearer()
        if (bearer != null) {
            runCatching {
                val remote = ApiClient.service.createAllocation(bearer, allocation.toRemote())
                return expenseDao.insertAllocation(remote.toLocal())
            }
        }
        return expenseDao.insertAllocation(allocation)
    }

    suspend fun deleteAllocationById(id: Long) {
        val bearer = ApiClient.bearer()
        if (bearer != null) {
            runCatching { ApiClient.service.deleteAllocation(bearer, id) }
        }
        expenseDao.deleteAllocationById(id)
    }

    suspend fun loadAllExpensesDirect(): List<Expense> {
        return expenseDao.getAllExpenses()
    }

    suspend fun loadAllAllocationsDirect(): List<BudgetAllocation> {
        return expenseDao.getAllAllocations()
    }

    /**
     * Authenticate against https://exp.ukssolution.com and cache the session locally.
     * Falls back to local Room users if the server is unreachable.
     */
    suspend fun loginRemoteOrLocal(username: String, password: String): Result<User> {
        val remote = runCatching {
            ApiClient.service.login(AuthRequest(username = username, password = password))
        }.getOrNull()

        if (remote != null) {
            ApiClient.setToken(remote.token)
            val user = remote.user.toLocal(passwordPlaceholder = password)
            expenseDao.insertUser(user)
            syncFromServer()
            return Result.success(user)
        }

        val local = expenseDao.getUserByUsername(username)
            ?: return Result.failure(IllegalArgumentException("User not found (offline and server unreachable)"))
        if (local.password != password) {
            return Result.failure(IllegalArgumentException("Incorrect password"))
        }
        return Result.success(local)
    }

    suspend fun registerRemoteOrLocal(
        username: String,
        password: String,
        fullName: String,
        role: String
    ): Result<User> {
        val remote = runCatching {
            ApiClient.service.register(
                AuthRequest(
                    username = username,
                    password = password,
                    fullName = fullName,
                    role = role
                )
            )
        }.getOrNull()

        if (remote != null) {
            ApiClient.setToken(remote.token)
            val user = remote.user.toLocal(passwordPlaceholder = password)
            expenseDao.insertUser(user)
            syncFromServer()
            return Result.success(user)
        }

        val existing = expenseDao.getUserByUsername(username)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Username already exists"))
        }
        val local = User(username = username, password = password, fullName = fullName, role = role)
        val id = expenseDao.insertUser(local)
        return Result.success(local.copy(id = id))
    }

    suspend fun restoreSession(token: String): User? {
        ApiClient.setToken(token)
        val me = runCatching { ApiClient.service.me(ApiClient.bearer()!!) }.getOrNull() ?: return null
        val user = me.user.toLocal()
        expenseDao.insertUser(user)
        syncFromServer()
        return user
    }

    suspend fun syncFromServer() {
        val bearer = ApiClient.bearer() ?: return
        runCatching {
            ApiClient.service.listExpenses(bearer).forEach { expenseDao.insertExpense(it.toLocal()) }
            ApiClient.service.listAttendance(bearer).forEach { expenseDao.insertAttendance(it.toLocal()) }
            ApiClient.service.listAllocations(bearer).forEach { expenseDao.insertAllocation(it.toLocal()) }
            runCatching {
                ApiClient.service.listUsers(bearer).forEach { remoteUser ->
                    expenseDao.insertUser(remoteUser.toLocal())
                }
            }
        }
    }

    fun clearRemoteSession() {
        ApiClient.setToken(null)
    }
}
