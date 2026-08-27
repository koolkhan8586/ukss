package com.example.data.remote

import com.example.data.Attendance
import com.example.data.BudgetAllocation
import com.example.data.Expense
import com.example.data.User
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    @Volatile
    private var token: String? = null

    fun setToken(value: String?) {
        token = value
    }

    fun bearer(): String? = token?.let { "Bearer $it" }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    val service: ExpenseApiService = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(ExpenseApiService::class.java)
}

fun RemoteUser.toLocal(passwordPlaceholder: String = ""): User = User(
    id = id,
    username = username,
    password = passwordPlaceholder,
    fullName = fullName,
    role = role,
    timestamp = timestamp
)

fun RemoteExpense.toLocal(): Expense = Expense(
    id = id,
    amount = amount,
    category = category,
    description = description,
    timestamp = timestamp,
    staffName = staffName,
    status = status,
    receiptUri = receiptUri,
    approvalTimestamp = approvalTimestamp,
    adminNotes = adminNotes,
    isSynced = true,
    latitude = latitude,
    longitude = longitude,
    locationAddress = locationAddress
)

fun Expense.toRemote(): RemoteExpense = RemoteExpense(
    id = id,
    amount = amount,
    category = category,
    description = description,
    timestamp = timestamp,
    staffName = staffName,
    status = status,
    receiptUri = receiptUri,
    approvalTimestamp = approvalTimestamp,
    adminNotes = adminNotes,
    isSynced = true,
    latitude = latitude,
    longitude = longitude,
    locationAddress = locationAddress
)

fun RemoteAllocation.toLocal(): BudgetAllocation = BudgetAllocation(
    id = id,
    amount = amount,
    description = description,
    timestamp = timestamp
)

fun BudgetAllocation.toRemote(): RemoteAllocation = RemoteAllocation(
    id = id,
    amount = amount,
    description = description,
    timestamp = timestamp
)

fun RemoteAttendance.toLocal(): Attendance = Attendance(
    id = id,
    staffName = staffName,
    type = type,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude,
    locationAddress = locationAddress,
    isSynced = true
)

fun Attendance.toRemote(): RemoteAttendance = RemoteAttendance(
    id = id,
    staffName = staffName,
    type = type,
    timestamp = timestamp,
    latitude = latitude,
    longitude = longitude,
    locationAddress = locationAddress,
    isSynced = true
)
