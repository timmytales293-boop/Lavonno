package com.example.audio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.content.SelfDefenseCatalog
import com.example.guard.GuardPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NoorVoiceEngineTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun selectBestFemaleVoice_prefersInstalledOfflineBanglaFemaleOverMaleAndUninstalled() {
        val candidates = listOf(
            VoiceCandidateDescriptor(
                name = "bn-in-x-bnm-local",
                language = "bn",
                country = "IN",
                isNetworkRequired = false,
                isInstalled = true
            ),
            VoiceCandidateDescriptor(
                name = "bn-bd-x-ban-network",
                language = "bn",
                country = "BD",
                isNetworkRequired = true,
                isInstalled = false
            ),
            VoiceCandidateDescriptor(
                name = "bn-in-x-bnf-local",
                language = "bn",
                country = "IN",
                isNetworkRequired = false,
                isInstalled = true
            ),
            VoiceCandidateDescriptor(
                name = "bn-bd-x-ban-local",
                language = "bn",
                country = "BD",
                isNetworkRequired = false,
                isInstalled = true
            )
        )

        val selected = NoorVoiceEngine.selectBestFemaleVoice(candidates)
        assertNotNull(selected)
        assertEquals("bn-bd-x-ban-local", selected?.name)
        assertTrue(NoorVoiceEngine.isLikelyFemaleVoice(selected!!))

        val inFallback = NoorVoiceEngine.selectBestFemaleVoice(
            candidates.filter { it.country == "IN" }
        )
        assertEquals("bn-in-x-bnf-local", inFallback?.name)
        assertFalse(
            NoorVoiceEngine.isLikelyFemaleVoice(
                candidates.first { it.name == "bn-in-x-bnm-local" }
            )
        )
    }

    @Test
    fun selectBestFemaleVoice_returnsNullOnEmptyCandidates() {
        assertNull(NoorVoiceEngine.selectBestFemaleVoice(emptyList()))
    }

    @Test
    fun prepareSpeechText_translatesBanglishRemindersAndStripsMarkdown() {
        val greeting = NoorVoiceEngine.prepareSpeechText(
            "**Assalamualaikum Boss**, Ajker Din Ta Valo Hok ✓"
        )
        assertEquals("আসসালামু আলাইকুম বস, আজকের দিনটা ভালো হোক", greeting)

        val namaz = NoorVoiceEngine.prepareSpeechText(
            "# Boss Apnar Namaz Er Time Hoise, Oju Kore Asen • 5 Min Baki"
        )
        assertEquals("বস Apnar নামাজের সময় হয়েছে, ওজু করে আসুন , 5 মিনিট বাকি", namaz)

        val ff = NoorVoiceEngine.prepareSpeechText("FF Time Ses → Ebar Quran Poren")
        assertEquals("ফ্রি ফায়ার টাইম শেষ , এবার কুরআন পড়ুন", ff)
    }

    @Test
    fun guardPreferences_persistsFemaleVoiceToneAcrossInstances() {
        val prefs = GuardPreferences(context)
        prefs.setFemaleVoiceTone(FemaleVoiceTone.GENTLE_CALM)
        assertEquals(FemaleVoiceTone.GENTLE_CALM, prefs.state.value.femaleVoiceTone)

        val reloaded = GuardPreferences(context)
        assertEquals(FemaleVoiceTone.GENTLE_CALM, reloaded.state.value.femaleVoiceTone)

        reloaded.setFemaleVoiceTone(FemaleVoiceTone.WARM_NATURAL)
        assertEquals(FemaleVoiceTone.WARM_NATURAL, GuardPreferences(context).state.value.femaleVoiceTone)

        reloaded.setFemaleVoiceTone(FemaleVoiceTone.SOFT_MISTI)
        assertEquals(FemaleVoiceTone.SOFT_MISTI, GuardPreferences(context).state.value.femaleVoiceTone)
    }

    @Test
    fun selfDefenseCatalog_chutanoTechniquesAndDrillsAreCompleteAndIdsMatch() {
        val ids = SelfDefenseCatalog.chutanoTechniques.map { it.id }
        assertEquals(
            listOf("hat_dhora", "jama_orna", "pison_theke", "sprint_10s"),
            ids
        )
        SelfDefenseCatalog.chutanoTechniques.forEach { tech ->
            assertTrue("Missing steps for ${tech.id}", tech.stepsBn.isNotEmpty())
            assertTrue("Missing keyPoint for ${tech.id}", tech.keyPointBn.isNotBlank())
        }
        assertTrue(SelfDefenseCatalog.daily5MinDrills.isNotEmpty())
        assertEquals(7, SelfDefenseCatalog.sevenDayPlan.size)
        // Ensure vid_back maps to pison_theke in catalog
        assertTrue(SelfDefenseCatalog.chutanoTechniques.any { it.id == "pison_theke" })
    }
}
