package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SurahDao {
    // --- Queries ---
    @Query("SELECT * FROM cached_surahs ORDER BY number ASC")
    fun observeAllSurahs(): Flow<List<SurahEntity>>

    @Query(
        "SELECT * FROM cached_surahs " +
            "WHERE transliteration LIKE '%' || :query || '%' " +
            "OR banglaName LIKE '%' || :query || '%' " +
            "OR CAST(number AS TEXT) = :query " +
            "ORDER BY number ASC"
    )
    fun searchSurahs(query: String): Flow<List<SurahEntity>>

    @Query("SELECT * FROM cached_ayahs WHERE surahNumber = :surahNumber ORDER BY numberInSurah ASC")
    fun observeAyahsForSurah(surahNumber: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM cached_ayahs WHERE surahNumber = :surahNumber ORDER BY numberInSurah ASC")
    suspend fun getAyahsForSurahOnce(surahNumber: Int): List<AyahEntity>

    @Query("SELECT COUNT(*) FROM cached_surahs")
    suspend fun countSurahs(): Int

    @Query("SELECT number FROM cached_surahs WHERE isAyahsCached = 1")
    suspend fun getCachedSurahNumbers(): List<Int>

    // --- Insertions & Updates ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurah(item: SurahEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(items: List<SurahEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(items: List<AyahEntity>)

    @Query("UPDATE cached_surahs SET isAyahsCached = :cached WHERE number = :surahNumber")
    suspend fun setSurahAyahsCached(surahNumber: Int, cached: Boolean = true)

    // --- Deletions ---
    @Query("DELETE FROM cached_ayahs WHERE surahNumber = :surahNumber")
    suspend fun deleteAyahsForSurah(surahNumber: Int)

    @Transaction
    suspend fun clearCachedSurahAyahs(surahNumber: Int) {
        deleteAyahsForSurah(surahNumber)
        setSurahAyahsCached(surahNumber, cached = false)
    }

    @Query("DELETE FROM cached_ayahs")
    suspend fun deleteAllAyahs()

    @Query("DELETE FROM cached_surahs")
    suspend fun deleteAllSurahs()
}

@Dao
interface HadithDao {
    // --- Queries ---
    @Query("SELECT * FROM cached_hadiths ORDER BY id ASC")
    fun observeAllHadiths(): Flow<List<HadithEntity>>

    @Query("SELECT * FROM cached_hadiths WHERE LOWER(book) = LOWER(:book) ORDER BY id ASC")
    fun observeHadithsByBook(book: String): Flow<List<HadithEntity>>

    @Query("SELECT * FROM cached_hadiths WHERE id = :id LIMIT 1")
    suspend fun getHadithById(id: String): HadithEntity?

    @Query("SELECT COUNT(*) FROM cached_hadiths")
    suspend fun countHadiths(): Int

    // --- Insertions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadith(item: HadithEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHadiths(items: List<HadithEntity>)

    // --- Deletions ---
    @Query("DELETE FROM cached_hadiths WHERE id = :id")
    suspend fun deleteHadithById(id: String)

    @Query("DELETE FROM cached_hadiths")
    suspend fun deleteAllHadiths()
}

@Dao
interface DuaDao {
    // --- Queries ---
    @Query("SELECT * FROM cached_duas ORDER BY id ASC")
    fun observeAllDuas(): Flow<List<DuaEntity>>

    @Query("SELECT * FROM cached_duas WHERE category = :category ORDER BY id ASC")
    fun observeDuasByCategory(category: String): Flow<List<DuaEntity>>

    @Query("SELECT * FROM cached_duas WHERE id = :id LIMIT 1")
    suspend fun getDuaById(id: String): DuaEntity?

    @Query("SELECT COUNT(*) FROM cached_duas")
    suspend fun countDuas(): Int

    // --- Insertions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDua(item: DuaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuas(items: List<DuaEntity>)

    // --- Deletions ---
    @Query("DELETE FROM cached_duas WHERE id = :id")
    suspend fun deleteDuaById(id: String)

    @Query("DELETE FROM cached_duas")
    suspend fun deleteAllDuas()
}
