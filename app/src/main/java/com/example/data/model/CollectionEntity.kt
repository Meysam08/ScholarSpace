package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collections",
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["sortOrder"])
    ]
)
data class CollectionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String? = null,
    val coverColorHex: String = "#176B63",
    val customCoverPath: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
