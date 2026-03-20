package com.verdura.app.data

import androidx.room.*
import com.verdura.app.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Update
    suspend fun update(user: User)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getById(userId: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeById(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getByEmail(email: String): User?

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
