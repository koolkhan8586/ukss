package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class AuthRequest(
    val username: String,
    val password: String,
    val fullName: String? = null,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val token: String,
    val user: RemoteUser
)

@JsonClass(generateAdapter = true)
data class MeResponse(
    val user: RemoteUser
)

@JsonClass(generateAdapter = true)
data class RemoteUser(
    val id: Long,
    val username: String,
    val fullName: String,
    val role: String,
    val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class RemoteExpense(
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val staffName: String,
    val status: String = "PENDING",
    val receiptUri: String? = null,
    val approvalTimestamp: Long? = null,
    val adminNotes: String? = null,
    val isSynced: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAddress: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteAllocation(
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class RemoteAttendance(
    val id: Long = 0,
    val staffName: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double,
    val longitude: Double,
    val locationAddress: String,
    val isSynced: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OkResponse(
    val ok: Boolean = true
)

interface ExpenseApiService {
    @POST("auth/login")
    suspend fun login(@Body body: AuthRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body body: AuthRequest): AuthResponse

    @GET("auth/me")
    suspend fun me(@Header("Authorization") bearer: String): MeResponse

    @GET("expenses")
    suspend fun listExpenses(@Header("Authorization") bearer: String): List<RemoteExpense>

    @POST("expenses")
    suspend fun createExpense(
        @Header("Authorization") bearer: String,
        @Body body: RemoteExpense
    ): RemoteExpense

    @PUT("expenses/{id}")
    suspend fun updateExpense(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long,
        @Body body: RemoteExpense
    ): RemoteExpense

    @DELETE("expenses/{id}")
    suspend fun deleteExpense(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long
    ): OkResponse

    @GET("allocations")
    suspend fun listAllocations(@Header("Authorization") bearer: String): List<RemoteAllocation>

    @POST("allocations")
    suspend fun createAllocation(
        @Header("Authorization") bearer: String,
        @Body body: RemoteAllocation
    ): RemoteAllocation

    @DELETE("allocations/{id}")
    suspend fun deleteAllocation(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long
    ): OkResponse

    @GET("attendance")
    suspend fun listAttendance(@Header("Authorization") bearer: String): List<RemoteAttendance>

    @POST("attendance")
    suspend fun createAttendance(
        @Header("Authorization") bearer: String,
        @Body body: RemoteAttendance
    ): RemoteAttendance

    @DELETE("attendance/{id}")
    suspend fun deleteAttendance(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long
    ): OkResponse

    @GET("users")
    suspend fun listUsers(@Header("Authorization") bearer: String): List<RemoteUser>

    @POST("users")
    suspend fun createUser(
        @Header("Authorization") bearer: String,
        @Body body: AuthRequest
    ): RemoteUser

    @DELETE("users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long
    ): OkResponse
}
