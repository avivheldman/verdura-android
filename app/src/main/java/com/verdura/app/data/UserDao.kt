package com.verdura.app.data

import androidx.room.*
import com.verdura.app.model.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getById(userId: String): User?

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
