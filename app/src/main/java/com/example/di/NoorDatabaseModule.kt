package com.example.di

import android.content.Context
import com.example.data.local.DuaDao
import com.example.data.local.HadithDao
import com.example.data.local.NoorCacheDatabase
import com.example.data.local.SurahDao
import com.example.data.repository.NoorRepository
import com.example.data.repository.OfflineContentRepository

/**
 * Lightweight dependency injection module providing singleton instances of the
 * local Room database, DAOs, and offline content repository via constructor injection.
 */
object NoorDatabaseModule {

    @Volatile
    private var offlineRepositoryInstance: OfflineContentRepository? = null

    fun provideLocalDatabase(context: Context): NoorCacheDatabase =
        NoorCacheDatabase.getInstance(context.applicationContext)

    fun provideSurahDao(context: Context): SurahDao =
        provideLocalDatabase(context).surahDao()

    fun provideHadithDao(context: Context): HadithDao =
        provideLocalDatabase(context).hadithDao()

    fun provideDuaDao(context: Context): DuaDao =
        provideLocalDatabase(context).duaDao()

    fun provideOfflineContentRepository(context: Context): OfflineContentRepository {
        return offlineRepositoryInstance ?: synchronized(this) {
            offlineRepositoryInstance ?: OfflineContentRepository(
                db = provideLocalDatabase(context)
            ).also { offlineRepositoryInstance = it }
        }
    }

    fun provideNoorRepository(context: Context): NoorRepository =
        NoorRepository(context.applicationContext)
}
