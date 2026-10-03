package com.aryaxzell.truedown.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PostEntity::class, MediaItemEntity::class, UpdateHistoryEntity::class],
    version = 3,
    exportSchema = true
)
abstract class TruedownDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun updateHistoryDao(): UpdateHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: TruedownDatabase? = null

        fun getInstance(context: Context): TruedownDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TruedownDatabase::class.java,
                    "truedown.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
