package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class FemaleVoiceTone(
    val code: String,
    val labelBn: String,
    val pitch: Float,
    val speechRate: Float
) {
    SOFT_MISTI(
        code = "soft_misti",
        labelBn = "মিষ্টি ও কোমল",
        pitch = 1.15f,
        speechRate = 0.92f
    ),
    WARM_NATURAL(
        code = "warm_natural",
        labelBn = "স্বাভাবিক (Warm)",
        pitch = 1.07f,
        speechRate = 0.95f
    ),
    GENTLE_CALM(
        code = "gentle_calm",
        labelBn = "ধীর ও শান্ত",
        pitch = 1.11f,
        speechRate = 0.86f
    );

    companion object {
        fun fromCode(code: String?): FemaleVoiceTone =
            entries.firstOrNull { it.code == code } ?: SOFT_MISTI
    }
}

data class VoiceCandidateDescriptor(
    val name: String,
    val language: String,
    val country: String,
    val isNetworkRequired: Boolean = false,
    val isInstalled: Boolean = true
)

data class NoorVoiceProfileSelection(
    val voiceName: String?,
    val localeTag: String,
    val isFemaleProfileMatched: Boolean
)

data class VoiceEngineState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val activeItemId: String? = null,
    val lastSpokenText: String? = null,
    val activeTone: FemaleVoiceTone = FemaleVoiceTone.SOFT_MISTI,
    val activeVoiceName: String? = null,
    val isFemaleVoiceMatched: Boolean = false,
    val isBanglaSupported: Boolean = true
)

/**
 * Dedicated Female Voice ("Noor") + Dua/Namaz Audio Speech Engine built on Android TextToSpeech.
 * Selects an authentic Bangla Female voice profile when installed, tunes warm/soft pitch &
 * cadence, requests transient speech audio focus, and works 100% offline.
 */
