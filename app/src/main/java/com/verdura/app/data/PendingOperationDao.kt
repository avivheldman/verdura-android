package com.verdura.app.data

import androidx.room.*
import com.verdura.app.model.PendingOperation
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    fun getAllPendingOperations(): Flow<List<PendingOperation>>

    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAllPendingOperationsSync(): List<PendingOperation>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun getPendingOperationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperation)

    @Delete
    suspend fun delete(operation: PendingOperation)

    @Query("DELETE FROM pending_operations WHERE id = :operationId")
    suspend fun deleteById(operationId: Long)

    @Query("DELETE FROM pending_operations")
    suspend fun deleteAll()
}
