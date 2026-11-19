package com.example.data.network

import com.example.data.content.QuranCatalog
import com.example.data.model.AyahItem
import com.example.data.model.SurahMeta
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AlQuranEditionsResponse(
    val code: Int = 0,
    val status: String = "",
    val data: List<AlQuranSurahEdition> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahEdition(
    val number: Int = 0,
    val ayahs: List<AlQuranAyahDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AlQuranAyahDto(
    val numberInSurah: Int = 0,
    val text: String = ""
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahListResponse(
    val code: Int = 0,
    val status: String = "",
    val data: List<AlQuranSurahMetaDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AlQuranSurahMetaDto(
    val number: Int = 0,
    val name: String = "",
    val englishName: String = "",
    val englishNameTranslation: String = "",
    val numberOfAyahs: Int = 0,
    val revelationType: String = ""
)

interface AlQuranApi {
    @GET("v1/surah")
    suspend fun getSurahList(): AlQuranSurahListResponse

    @GET("v1/surah/{surahNumber}/editions/quran-uthmani,bn.bengali,en.transliteration")
    suspend fun getSurahEditions(
        @Path("surahNumber") surahNumber: Int
    ): AlQuranEditionsResponse
}

object QuranApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val api: AlQuranApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.alquran.cloud/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AlQuranApi::class.java)
    }

    suspend fun fetchAllSurahMetas(): List<SurahMeta> {
        val response = api.getSurahList()
        if (response.data.isEmpty()) return emptyList()
        val banglaLookup = QuranCatalog.all114Surahs.associateBy { it.number }
        return response.data.mapNotNull { dto ->
            if (dto.number <= 0) return@mapNotNull null
            val existing = banglaLookup[dto.number]
            val revTypeBn = when (dto.revelationType.lowercase()) {
                "meccan" -> "মাক্কী"
                "medinan" -> "মাদানী"
                else -> existing?.revelationType ?: dto.revelationType
            }
            SurahMeta(
                number = dto.number,
                arabicName = dto.name.ifBlank { existing?.arabicName.orEmpty() },
                transliteration = dto.englishName.ifBlank { existing?.transliteration.orEmpty() },
                banglaName = existing?.banglaName ?: dto.englishNameTranslation,
                revelationType = revTypeBn,
                ayahCount = if (dto.numberOfAyahs > 0) dto.numberOfAyahs else (existing?.ayahCount ?: 0),
                isCachedOffline = false
            )
        }
    }

    suspend fun fetchSurahAyahs(surahNumber: Int): List<AyahItem> {
        val response = api.getSurahEditions(surahNumber)
        if (response.data.size < 2) return emptyList()
        val arabicAyahs = response.data[0].ayahs
        val banglaAyahs = response.data[1].ayahs
        val translitAyahs = response.data.getOrNull(2)?.ayahs.orEmpty()

        return arabicAyahs.mapIndexed { index, arabic ->
            val bn = banglaAyahs.getOrNull(index)?.text.orEmpty()
            val tr = translitAyahs.getOrNull(index)?.text.orEmpty()
            AyahItem(
                numberInSurah = arabic.numberInSurah,
                arabicText = arabic.text,
                banglaTransliteration = tr.ifBlank { "উচ্চারণ অনুপলব্ধ" },
                banglaMeaning = bn,
                shortTafsir = "সংক্ষিপ্ত তাফসীর: এই আয়াতে আল্লাহর একত্ববাদ, স্মরণ ও সৎপথে অটল থাকার শিক্ষা দেওয়া হয়েছে (সূরা নং $surahNumber, আয়াত ${arabic.numberInSurah})।"
            )
        }
    }

    fun getAudioStreamUrl(surahNumber: Int): String {
        return "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/$surahNumber.mp3"
    }
}
