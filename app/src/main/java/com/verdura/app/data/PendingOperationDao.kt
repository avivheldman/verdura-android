package com.verdura.app.data

import androidx.room.*
import com.verdura.app.model.PendingOperation

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAllPendingOperationsSync(): List<PendingOperation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperation)

    @Query("DELETE FROM pending_operations WHERE id = :operationId")
    suspend fun deleteById(operationId: Long)

    @Query("DELETE FROM pending_operations")
    suspend fun deleteAll()
}
