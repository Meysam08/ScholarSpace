package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    indices = [
        Index(value = ["startedAt"]),
        Index(value = ["isCompleted"]),
        Index(value = ["associatedTaskId"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val sessionType: String, // "FOCUS", "SHORT_BREAK", "LONG_BREAK"
    val plannedDurationSeconds: Int,
    val actualDurationSeconds: Int,
    val startedAt: Long,
    val completedAt: Long? = null,
    val isCompleted: Boolean = false,
    val associatedTaskId: String? = null,
    val associatedCollectionId: String? = null,
    val associatedMaterialId: String? = null
)
