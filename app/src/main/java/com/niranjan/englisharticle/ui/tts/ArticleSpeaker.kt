package com.niranjan.englisharticle.ui.tts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.niranjan.englisharticle.BuildConfig

enum class ArticlePlaybackState { Idle, Playing, Paused }

class ArticleSpeaker(
    context: Context,
    deepgramApiKey: String = BuildConfig.DEEPGRAM_API_KEY,
    deepgramVoice: String = BuildConfig.DEEPGRAM_VOICE,
    deepgramAudioOffsetMs: Int = BuildConfig.DEEPGRAM_AUDIO_OFFSET_MS
) {
    private val systemTts = ArticleTextToSpeech(context)
    private val deepgram = DeepgramTextToSpeech(
        context = context,
        apiKey = deepgramApiKey,
        voice = deepgramVoice,
        audioOffsetMs = deepgramAudioOffsetMs
    )

    /** Which engine is reading the article right now, so pause/resume reach the right one. */
    private enum class ArticleEngine { None, Deepgram, System }

    private var articleEngine = ArticleEngine.None

    /** Bumped on every new article playback or stop, so a replaced playback's callbacks are ignored. */
    private var articleSession = 0

    private val _playbackState = mutableStateOf(ArticlePlaybackState.Idle)
    val playbackState: State<ArticlePlaybackState> = _playbackState

    val isArticleSpeaking: State<Boolean> = derivedStateOf {
        _playbackState.value != ArticlePlaybackState.Idle
    }

    private val _currentWordIndex = mutableStateOf<Int?>(null)
    val currentWordIndex: State<Int?> = _currentWordIndex

    /**
     * Text currently being spoken via the short-speech (Android TTS) path. UI buttons can
     * compare their own text to this to render a stop affordance for the active utterance.
     */
    val currentShortSpeechText: State<String?> = systemTts.currentSpeechText

    /**
     * Short English snippets (single words, phrases, sentence lookups, the summary). Always uses
     * Android TTS, and never drives the article's play state or word highlight.
     */
    fun speakEnglish(text: String) {
        endArticlePlayback()
        systemTts.speakEnglish(text)
    }

    fun toggleEnglish(text: String) {
        if (currentShortSpeechText.value == text.trim()) {
            stopShortSpeech()
        } else {
            speakEnglish(text)
        }
    }

    fun toggleKannada(text: String) {
        if (currentShortSpeechText.value == text.trim()) {
            stopShortSpeech()
        } else {
            speakKannada(text)
        }
    }

    /** Stops only the short-speech (Android TTS) path; leaves any article playback alone. */
    fun stopShortSpeech() {
        if (articleEngine == ArticleEngine.System) return
        systemTts.stop()
    }

    /**
     * Long-form article text. Uses Deepgram when configured; otherwise, or when Deepgram fails
     * part way, Android TTS reads the rest.
     *
     * [globalWordOffset] is added to every word index emitted via [currentWordIndex], so the UI
     * can map back to its absolute token list when only a slice of the article is being read.
     */
    fun speakArticleEnglish(text: String, globalWordOffset: Int = 0) {
        if (text.isBlank()) return
        endArticlePlayback()
        val session = ++articleSession

        _playbackState.value = ArticlePlaybackState.Playing
        _currentWordIndex.value = null

        fun onWordIndex(relative: Int?) {
            if (session != articleSession) return
            _currentWordIndex.value = relative?.let { it + globalWordOffset }
        }

        fun onComplete() {
            if (session != articleSession) return
            articleEngine = ArticleEngine.None
            _playbackState.value = ArticlePlaybackState.Idle
            _currentWordIndex.value = null
        }

        fun speakWithSystemTts(fallbackText: String, firstWordIndex: Int) {
            if (session != articleSession) return
            val wasPaused = _playbackState.value == ArticlePlaybackState.Paused
            articleEngine = ArticleEngine.System
            systemTts.speakLongEnglish(
                text = fallbackText,
                onWordIndex = { relative -> onWordIndex(relative?.let { it + firstWordIndex }) },
                onComplete = ::onComplete
            )
            if (wasPaused) systemTts.pauseLong()
        }

        if (deepgram.isConfigured) {
            articleEngine = ArticleEngine.Deepgram
            deepgram.speak(
                text = text,
                onUnavailable = ::speakWithSystemTts,
                onWordIndex = ::onWordIndex,
                onComplete = ::onComplete
            )
        } else {
            speakWithSystemTts(text, 0)
        }
    }

    fun pauseArticle() {
        if (_playbackState.value != ArticlePlaybackState.Playing) return
        when (articleEngine) {
            ArticleEngine.Deepgram -> deepgram.pause()
            ArticleEngine.System -> systemTts.pauseLong()
            ArticleEngine.None -> return
        }
        _playbackState.value = ArticlePlaybackState.Paused
    }

    fun resumeArticle() {
        if (_playbackState.value != ArticlePlaybackState.Paused) return
        when (articleEngine) {
            ArticleEngine.Deepgram -> deepgram.resume()
            ArticleEngine.System -> systemTts.resumeLong()
            ArticleEngine.None -> return
        }
        _playbackState.value = ArticlePlaybackState.Playing
    }

    fun speakKannada(text: String) {
        endArticlePlayback()
        systemTts.speakKannada(text)
    }

    fun stop() {
        endArticlePlayback()
        systemTts.stop()
    }

    fun shutdown() {
        endArticlePlayback()
        deepgram.shutdown()
        systemTts.shutdown()
    }

    /** Stops article playback on either engine and resets its state. Leaves short speech alone. */
    private fun endArticlePlayback() {
        articleSession++
        deepgram.stop()
        if (articleEngine == ArticleEngine.System) systemTts.stop()
        articleEngine = ArticleEngine.None
        _playbackState.value = ArticlePlaybackState.Idle
        _currentWordIndex.value = null
    }
}

/**
 * Returns the app's [ArticleSpeaker], kept in a ViewModel so it survives configuration
 * changes. It used to be remembered per Activity and shut down on dispose, so rotating the
 * phone stopped the article being read aloud.
 */
@Composable
fun rememberArticleSpeaker(): ArticleSpeaker {
    val appContext = LocalContext.current.applicationContext
    val holder: ArticleSpeakerHolder = viewModel(
        factory = remember(appContext) { ArticleSpeakerHolder.factory(appContext) }
    )
    return holder.speaker
}

/** Owns the [ArticleSpeaker] for as long as the screen's ViewModel store lives. */
class ArticleSpeakerHolder(context: Context) : ViewModel() {
    val speaker = ArticleSpeaker(context.applicationContext)

    override fun onCleared() {
        speaker.shutdown()
    }

    companion object {
        fun factory(context: Context) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ArticleSpeakerHolder::class.java)) {
                    "Unknown ViewModel class: ${modelClass.name}"
                }
                return ArticleSpeakerHolder(context) as T
            }
        }
    }
}
