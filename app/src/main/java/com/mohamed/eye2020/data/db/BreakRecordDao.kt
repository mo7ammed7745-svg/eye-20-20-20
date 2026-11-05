package com.mohamed.eye2020.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BreakRecordDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(record: BreakRecord): Long

    @Query(
        "SELECT * FROM break_records " +
            "WHERE completedAtMillis >= :fromMillis ORDER BY completedAtMillis",
    )
    fun observeSince(fromMillis: Long): Flow<List<BreakRecord>>

    @Query("SELECT COUNT(*) FROM break_records")
    fun observeTotalCount(): Flow<Int>
}
