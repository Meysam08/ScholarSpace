package com.example.data.repository

import com.example.data.dao.FocusDao
import com.example.data.model.FocusSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class FocusRepository(private val focusDao: FocusDao) {

    fun getRecentCompletedSessions(limit: Int = 20): Flow<List<FocusSessionEntity>> =
        focusDao.getRecentCompletedSessions(limit)

    fun getCompletedFocusSessionsCount(): Flow<Int> =
        focusDao.getCompletedFocusSessionsCount()

    fun getTotalFocusDurationSeconds(): Flow<Long?> =
        focusDao.getTotalFocusDurationSeconds()

    suspend fun recordSession(
        sessionType: String,
        plannedDurationSeconds: Int,
        actualDurationSeconds: Int,
        startedAt: Long,
        isCompleted: Boolean,
        associatedTaskId: String? = null,
        associatedCollectionId: String? = null,
        associatedMaterialId: String? = null
    ): FocusSessionEntity = withContext(Dispatchers.IO) {
        val session = FocusSessionEntity(
            id = UUID.randomUUID().toString(),
            sessionType = sessionType,
            plannedDurationSeconds = plannedDurationSeconds,
            actualDurationSeconds = actualDurationSeconds,
            startedAt = startedAt,
            completedAt = if (isCompleted) System.currentTimeMillis() else null,
            isCompleted = isCompleted,
            associatedTaskId = associatedTaskId,
            associatedCollectionId = associatedCollectionId,
            associatedMaterialId = associatedMaterialId
        )
        focusDao.insert(session)
        session
    }
}
