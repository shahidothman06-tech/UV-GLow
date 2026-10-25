package com.tubers.blacklight.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<PresetEntity>>

    @Insert
    suspend fun insert(entity: PresetEntity): Long

    @Query("UPDATE presets SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ThemeDao {
    @Query("SELECT * FROM custom_themes ORDER BY createdAt ASC, id ASC")
    fun observeAll(): Flow<List<ThemeEntity>>

    @Insert
    suspend fun insert(entity: ThemeEntity): Long

    @Query("UPDATE custom_themes SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("UPDATE custom_themes SET accentArgb = :accent, groundArgb = :ground WHERE id = :id")
    suspend fun updateColors(id: Long, accent: Int, ground: Int)

    @Query("DELETE FROM custom_themes WHERE id = :id")
    suspend fun delete(id: Long)
}
