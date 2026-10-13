package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["isCompleted"]),
        Index(value = ["isMyDay"]),
        Index(value = ["dueDate"]),
        Index(value = ["collectionId"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String? = null,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val isMyDay: Boolean = false,
    val myDayDate: Long? = null,
    val collectionId: String? = null,
    val materialId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
