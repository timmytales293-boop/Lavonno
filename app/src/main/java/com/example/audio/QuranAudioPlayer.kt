package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuranPlaybackState(
    val surahNumber: Int? = null,
    val isPreparing: Boolean = false,
    val isPlaying: Boolean = false,
    val error: String? = null
)

class QuranAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private val _state = MutableStateFlow(QuranPlaybackState())
    val state: StateFlow<QuranPlaybackState> = _state.asStateFlow()

    fun togglePlay(surahNumber: Int, streamUrl: String) {
        val current = _state.value
        if (current.surahNumber == surahNumber && mediaPlayer != null && !current.isPreparing) {
            val player = mediaPlayer!!
            if (player.isPlaying) {
                player.pause()
                _state.value = current.copy(isPlaying = false, error = null)
            } else {
                player.start()
                _state.value = current.copy(isPlaying = true, error = null)
            }
            return
        }

        release()
        _state.value = QuranPlaybackState(
            surahNumber = surahNumber,
            isPreparing = true,
            isPlaying = false,
            error = null
        )

        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setOnPreparedListener { mp ->
                mp.start()
                _state.value = QuranPlaybackState(
                    surahNumber = surahNumber,
                    isPreparing = false,
                    isPlaying = true,
                    error = null
                )
            }
            setOnCompletionListener {
                _state.value = QuranPlaybackState(
                    surahNumber = surahNumber,
                    isPreparing = false,
                    isPlaying = false,
                    error = null
                )
            }
            setOnErrorListener { _, _, _ ->
                _state.value = QuranPlaybackState(
                    surahNumber = surahNumber,
                    isPreparing = false,
                    isPlaying = false,
                    error = "তিলাওয়াত লোড করা যায়নি, ইন্টারনেট চেক করুন।"
                )
                true
            }
        }
        mediaPlayer = player

        runCatching {
            player.setDataSource(streamUrl)
            player.prepareAsync()
        }.onFailure { err ->
            _state.value = QuranPlaybackState(
                surahNumber = surahNumber,
                isPreparing = false,
                isPlaying = false,
                error = err.localizedMessage ?: "Audio error"
            )
        }
    }

    fun release() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        _state.value = QuranPlaybackState()
    }
}
