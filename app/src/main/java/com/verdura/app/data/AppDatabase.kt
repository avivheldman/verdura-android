package com.verdura.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.verdura.app.model.CachedPlantDetail
import com.verdura.app.model.CachedTrefleDetail
import com.verdura.app.model.PendingOperation
import com.verdura.app.model.PlantInfo
import com.verdura.app.model.Post
import com.verdura.app.model.User

@Database(
    entities = [User::class, Post::class, PendingOperation::class, PlantInfo::class, CachedPlantDetail::class, CachedTrefleDetail::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun plantInfoDao(): PlantInfoDao
    abstract fun plantDetailCacheDao(): PlantDetailCacheDao
    abstract fun trefleDetailCacheDao(): TrefleDetailCacheDao

    companion object {
        private const val DATABASE_NAME = "verdura_database"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_operations (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        operationType TEXT NOT NULL,
                        postId TEXT NOT NULL,
                        postData TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS plant_info (
                        id INTEGER PRIMARY KEY NOT NULL,
                        commonName TEXT,
                        scientificName TEXT,
                        cycle TEXT,
                        watering TEXT,
                        sunlight TEXT,
                        imageUrl TEXT,
                        cachedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_plant_details (
                        plantId INTEGER PRIMARY KEY NOT NULL,
                        detailJson TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cached_trefle_details (
                        scientificName TEXT PRIMARY KEY NOT NULL,
                        detailJson TEXT NOT NULL,
                        cachedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM cached_trefle_details")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM plant_info")
                db.execSQL("DELETE FROM cached_plant_details")
                db.execSQL("DELETE FROM cached_trefle_details")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
