package com.aryaxzell.truedown.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "update_history")
data class UpdateHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val versionName: String,
    val updateType: String, // "Stable" atau "Nightly"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String, // "Success", "Failed", "Skipped"
    val errorMessage: String? = null
)
