package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    suspend fun getAllSessionsList(): List<FocusSessionEntity>

    @Query("SELECT * FROM focus_sessions WHERE isCompleted = 1 ORDER BY startedAt DESC LIMIT :limit")
    fun getRecentCompletedSessions(limit: Int = 20): Flow<List<FocusSessionEntity>>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE isCompleted = 1 AND sessionType = 'FOCUS'")
    fun getCompletedFocusSessionsCount(): Flow<Int>

    @Query("SELECT SUM(actualDurationSeconds) FROM focus_sessions WHERE isCompleted = 1 AND sessionType = 'FOCUS'")
    fun getTotalFocusDurationSeconds(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: FocusSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<FocusSessionEntity>)

    @Delete
    suspend fun delete(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAll()
}
