package com.example.data

import android.content.Context
import android.util.Base64
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class DriveSyncManager(private val context: Context, private val repository: ExpenseRepository) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val client = OkHttpClient()

    // Define Backup Structure
    data class BackupData(
        val backupVersion: Int,
        val timestamp: Long,
        val expenses: List<Expense>,
        val allocations: List<BudgetAllocation>,
        val attendance: List<Attendance>? = null
    )

    /**
     * Generate the complete JSON string of all database records
     */
    suspend fun generateBackupJson(): String = withContext(Dispatchers.IO) {
        val expenses = repository.loadAllExpensesDirect()
        val allocations = repository.loadAllAllocationsDirect()
        val attendance = repository.loadAllAttendanceDirect()
        val backup = BackupData(
            backupVersion = 2,
            timestamp = System.currentTimeMillis(),
            expenses = expenses,
            allocations = allocations,
            attendance = attendance
        )
        val adapter = moshi.adapter(BackupData::class.java)
        adapter.toJson(backup)
    }

    /**
     * Restore database from Backup JSON string
     */
    suspend fun restoreBackupFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(BackupData::class.java)
            val backup = adapter.fromJson(jsonString) ?: return@withContext false
            
            // Delete all and import
            for (allocation in backup.allocations) {
                repository.insertAllocation(allocation)
            }
            for (expense in backup.expenses) {
                // Update or insert
                repository.insertExpense(expense)
            }
            backup.attendance?.let {
                for (att in it) {
                    repository.insertAttendance(att)
                }
            }
            true
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Restore failed", e)
            false
        }
    }

    /**
     * Save backup locally on external storage / cache for offline mode
     */
    suspend fun saveBackupLocally(): File = withContext(Dispatchers.IO) {
        val json = generateBackupJson()
        val dir = File(context.cacheDir, "backups")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "expense_backup_${System.currentTimeMillis() / 1000}.json")
        file.writeText(json)
        file
    }

    /**
     * Sync with Google Drive REST API using a Bearer token
     * This calls the standard Google Drive API to upload the backup.
     */
    suspend fun syncWithGoogleDrive(accessToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = generateBackupJson()
            
            // Step 1: Create metadata for Drive file
            val metadataJson = """
                {
                  "name": "Expense_Manager_Backup.json",
                  "mimeType": "application/json",
                  "parents": ["appDataFolder"]
                }
            """.trimIndent()

            // Multiple part upload or simple update
            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = json.toRequestBody(mediaType)

            // Let's call Drive API to upload the file
            // POST https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart
            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=media")
                .header("Authorization", "Bearer $accessToken")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Result.success("Sync successful! File Saved to Google Drive App Storage. Response: $body")
                } else {
                    Result.failure(Exception("Drive upload failed: ${response.code} - ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Log.e("DriveSyncManager", "Drive upload error", e)
            Result.failure(e)
        }
    }
}