class NoorVoiceEngine(
    context: Context,
    initialTone: FemaleVoiceTone = FemaleVoiceTone.SOFT_MISTI,
    private val ttsBuilder: ((Context, TextToSpeech.OnInitListener) -> TextToSpeech)? = null
) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var tts: TextToSpeech? = null
    private var isReady = false
    private var activeTone: FemaleVoiceTone = initialTone
    private var pendingSpeech: PendingSpeech? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private data class PendingSpeech(
        val text: String,
        val isRecitation: Boolean,
        val itemId: String?
    )

    private val _state = MutableStateFlow(VoiceEngineState(activeTone = initialTone))
    val state: StateFlow<VoiceEngineState> = _state.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                val engine = tts
                val selection = if (engine != null) {
                    configureFemaleVoiceProfile(engine, activeTone)
                } else {
                    NoorVoiceProfileSelection(null, "bn-BD", false)
                }
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = true,
                            activeItemId = utteranceId
                        )
                    }

                    override fun onDone(utteranceId: String?) {
                        abandonSpeechAudioFocus()
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            activeItemId = null
                        )
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        abandonSpeechAudioFocus()
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            activeItemId = null
                        )
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        abandonSpeechAudioFocus()
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            activeItemId = null
                        )
                    }
                })
                _state.value = _state.value.copy(
                    isReady = true,
                    activeVoiceName = selection.voiceName,
                    isFemaleVoiceMatched = selection.isFemaleProfileMatched,
                    isBanglaSupported = selection.localeTag.startsWith("bn")
                )
                pendingSpeech?.let { queued ->
                    pendingSpeech = null
                    speakInternal(
                        text = queued.text,
                        isRecitation = queued.isRecitation,
                        utteranceId = queued.itemId
                    )
                }
            } else {
                isReady = false
                _state.value = _state.value.copy(isReady = false)
            }
        }
        tts = ttsBuilder?.invoke(appContext, listener) ?: TextToSpeech(appContext, listener)
    }

    fun setVoiceTone(tone: FemaleVoiceTone) {
        activeTone = tone
        _state.value = _state.value.copy(activeTone = tone)
        val engine = tts
        if (isReady && engine != null) {
            val selection = configureFemaleVoiceProfile(engine, tone)
            _state.value = _state.value.copy(
                activeVoiceName = selection.voiceName,
                isFemaleVoiceMatched = selection.isFemaleProfileMatched
            )
        }
    }

    /**
     * Speaks a soft, misti Female Voice reminder, daily report, or Live Voice Chat reply as "Noor".
     */
    fun speakFemaleNoor(text: String, itemId: String = "noor_voice") {
        val clean = prepareSpeechText(text)
        if (clean.isBlank()) return
        if (!isReady) {
            pendingSpeech = PendingSpeech(clean, isRecitation = false, itemId = itemId)
            return
        }
        speakInternal(clean, isRecitation = false, utteranceId = itemId)
    }

    /**
     * Speaks Dua / Namaz Sura recitation & Bangla meaning clearly offline using Noor's female voice profile.
     */
    fun toggleSpeakDuaOrSurah(itemId: String, textToSpeak: String) {
        val current = _state.value
        if (current.isSpeaking && current.activeItemId == itemId) {
            stop()
            return
        }
        val clean = prepareSpeechText(textToSpeak)
        if (clean.isBlank()) return
        if (!isReady) {
            pendingSpeech = PendingSpeech(clean, isRecitation = true, itemId = itemId)
            return
        }
        speakInternal(clean, isRecitation = true, utteranceId = itemId)
    }

    private fun speakInternal(text: String, isRecitation: Boolean, utteranceId: String?) {
        val engine = tts ?: return
        val pitch = if (isRecitation) {
            (activeTone.pitch - 0.06f).coerceIn(1.0f, 1.15f)
        } else {
            activeTone.pitch
        }
        val rate = if (isRecitation) {
            (activeTone.speechRate - 0.04f).coerceIn(0.80f, 0.96f)
        } else {
            activeTone.speechRate
        }
        engine.setPitch(pitch)
        engine.setSpeechRate(rate)

        requestSpeechAudioFocus()
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        val id = utteranceId ?: "noor_utterance"
        _state.value = _state.value.copy(
            isSpeaking = true,
            activeItemId = id,
            lastSpokenText = text
        )
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, id)
    }

    fun stop() {
        pendingSpeech = null
        runCatching { tts?.stop() }
        abandonSpeechAudioFocus()
        _state.value = _state.value.copy(isSpeaking = false, activeItemId = null)
    }

    fun release() {
        pendingSpeech = null
        abandonSpeechAudioFocus()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        isReady = false
        _state.value = _state.value.copy(
            isReady = false,
            isSpeaking = false,
            activeItemId = null
        )
    }

    private fun requestSpeechAudioFocus() {
        val am = audioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(SPEECH_AUDIO_ATTRIBUTES)
                    .setOnAudioFocusChangeListener {}
                    .build()
                audioFocusRequest = focusReq
                am.requestAudioFocus(focusReq)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        }
    }

    private fun abandonSpeechAudioFocus() {
        val am = audioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
    }

    companion object {
        private val SPEECH_AUDIO_ATTRIBUTES: AudioAttributes by lazy {
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        }

        private val maleHintRegex = Regex("""(^|[-_#])(male|man|boy|bnm|m)($|[-_#])""")
        private val femaleHintRegex = Regex("""(^|[-_#])(female|woman|girl|bnf|bna|bnd|ban|sfg|f)($|[-_#])""")

        private val banglishSpeechMap: List<Pair<Regex, String>> = listOf(
            Regex("""\bAssalamualaikum\b""", RegexOption.IGNORE_CASE) to "আসসালামু আলাইকুম",
            Regex("""\bMashAllah\b""", RegexOption.IGNORE_CASE) to "মাশাআল্লাহ",
            Regex("""\bNamaz Er Time Hoise\b""", RegexOption.IGNORE_CASE) to "নামাজের সময় হয়েছে",
            Regex("""\bOju Kore Asen\b""", RegexOption.IGNORE_CASE) to "ওজু করে আসুন",
            Regex("""\bOju Kore Nin\b""", RegexOption.IGNORE_CASE) to "ওজু করে নিন",
            Regex("""\bPani Newar Time Hoise\b""", RegexOption.IGNORE_CASE) to "পানি নেওয়ার সময় হয়েছে",
            Regex("""\bEktu Pani Khan\b""", RegexOption.IGNORE_CASE) to "একটু পানি খান",
            Regex("""\bSorir Valo Thakbo\b""", RegexOption.IGNORE_CASE) to "শরীর ভালো থাকবে",
            Regex("""\bSelf Defence Practice Er Time Hoise\b""", RegexOption.IGNORE_CASE) to "সেলফ ডিফেন্স প্র্যাকটিসের সময় হয়েছে",
            Regex("""\bChutano Beyam Er Time Hoise\b""", RegexOption.IGNORE_CASE) to "ছুটানো ব্যায়ামের সময় হয়েছে",
            Regex("""\bPhone Beshi Choltase\b""", RegexOption.IGNORE_CASE) to "ফোন বেশি চলছে",
            Regex("""\bEktu Rakhen\b""", RegexOption.IGNORE_CASE) to "একটু রাখুন",
            Regex("""\bFF Time Ses\b""", RegexOption.IGNORE_CASE) to "ফ্রি ফায়ার টাইম শেষ",
            Regex("""\bEbar Quran Poren\b""", RegexOption.IGNORE_CASE) to "এবার কুরআন পড়ুন",
            Regex("""\bGhumer Time Hoise\b""", RegexOption.IGNORE_CASE) to "ঘুমের সময় হয়েছে",
            Regex("""\bPhone Rakhen\b""", RegexOption.IGNORE_CASE) to "ফোন রাখুন",
            Regex("""\bMon Kharap\b""", RegexOption.IGNORE_CASE) to "মন খারাপ",
            Regex("""\bAsen Ekta Dua Pori\b""", RegexOption.IGNORE_CASE) to "আসুন একটা দোয়া পড়ি",
            Regex("""\bAjker Din Ta Valo Hok\b""", RegexOption.IGNORE_CASE) to "আজকের দিনটা ভালো হোক",
            Regex("""\bAjke Onnek Valo Korsen\b""", RegexOption.IGNORE_CASE) to "আজকে অনেক ভালো করেছেন",
            Regex("""\bOju Hoise\b""", RegexOption.IGNORE_CASE) to "ওজু হয়েছে",
            Regex("""\bNamaz Ta Pore Felen\b""", RegexOption.IGNORE_CASE) to "নামাজটা পড়ে ফেলুন",
            Regex("""\bPractice Baki\b""", RegexOption.IGNORE_CASE) to "প্র্যাকটিস বাকি",
            Regex("""\b5 Min Matro\b""", RegexOption.IGNORE_CASE) to "মাত্র ৫ মিনিট",
            Regex("""\b5 Min Practice Koren\b""", RegexOption.IGNORE_CASE) to "৫ মিনিট প্র্যাকটিস করুন",
            Regex("""\bApni Parben\b""", RegexOption.IGNORE_CASE) to "আপনি পারবেন",
            Regex("""\bAllah Apnar Loge Ase\b""", RegexOption.IGNORE_CASE) to "আল্লাহ আপনার সাথে আছেন",
            Regex("""\bAllah Dekhtase Mama, Tham\b""", RegexOption.IGNORE_CASE) to "আল্লাহ দেখছেন, এখনই থামুন",
            Regex("""\bAjke 1 Page Quran Poren\b""", RegexOption.IGNORE_CASE) to "আজকে ১ পেজ কুরআন পড়ুন",
            Regex("""\b(\d+)\s*Min Baki\b""", RegexOption.IGNORE_CASE) to "$1 মিনিট বাকি",
            Regex("""\bBoss\b""", RegexOption.IGNORE_CASE) to "বস"
        )

        /**
         * Applies Noor's soft Female voice profile (AudioAttributes, locale, best female Voice,
         * pitch, and cadence) to any Android TextToSpeech instance (shared with NoorAlarmReceiver).
         */
        fun configureFemaleVoiceProfile(
            engine: TextToSpeech,
            tone: FemaleVoiceTone = FemaleVoiceTone.SOFT_MISTI
        ): NoorVoiceProfileSelection {
            runCatching { engine.setAudioAttributes(SPEECH_AUDIO_ATTRIBUTES) }

            val banglaBd = Locale("bn", "BD")
            var activeLocale = banglaBd
            val bdRes = runCatching { engine.setLanguage(banglaBd) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (bdRes == TextToSpeech.LANG_MISSING_DATA || bdRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                val banglaIn = Locale("bn", "IN")
                val inRes = runCatching { engine.setLanguage(banglaIn) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                if (inRes != TextToSpeech.LANG_MISSING_DATA && inRes != TextToSpeech.LANG_NOT_SUPPORTED) {
                    activeLocale = banglaIn
                }
            }

            val voices = runCatching { engine.voices.orEmpty() }.getOrDefault(emptyList())
            val descriptors = voices.map { v ->
                val features = runCatching { v.features.orEmpty() }.getOrDefault(emptySet())
                VoiceCandidateDescriptor(
                    name = v.name.orEmpty(),
                    language = v.locale?.language.orEmpty(),
                    country = v.locale?.country.orEmpty(),
                    isNetworkRequired = v.isNetworkConnectionRequired,
                    isInstalled = TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features
                )
            }

            val bestDescriptor = selectBestFemaleVoice(descriptors)
            val matchedVoice: Voice? = bestDescriptor?.let { target ->
                voices.firstOrNull { it.name == target.name }
            }
            if (matchedVoice != null) {
                runCatching { engine.voice = matchedVoice }
                matchedVoice.locale?.let { activeLocale = it }
            }

            runCatching {
                engine.setPitch(tone.pitch)
                engine.setSpeechRate(tone.speechRate)
            }

            return NoorVoiceProfileSelection(
                voiceName = matchedVoice?.name,
                localeTag = "${activeLocale.language}-${activeLocale.country}".trimEnd('-'),
                isFemaleProfileMatched = bestDescriptor != null && isLikelyFemaleVoice(bestDescriptor)
            )
        }

        fun isLikelyFemaleVoice(candidate: VoiceCandidateDescriptor): Boolean {
            val lower = candidate.name.lowercase(Locale.US)
            if (maleHintRegex.containsMatchIn(lower)) return false
            return femaleHintRegex.containsMatchIn(lower)
        }

        /**
         * Deterministically ranks available TTS voices to choose the best installed offline
         * Bangla Female voice profile ("Noor"), strongly penalizing male profiles.
         */
        fun selectBestFemaleVoice(candidates: Collection<VoiceCandidateDescriptor>): VoiceCandidateDescriptor? {
            if (candidates.isEmpty()) return null
            return candidates.maxByOrNull { c ->
                var score = 0
                val lower = c.name.lowercase(Locale.US)
                val lang = c.language.lowercase(Locale.US)
                val country = c.country.uppercase(Locale.US)

                if (c.isInstalled) score += 100 else score -= 200
                if (!c.isNetworkRequired) score += 25

                when {
                    lang == "bn" && country == "BD" -> score += 80
                    lang == "bn" && country == "IN" -> score += 65
                    lang == "bn" -> score += 55
                    lang == "en" -> score += 10
                }

                if (femaleHintRegex.containsMatchIn(lower)) score += 80
                if (lower.contains("female")) score += 10
                if (maleHintRegex.containsMatchIn(lower)) score -= 90

                score
            }
        }

        fun prepareSpeechText(raw: String): String {
            var text = raw
                .replace("*", "")
                .replace("#", "")
                .replace("✓", "")
                .replace("•", ",")
                .replace("→", ",")
                .replace("\n", ". ")
                .trim()

            for ((pattern, banglaReplacement) in banglishSpeechMap) {
                text = pattern.replace(text, banglaReplacement)
            }

            return text.replace(Regex("\\s+"), " ").take(1500).trim()
        }
    }
}
