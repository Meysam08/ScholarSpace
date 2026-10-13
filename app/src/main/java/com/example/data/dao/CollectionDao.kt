package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections WHERE parentId IS NULL ORDER BY sortOrder ASC, createdAt DESC")
    fun getRootCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE parentId = :parentId ORDER BY sortOrder ASC, createdAt DESC")
    fun getSubCollections(parentId: String): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllCollectionsList(): List<CollectionEntity>

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    fun getCollectionById(id: String): Flow<CollectionEntity?>

    @Query("SELECT * FROM collections WHERE id = :id LIMIT 1")
    suspend fun getCollectionByIdSync(id: String): CollectionEntity?

    @Query("SELECT * FROM collections WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCollections(query: String): Flow<List<CollectionEntity>>

    @Query("SELECT COUNT(*) FROM collections WHERE parentId = :parentId")
    suspend fun getSubCollectionCount(parentId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(collections: List<CollectionEntity>)

    @Update
    suspend fun update(collection: CollectionEntity)

    @Delete
    suspend fun delete(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM collections")
    suspend fun deleteAll()
}
