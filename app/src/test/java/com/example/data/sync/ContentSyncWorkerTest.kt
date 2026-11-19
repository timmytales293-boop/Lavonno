package com.example.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.data.local.NoorCacheDatabase
import com.example.data.model.AyahItem
import com.example.data.model.DuaItem
import com.example.data.model.HadithItem
import com.example.data.model.SurahMeta
import com.example.data.repository.OfflineContentRepository
import com.example.data.repository.RemoteContentSyncSource
import com.example.guard.GuardPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class ContentSyncWorkerTest {

    private lateinit var context: Context
    private lateinit var db: NoorCacheDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NoorCacheDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        ContentSyncWorker.repositoryOverride = null
        db.close()
    }

    @Test
    fun doWork_syncsQuranHadithAndDuaIntoRoomAndReturnsSuccess() = runBlocking {
        val fakeRemote = object : RemoteContentSyncSource {
            override suspend fun fetchSurahList(): List<SurahMeta> = listOf(
                SurahMeta(
                    number = 1,
                    arabicName = "الفاتحة",
                    transliteration = "Al-Fatihah",
                    banglaName = "সূচনা (Updated)",
                    revelationType = "মাক্কী",
                    ayahCount = 7
                )
            )

            override suspend fun fetchSurahAyahs(surahNumber: Int): List<AyahItem> =
                if (surahNumber == 18) {
                    listOf(
                        AyahItem(
                            numberInSurah = 1,
                            arabicText = "الْحَمْدُ لِلَّهِ",
                            banglaTransliteration = "আলহামদুলিল্লাহ",
                            banglaMeaning = "সমস্ত প্রশংসা আল্লাহর জন্য।",
                            shortTafsir = "সূরা কাহফ আয়াত ১"
                        )
                    )
                } else {
                    emptyList()
                }

            override suspend fun fetchHadiths(): List<HadithItem> = listOf(
                HadithItem(
                    id = "sync_hadith_1",
                    book = "Bukhari",
                    hadithNumber = "সহীহ বুখারী - সিঙ্ক ১",
                    arabicText = "الدِّينُ النَّصِيحَةُ",
                    banglaText = "দ্বীন হলো কল্যাণকামিতা।",
                    simpleExplanation = "অন্যের মঙ্গল কামনা।",
                    narrator = "হযরত তামিম আদ-দারী (রা.)"
                )
            )

            override suspend fun fetchDuas(): List<DuaItem> = listOf(
                DuaItem(
                    id = "sync_dua_1",
                    category = "সকাল",
                    titleBn = "সিঙ্ককৃত সকালের দোয়া",
                    arabicText = "رَبِّ زِدْنِي عِلْمًا",
                    banglaPronunciation = "রাব্বি যিদনী ইলমা",
                    banglaMeaning = "হে আমার রব, আমার জ্ঞান বাড়িয়ে দিন।",
                    reference = "সূরা ত্বা-হা - ১১৪"
                )
            )
        }

        val repository = OfflineContentRepository(db = db, remoteSyncSource = fakeRemote)
        ContentSyncWorker.repositoryOverride = repository

        val worker = TestListenableWorkerBuilder<ContentSyncWorker>(context).build()
        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val output = (result as ListenableWorker.Result.Success).outputData
        assertEquals(1, output.getInt(ContentSyncWorker.KEY_SURAHS_SYNCED, -1))
        assertEquals(1, output.getInt(ContentSyncWorker.KEY_AYAHS_SYNCED, -1))
        assertEquals(1, output.getInt(ContentSyncWorker.KEY_HADITHS_SYNCED, -1))
        assertEquals(1, output.getInt(ContentSyncWorker.KEY_DUAS_SYNCED, -1))
        assertTrue(output.getLong(ContentSyncWorker.KEY_SYNCED_AT_MS, 0L) > 0L)

        // Verify Room holds synced items AND preserves isAyahsCached on Surah 1 + marks Surah 18 cached
        val surahs = repository.surahs.first()
        assertTrue(surahs.first { it.number == 1 }.isCachedOffline)
        assertEquals("সূচনা (Updated)", surahs.first { it.number == 1 }.banglaName)
        assertTrue(surahs.first { it.number == 18 }.isCachedOffline)
        assertEquals(1, repository.getOrFetchSurahAyahs(18).size)
        assertTrue(repository.hadiths.first().any { it.id == "sync_hadith_1" })
        assertTrue(repository.duas.first().any { it.id == "sync_dua_1" })

        val guardState = GuardPreferences(context).getCurrentState()
        assertTrue(guardState.lastContentSyncEpochMs > 0L)
        assertTrue(guardState.lastContentSyncSummary.contains("Hadiths:1"))
    }

    @Test
    fun doWork_retriesOnTransientFailureAndFailsAfterMaxAttempts() = runBlocking {
        val failingRemote = object : RemoteContentSyncSource {
            override suspend fun fetchSurahList(): List<SurahMeta> = throw IOException("No network")
            override suspend fun fetchSurahAyahs(surahNumber: Int): List<AyahItem> = throw IOException("No network")
            override suspend fun fetchHadiths(): List<HadithItem> = throw IOException("No network")
            override suspend fun fetchDuas(): List<DuaItem> = throw IOException("No network")
        }

        val repository = OfflineContentRepository(db = db, remoteSyncSource = failingRemote)
        ContentSyncWorker.repositoryOverride = repository

        val retryWorker = TestListenableWorkerBuilder<ContentSyncWorker>(context)
            .setRunAttemptCount(0)
            .build()
        assertEquals(ListenableWorker.Result.retry(), retryWorker.doWork())

        val exhaustedWorker = TestListenableWorkerBuilder<ContentSyncWorker>(context)
            .setRunAttemptCount(ContentSyncWorker.MAX_RETRY_ATTEMPTS)
            .build()
        val exhaustedResult = exhaustedWorker.doWork()
        assertTrue(exhaustedResult is ListenableWorker.Result.Failure)
    }

    @Test
    fun scheduler_buildsConnectedAndBatteryNotLowPeriodicRequest() {
        val request = ContentSyncScheduler.buildPeriodicSyncRequest()
        val constraints = request.workSpec.constraints
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        assertTrue(constraints.requiresBatteryNotLow())
        assertTrue(request.tags.contains(ContentSyncScheduler.SYNC_WORK_TAG))
    }
}
