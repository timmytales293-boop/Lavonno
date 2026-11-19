package com.example.data.network

import com.example.data.model.DuaItem
import com.example.data.model.HadithItem
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class RemoteHadithEditionResponse(
    val hadiths: List<RemoteHadithDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RemoteHadithDto(
    val hadithnumber: Double = 0.0,
    val arabicnumber: Double = 0.0,
    val text: String = ""
)

@JsonClass(generateAdapter = true)
data class RemoteAyahEditionsResponse(
    val code: Int = 0,
    val status: String = "",
    val data: List<RemoteSingleAyahEditionDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RemoteSingleAyahEditionDto(
    val text: String = ""
)

interface NoorHadithRemoteApi {
    @GET("editions/ben-bukhari.min.json")
    suspend fun getBukhariBangla(): RemoteHadithEditionResponse

    @GET("editions/ben-muslim.min.json")
    suspend fun getMuslimBangla(): RemoteHadithEditionResponse
}

interface NoorQuranDuaRemoteApi {
    @GET("v1/ayah/{reference}/editions/quran-uthmani,bn.bengali,en.transliteration")
    suspend fun getQuranicDuaAyah(
        @Path("reference") reference: String
    ): RemoteAyahEditionsResponse
}

object NoorSyncApiService {

    private const val HADITH_BASE_URL = "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/"
    private const val QURAN_DUA_BASE_URL = "https://api.alquran.cloud/"

    private data class QuranDuaSpec(
        val id: String,
        val ayahRef: String,
        val category: String,
        val titleBn: String,
        val referenceLabel: String
    )

    private val syncedQuranDuaSpecs = listOf(
        QuranDuaSpec(
            id = "remote_dua_rabbana_201",
            ayahRef = "2:201",
            category = "সকাল",
            titleBn = "দুনিয়া ও আখিরাতের কল্যাণের দোয়া",
            referenceLabel = "সূরা আল-বাকারাহ - ২০১"
        ),
        QuranDuaSpec(
            id = "remote_dua_rabbi_ilm_114",
            ayahRef = "20:114",
            category = "সকাল",
            titleBn = "জ্ঞান ও প্রজ্ঞা বৃদ্ধির দোয়া",
            referenceLabel = "সূরা ত্বা-হা - ১১৪"
        ),
        QuranDuaSpec(
            id = "remote_dua_yunus_87",
            ayahRef = "21:87",
            category = "অজু ও গুনাহ থেকে বাঁচা",
            titleBn = "বিপদ ও গুনাহ থেকে মুক্তির দোয়া (দোয়া ইউনুস)",
            referenceLabel = "সূরা আল-আম্বিয়া - ৮৭"
        ),
        QuranDuaSpec(
            id = "remote_dua_maghfirah_8",
            ayahRef = "3:8",
            category = "রাত",
            titleBn = "হিদায়াতের ওপর অটল থাকার দোয়া",
            referenceLabel = "সূরা আলে ইমরান - ৮"
        )
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val hadithApi: NoorHadithRemoteApi by lazy {
        Retrofit.Builder()
            .baseUrl(HADITH_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(NoorHadithRemoteApi::class.java)
    }

    private val duaApi: NoorQuranDuaRemoteApi by lazy {
        Retrofit.Builder()
            .baseUrl(QURAN_DUA_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(NoorQuranDuaRemoteApi::class.java)
    }

    suspend fun fetchRemoteHadiths(limitPerBook: Int = 15): List<HadithItem> {
        val results = mutableListOf<HadithItem>()

        val bukhari = hadithApi.getBukhariBangla()
            .hadiths
            .asSequence()
            .filter { it.text.isNotBlank() && it.hadithnumber > 0 }
            .take(limitPerBook)
            .map { dto ->
                val numInt = dto.hadithnumber.toInt()
                HadithItem(
                    id = "remote_bukhari_$numInt",
                    book = "Bukhari",
                    hadithNumber = "সহীহ বুখারী - হাদীস $numInt",
                    arabicText = "",
                    banglaText = dto.text.trim(),
                    simpleExplanation = "সহীহ বুখারী থেকে অনলাইন সিঙ্ককৃত হাদীস (নং $numInt)।",
                    narrator = "সহীহ বুখারী"
                )
            }
            .toList()
        results.addAll(bukhari)

        val muslim = runCatching {
            hadithApi.getMuslimBangla()
                .hadiths
                .asSequence()
                .filter { it.text.isNotBlank() && it.hadithnumber > 0 }
                .take(limitPerBook)
                .map { dto ->
                    val numInt = dto.hadithnumber.toInt()
                    HadithItem(
                        id = "remote_muslim_$numInt",
                        book = "Muslim",
                        hadithNumber = "সহীহ মুসলিম - হাদীস $numInt",
                        arabicText = "",
                        banglaText = dto.text.trim(),
                        simpleExplanation = "সহীহ মুসলিম থেকে অনলাইন সিঙ্ককৃত হাদীস (নং $numInt)।",
                        narrator = "সহীহ মুসলিম"
                    )
                }
                .toList()
        }.getOrDefault(emptyList())
        results.addAll(muslim)

        return results
    }

    suspend fun fetchRemoteDuas(): List<DuaItem> {
        val results = mutableListOf<DuaItem>()
        var lastError: Throwable? = null
        for (spec in syncedQuranDuaSpecs) {
            try {
                val response = duaApi.getQuranicDuaAyah(spec.ayahRef)
                val arabic = response.data.getOrNull(0)?.text.orEmpty()
                val bangla = response.data.getOrNull(1)?.text.orEmpty()
                val translit = response.data.getOrNull(2)?.text.orEmpty()
                if (arabic.isNotBlank() && bangla.isNotBlank()) {
                    results.add(
                        DuaItem(
                            id = spec.id,
                            category = spec.category,
                            titleBn = spec.titleBn,
                            arabicText = arabic,
                            banglaPronunciation = translit.ifBlank { bangla },
                            banglaMeaning = bangla,
                            reference = spec.referenceLabel
                        )
                    )
                }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                lastError = t
            }
        }
        if (results.isEmpty() && lastError != null) {
            throw lastError
        }
        return results
    }
}
