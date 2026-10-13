package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {

    @Query("SELECT * FROM materials WHERE collectionId = :collectionId ORDER BY sortOrder ASC, createdAt DESC")
    fun getMaterialsForCollection(collectionId: String): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials ORDER BY createdAt DESC")
    fun getAllMaterials(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials ORDER BY createdAt DESC")
    suspend fun getAllMaterialsList(): List<MaterialEntity>

    @Query("SELECT * FROM materials WHERE id = :id LIMIT 1")
    fun getMaterialById(id: String): Flow<MaterialEntity?>

    @Query("SELECT * FROM materials WHERE id = :id LIMIT 1")
    suspend fun getMaterialByIdSync(id: String): MaterialEntity?

    @Query("SELECT * FROM materials WHERE displayName LIKE '%' || :query || '%' ORDER BY displayName ASC")
    fun searchMaterials(query: String): Flow<List<MaterialEntity>>

    @Query("SELECT COUNT(*) FROM materials WHERE collectionId = :collectionId")
    suspend fun getCountForCollection(collectionId: String): Int

    @Query("SELECT COUNT(*) FROM materials")
    fun getTotalMaterialsCount(): Flow<Int>

    @Query("SELECT SUM(fileSizeBytes) FROM materials")
    fun getTotalFileSizeBytes(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(material: MaterialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(materials: List<MaterialEntity>)

    @Update
    suspend fun update(material: MaterialEntity)

    @Delete
    suspend fun delete(material: MaterialEntity)

    @Query("DELETE FROM materials WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM materials WHERE collectionId = :collectionId")
    suspend fun deleteByCollectionId(collectionId: String)

    @Query("DELETE FROM materials")
    suspend fun deleteAll()
}
