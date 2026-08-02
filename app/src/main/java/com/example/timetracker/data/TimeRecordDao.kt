package com.example.timetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TimeRecordDao {
    @Query("SELECT * FROM time_records ORDER BY startTime DESC")
    fun getAllRecords(): Flow<List<TimeRecord>>

    @Query("SELECT * FROM time_records WHERE startTime >= :start AND startTime < :end ORDER BY startTime DESC")
    fun getRecordsBetween(start: Instant, end: Instant): Flow<List<TimeRecord>>

    @Query("SELECT * FROM time_records WHERE id = :id")
    suspend fun getById(id: Long): TimeRecord?

    @Insert
    suspend fun insert(record: TimeRecord): Long

    @Update
    suspend fun update(record: TimeRecord)

    @Delete
    suspend fun delete(record: TimeRecord)

    @Query("UPDATE time_records SET categoryId = NULL, categoryName = '' WHERE categoryId = :categoryId")
    suspend fun clearCategoryForDeletedCategory(categoryId: Long)

    @Query("SELECT SUM(durationSeconds) FROM time_records WHERE categoryId = :categoryId AND startTime >= :start AND startTime < :end")
    suspend fun getTotalSecondsByCategory(categoryId: Long, start: Instant, end: Instant): Long?
}
