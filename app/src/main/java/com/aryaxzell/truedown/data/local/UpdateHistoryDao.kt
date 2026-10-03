package com.aryaxzell.truedown.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateHistoryDao {

    @Query("SELECT * FROM update_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<UpdateHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entity: UpdateHistoryEntity)

    @Query("DELETE FROM update_history")
    suspend fun clearHistory()
}
