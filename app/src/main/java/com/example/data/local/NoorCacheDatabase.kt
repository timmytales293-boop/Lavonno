package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        HadithEntity::class,
        DuaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NoorCacheDatabase : RoomDatabase() {
    abstract fun surahDao(): SurahDao
    abstract fun hadithDao(): HadithDao
    abstract fun duaDao(): DuaDao

    companion object {
        @Volatile
        private var INSTANCE: NoorCacheDatabase? = null

        fun getInstance(context: Context): NoorCacheDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoorCacheDatabase::class.java,
                    "noor_offline_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
