package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "materials",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["mimeType"]),
        Index(value = ["sortOrder"])
    ]
)
data class MaterialEntity(
    @PrimaryKey val id: String,
    val collectionId: String,
    val displayName: String,
    val mimeType: String,
    val relativeFilePath: String,
    val fileSizeBytes: Long = 0L,
    val thumbnailPath: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
