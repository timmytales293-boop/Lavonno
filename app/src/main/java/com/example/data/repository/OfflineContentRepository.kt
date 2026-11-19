package com.example.data.repository

import com.example.data.content.DuaCatalog
import com.example.data.content.HadithCatalog
import com.example.data.content.QuranCatalog
import com.example.data.local.AyahEntity
import com.example.data.local.DuaEntity
import com.example.data.local.HadithEntity
import com.example.data.local.NoorCacheDatabase
import com.example.data.local.SurahEntity
import com.example.data.model.AyahItem
import com.example.data.model.DuaItem
import com.example.data.model.HadithItem
import com.example.data.model.SurahMeta
import com.example.data.network.NoorSyncApiService
import com.example.data.network.QuranApiService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class ContentSyncResult(
    val surahsUpdated: Int,
    val ayahsUpdated: Int,
    val hadithsUpdated: Int,
    val duasUpdated: Int,
    val partialFailures: Int
) {
    val totalItemsUpdated: Int
        get() = surahsUpdated + ayahsUpdated + hadithsUpdated + duasUpdated
}

interface RemoteContentSyncSource {
    suspend fun fetchSurahList(): List<SurahMeta>
    suspend fun fetchSurahAyahs(surahNumber: Int): List<AyahItem>
    suspend fun fetchHadiths(): List<HadithItem>
    suspend fun fetchDuas(): List<DuaItem>
}

class DefaultRemoteContentSyncSource(
    private val ayahFetcher: suspend (Int) -> List<AyahItem> = { surah ->
        QuranApiService.fetchSurahAyahs(surah)
    }
) : RemoteContentSyncSource {
    override suspend fun fetchSurahList(): List<SurahMeta> =
        QuranApiService.fetchAllSurahMetas()

    override suspend fun fetchSurahAyahs(surahNumber: Int): List<AyahItem> =
        ayahFetcher(surahNumber)

    override suspend fun fetchHadiths(): List<HadithItem> =
        NoorSyncApiService.fetchRemoteHadiths()

    override suspend fun fetchDuas(): List<DuaItem> =
        NoorSyncApiService.fetchRemoteDuas()
}

