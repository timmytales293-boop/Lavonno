package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AyahItem
import com.example.data.model.DuaItem
import com.example.data.model.HadithItem
import com.example.data.model.SurahMeta

@Entity(tableName = "cached_surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    val arabicName: String,
    val transliteration: String,
    val banglaName: String,
    val revelationType: String,
    val ayahCount: Int,
    val isAyahsCached: Boolean = false
) {
    fun toModel(): SurahMeta = SurahMeta(
        number = number,
        arabicName = arabicName,
        transliteration = transliteration,
        banglaName = banglaName,
        revelationType = revelationType,
        ayahCount = ayahCount,
        isCachedOffline = isAyahsCached
    )

    companion object {
        fun fromModel(meta: SurahMeta, isAyahsCached: Boolean = false): SurahEntity = SurahEntity(
            number = meta.number,
            arabicName = meta.arabicName,
            transliteration = meta.transliteration,
            banglaName = meta.banglaName,
            revelationType = meta.revelationType,
            ayahCount = meta.ayahCount,
            isAyahsCached = isAyahsCached
        )
    }
}

@Entity(
    tableName = "cached_ayahs",
    primaryKeys = ["surahNumber", "numberInSurah"],
    indices = [Index(value = ["surahNumber"])]
)
data class AyahEntity(
    val surahNumber: Int,
    val numberInSurah: Int,
    val arabicText: String,
    val banglaTransliteration: String,
    val banglaMeaning: String,
    val shortTafsir: String,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toModel(): AyahItem = AyahItem(
        numberInSurah = numberInSurah,
        arabicText = arabicText,
        banglaTransliteration = banglaTransliteration,
        banglaMeaning = banglaMeaning,
        shortTafsir = shortTafsir
    )

    companion object {
        fun fromModel(surahNumber: Int, item: AyahItem): AyahEntity = AyahEntity(
            surahNumber = surahNumber,
            numberInSurah = item.numberInSurah,
            arabicText = item.arabicText,
            banglaTransliteration = item.banglaTransliteration,
            banglaMeaning = item.banglaMeaning,
            shortTafsir = item.shortTafsir
        )
    }
}

@Entity(
    tableName = "cached_hadiths",
    indices = [Index(value = ["book"])]
)
data class HadithEntity(
    @PrimaryKey val id: String,
    val book: String,
    val hadithNumber: String,
    val arabicText: String,
    val banglaText: String,
    val simpleExplanation: String,
    val narrator: String,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toModel(): HadithItem = HadithItem(
        id = id,
        book = book,
        hadithNumber = hadithNumber,
        arabicText = arabicText,
        banglaText = banglaText,
        simpleExplanation = simpleExplanation,
        narrator = narrator
    )

    companion object {
        fun fromModel(item: HadithItem): HadithEntity = HadithEntity(
            id = item.id,
            book = item.book,
            hadithNumber = item.hadithNumber,
            arabicText = item.arabicText,
            banglaText = item.banglaText,
            simpleExplanation = item.simpleExplanation,
            narrator = item.narrator
        )
    }
}

@Entity(
    tableName = "cached_duas",
    indices = [Index(value = ["category"])]
)
data class DuaEntity(
    @PrimaryKey val id: String,
    val category: String,
    val titleBn: String,
    val arabicText: String,
    val banglaPronunciation: String,
    val banglaMeaning: String,
    val reference: String,
    val cachedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun toModel(): DuaItem = DuaItem(
        id = id,
        category = category,
        titleBn = titleBn,
        arabicText = arabicText,
        banglaPronunciation = banglaPronunciation,
        banglaMeaning = banglaMeaning,
        reference = reference
    )

    companion object {
        fun fromModel(item: DuaItem): DuaEntity = DuaEntity(
            id = item.id,
            category = item.category,
            titleBn = item.titleBn,
            arabicText = item.arabicText,
            banglaPronunciation = item.banglaPronunciation,
            banglaMeaning = item.banglaMeaning,
            reference = item.reference
        )
    }
}
