package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NoorCacheDatabase
import com.example.data.model.AyahItem
import com.example.data.model.DuaItem
import com.example.data.model.HadithItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class OfflineContentRepositoryTest {

    private lateinit var db: NoorCacheDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, NoorCacheDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun seedInitialCache_populatesSurahsHadithsDuasAndOfflineAyahs() = runBlocking {
        val repo = OfflineContentRepository(db)
        repo.seedInitialCacheIfNeeded()

        val surahs = repo.surahs.first()
        val hadiths = repo.hadiths.first()
        val duas = repo.duas.first()

        assertEquals(114, surahs.size)
        assertTrue(surahs.first { it.number == 1 }.isCachedOffline)
        assertTrue(hadiths.isNotEmpty())
        assertTrue(duas.isNotEmpty())

        val fatihaAyahs = repo.getOrFetchSurahAyahs(1)
        assertEquals(7, fatihaAyahs.size)
    }

    @Test
    fun getOrFetchSurahAyahs_cachesAndClearsSurahAyahsProperly() = runBlocking {
        var fetchCalls = 0
        val sampleAyahs = listOf(
            AyahItem(1, "الم", "Alif-Lam-Mim", "আলিফ-লাম-মীম।", "সংক্ষিপ্ত তাফসীর")
        )
        val repo = OfflineContentRepository(db) { _ ->
            fetchCalls++
            sampleAyahs
        }
        repo.seedInitialCacheIfNeeded()

        val firstRead = repo.getOrFetchSurahAyahs(2)
        val secondRead = repo.getOrFetchSurahAyahs(2)
        assertEquals(1, fetchCalls)
        assertEquals(sampleAyahs, firstRead)
        assertEquals(sampleAyahs, secondRead)
        assertTrue(repo.surahs.first().first { it.number == 2 }.isCachedOffline)

        repo.clearSurahAyahCache(2)
        assertFalse(repo.surahs.first().first { it.number == 2 }.isCachedOffline)
        assertTrue(db.surahDao().getAyahsForSurahOnce(2).isEmpty())
    }

    @Test
    fun daoQueriesInsertionsAndDeletions_workForQuranHadithAndDua() = runBlocking {
        val repo = OfflineContentRepository(db)
        repo.seedInitialCacheIfNeeded()

        // Search Surah query
        val kahfSearch = repo.observeSurahSearch("Kahf").first()
        assertEquals(1, kahfSearch.size)
        assertEquals(18, kahfSearch.first().number)

        // Insert, query by book, and delete Hadith
        val customHadith = HadithItem(
            id = "custom_h_1",
            book = "Bukhari",
            hadithNumber = "বুখারী - ৯৯৯৯",
            arabicText = "الدِّينُ النَّصِيحَةُ",
            banglaText = "দ্বীন হলো কল্যাণকামিতা।",
            simpleExplanation = "অন্যের মঙ্গল কামনা করা।",
            narrator = "তামিম আদ-দারী (রা.)"
        )
        repo.saveHadith(customHadith)
        assertTrue(repo.observeHadithsByBook("Bukhari").first().any { it.id == "custom_h_1" })
        repo.deleteHadith("custom_h_1")
        assertNull(db.hadithDao().getHadithById("custom_h_1"))

        // Insert, query by category, and delete Dua
        val customDua = DuaItem(
            id = "custom_d_1",
            category = "সকাল",
            titleBn = "অতিরিক্ত সকালের দোয়া",
            arabicText = "سُبْحَانَ اللَّهِ",
            banglaPronunciation = "সুবহানাল্লাহ",
            banglaMeaning = "আল্লাহ পবিত্র",
            reference = "মুসলিম"
        )
        repo.saveDua(customDua)
        assertTrue(repo.observeDuasByCategory("সকাল").first().any { it.id == "custom_d_1" })
        repo.deleteDua("custom_d_1")
        assertNull(db.duaDao().getDuaById("custom_d_1"))
    }

    @Test
    fun diModule_providesSingletonDatabaseAndRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db1 = com.example.di.NoorDatabaseModule.provideLocalDatabase(context)
        val db2 = com.example.di.NoorDatabaseModule.provideLocalDatabase(context)
        val repo1 = com.example.di.NoorDatabaseModule.provideOfflineContentRepository(context)
        val repo2 = com.example.di.NoorDatabaseModule.provideOfflineContentRepository(context)

        assertTrue(db1 === db2)
        assertTrue(repo1 === repo2)
    }

    @Test
    fun syncContentFromRemote_preservesExistingOfflineCacheWhenOneFeedFails() = runBlocking {
        val partialRemote = object : RemoteContentSyncSource {
            override suspend fun fetchSurahList(): List<com.example.data.model.SurahMeta> = emptyList()
            override suspend fun fetchSurahAyahs(surahNumber: Int): List<AyahItem> = emptyList()
            override suspend fun fetchHadiths(): List<HadithItem> = listOf(
                HadithItem(
                    id = "partial_h_1",
                    book = "Muslim",
                    hadithNumber = "সহীহ মুসলিম - ১০০",
                    arabicText = "",
                    banglaText = "নতুন হাদীস আপডেট।",
                    simpleExplanation = "ব্যাখ্যা",
                    narrator = "আবু হুরায়রা (রা.)"
                )
            )
            override suspend fun fetchDuas(): List<DuaItem> = throw java.io.IOException("Dua endpoint unavailable")
        }

        val repo = OfflineContentRepository(db = db, remoteSyncSource = partialRemote)
        val result = repo.syncContentFromRemote()

        assertEquals(1, result.hadithsUpdated)
        assertEquals(1, result.partialFailures)
        assertTrue(repo.hadiths.first().any { it.id == "partial_h_1" })
        // Existing seeded Duas remain intact despite remote Dua failure
        assertTrue(repo.duas.first().isNotEmpty())
    }
}