class OfflineContentRepository(
    private val db: NoorCacheDatabase,
    private val remoteSyncSource: RemoteContentSyncSource = DefaultRemoteContentSyncSource(),
    private val remoteFetcher: suspend (Int) -> List<AyahItem> = { surah ->
        remoteSyncSource.fetchSurahAyahs(surah)
    }
) {
    private val surahDao = db.surahDao()
    private val hadithDao = db.hadithDao()
    private val duaDao = db.duaDao()

    val surahs: Flow<List<SurahMeta>> = surahDao.observeAllSurahs().map { entities ->
        if (entities.isEmpty()) QuranCatalog.all114Surahs else entities.map { it.toModel() }
    }

    val hadiths: Flow<List<HadithItem>> = hadithDao.observeAllHadiths().map { entities ->
        if (entities.isEmpty()) HadithCatalog.allHadiths else entities.map { it.toModel() }
    }

    val duas: Flow<List<DuaItem>> = duaDao.observeAllDuas().map { entities ->
        if (entities.isEmpty()) DuaCatalog.allDuas else entities.map { it.toModel() }
    }

    fun observeSurahSearch(query: String): Flow<List<SurahMeta>> =
        surahDao.searchSurahs(query).map { list -> list.map { it.toModel() } }

    fun observeHadithsByBook(book: String): Flow<List<HadithItem>> =
        hadithDao.observeHadithsByBook(book).map { list -> list.map { it.toModel() } }

    fun observeDuasByCategory(category: String): Flow<List<DuaItem>> =
        duaDao.observeDuasByCategory(category).map { list -> list.map { it.toModel() } }

    suspend fun seedInitialCacheIfNeeded() = withContext(Dispatchers.IO) {
        val bundledOfflineSurahNums = setOf(1, 103, 108, 112, 113, 114)
        if (surahDao.countSurahs() == 0) {
            surahDao.insertSurahs(
                QuranCatalog.all114Surahs.map { meta ->
                    SurahEntity.fromModel(
                        meta = meta,
                        isAyahsCached = meta.number in bundledOfflineSurahNums
                    )
                }
            )
            val allBundledAyahs = bundledOfflineSurahNums.flatMap { surahNum ->
                QuranCatalog.getOfflineAyahs(surahNum).map { ayah ->
                    AyahEntity.fromModel(surahNum, ayah)
                }
            }
            if (allBundledAyahs.isNotEmpty()) {
                surahDao.insertAyahs(allBundledAyahs)
            }
        }

        if (hadithDao.countHadiths() == 0) {
            hadithDao.insertHadiths(HadithCatalog.allHadiths.map(HadithEntity::fromModel))
        }

        if (duaDao.countDuas() == 0) {
            duaDao.insertDuas(DuaCatalog.allDuas.map(DuaEntity::fromModel))
        }
    }

    suspend fun getOrFetchSurahAyahs(surahNumber: Int): List<AyahItem> = withContext(Dispatchers.IO) {
        val cached = surahDao.getAyahsForSurahOnce(surahNumber)
        if (cached.isNotEmpty()) {
            return@withContext cached.map { it.toModel() }
        }

        val bundled = QuranCatalog.getOfflineAyahs(surahNumber)
        if (bundled.isNotEmpty()) {
            surahDao.insertAyahs(bundled.map { AyahEntity.fromModel(surahNumber, it) })
            surahDao.setSurahAyahsCached(surahNumber, cached = true)
            return@withContext bundled
        }

        val fetched = remoteFetcher(surahNumber)
        if (fetched.isNotEmpty()) {
            surahDao.insertAyahs(fetched.map { AyahEntity.fromModel(surahNumber, it) })
            surahDao.setSurahAyahsCached(surahNumber, cached = true)
        }
        fetched
    }

    /**
     * Periodically synchronizes Quran (Surah metadata + selected/cached Surahs' ayahs),
     * Hadith, and Dua content from remote APIs into Room without discarding previously
     * cached offline records if individual feeds fail.
     */
    suspend fun syncContentFromRemote(
        surahsToRefresh: List<Int> = DEFAULT_SYNC_SURAHS
    ): ContentSyncResult = withContext(Dispatchers.IO) {
        seedInitialCacheIfNeeded()

        var surahsUpdated = 0
        var ayahsUpdated = 0
        var hadithsUpdated = 0
        var duasUpdated = 0
        var partialFailures = 0
        var firstError: Throwable? = null

        val cachedSurahNumbers = surahDao.getCachedSurahNumbers().toSet()

        // 1. Refresh 114 Surah metadata while preserving isAyahsCached flags
        try {
            val remoteSurahs = remoteSyncSource.fetchSurahList()
            if (remoteSurahs.isNotEmpty()) {
                surahDao.insertSurahs(
                    remoteSurahs.map { meta ->
                        SurahEntity.fromModel(
                            meta = meta,
                            isAyahsCached = meta.number in cachedSurahNumbers
                        )
                    }
                )
                surahsUpdated = remoteSurahs.size
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            partialFailures++
            if (firstError == null) firstError = t
        }

        // 2. Refresh Ayahs for essential + already-cached Surahs
        val targetSurahs = (cachedSurahNumbers + surahsToRefresh).distinct().sorted()
        for (surahNumber in targetSurahs) {
            try {
                val remoteAyahs = remoteSyncSource.fetchSurahAyahs(surahNumber)
                if (remoteAyahs.isNotEmpty()) {
                    surahDao.insertAyahs(
                        remoteAyahs.map { AyahEntity.fromModel(surahNumber, it) }
                    )
                    surahDao.setSurahAyahsCached(surahNumber, cached = true)
                    ayahsUpdated += remoteAyahs.size
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                partialFailures++
                if (firstError == null) firstError = t
            }
        }

        // 3. Refresh Hadith records
        try {
            val remoteHadiths = remoteSyncSource.fetchHadiths()
            if (remoteHadiths.isNotEmpty()) {
                hadithDao.insertHadiths(remoteHadiths.map(HadithEntity::fromModel))
                hadithsUpdated = remoteHadiths.size
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            partialFailures++
            if (firstError == null) firstError = t
        }

        // 4. Refresh Dua records
        try {
            val remoteDuas = remoteSyncSource.fetchDuas()
            if (remoteDuas.isNotEmpty()) {
                duaDao.insertDuas(remoteDuas.map(DuaEntity::fromModel))
                duasUpdated = remoteDuas.size
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            partialFailures++
            if (firstError == null) firstError = t
        }

        val result = ContentSyncResult(
            surahsUpdated = surahsUpdated,
            ayahsUpdated = ayahsUpdated,
            hadithsUpdated = hadithsUpdated,
            duasUpdated = duasUpdated,
            partialFailures = partialFailures
        )

        if (result.totalItemsUpdated == 0 && firstError != null) {
            throw firstError
        }

        result
    }

    suspend fun saveHadith(item: HadithItem) = withContext(Dispatchers.IO) {
        hadithDao.insertHadith(HadithEntity.fromModel(item))
    }

    suspend fun deleteHadith(id: String) = withContext(Dispatchers.IO) {
        hadithDao.deleteHadithById(id)
    }

    suspend fun saveDua(item: DuaItem) = withContext(Dispatchers.IO) {
        duaDao.insertDua(DuaEntity.fromModel(item))
    }

    suspend fun deleteDua(id: String) = withContext(Dispatchers.IO) {
        duaDao.deleteDuaById(id)
    }

    suspend fun clearSurahAyahCache(surahNumber: Int) = withContext(Dispatchers.IO) {
        surahDao.clearCachedSurahAyahs(surahNumber)
    }

    suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        surahDao.deleteAllAyahs()
        surahDao.deleteAllSurahs()
        hadithDao.deleteAllHadiths()
        duaDao.deleteAllDuas()
    }

    companion object {
        val DEFAULT_SYNC_SURAHS = listOf(1, 18, 36, 55, 67, 103, 108, 112, 113, 114)
    }
}
