package com.verdura.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdura.app.model.CachedTrefleDetail

@Dao
interface TrefleDetailCacheDao {
    @Query("SELECT * FROM cached_trefle_details WHERE scientificName = :name LIMIT 1")
    suspend fun get(name: String): CachedTrefleDetail?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: CachedTrefleDetail)
}
