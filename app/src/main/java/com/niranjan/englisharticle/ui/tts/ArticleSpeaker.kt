package com.niranjan.englisharticle.ui.tts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.niranjan.englisharticle.BuildConfig

enum class ArticlePlaybackState { Idle, Playing, Paused }

class ArticleSpeaker(
    context: Context,
    deepgramApiKey: String = BuildConfig.DEEPGRAM_API_KEY,
    deepgramVoice: String = BuildConfig.DEEPGRAM_VOICE
) {
    private val systemTts = ArticleTextToSpeech(context)
    private val deepgram = DeepgramTextToSpeech(
        context = context,
        apiKey = deepgramApiKey,
        voice = deepgramVoice
    )

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

    /** Short English snippets (single words, phrases, sentence lookups). Always uses Android TTS. */
    fun speakEnglish(text: String) {
        deepgram.stop()
        _playbackState.value = ArticlePlaybackState.Idle
        _currentWordIndex.value = null
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
        systemTts.stop()
    }

    /**
     * Long-form article / summary text. Uses Deepgram when configured; falls back to Android TTS.
     *
     * [globalWordOffset] is added to every word index emitted via [currentWordIndex], so the UI
     * can map back to its absolute token list when only a slice of the article is being read.
     */
    fun speakArticleEnglish(text: String, globalWordOffset: Int = 0) {
        if (text.isBlank()) return
        systemTts.stop()

        if (deepgram.isConfigured) {
            _playbackState.value = ArticlePlaybackState.Playing
            _currentWordIndex.value = null
            deepgram.speak(
                text = text,
                onUnavailable = { fallbackText ->
                    _playbackState.value = ArticlePlaybackState.Idle
                    _currentWordIndex.value = null
                    systemTts.speakEnglish(fallbackText)
                },
                onWordIndex = { relative ->
                    _currentWordIndex.value = relative?.let { it + globalWordOffset }
                },
                onComplete = {
                    _playbackState.value = ArticlePlaybackState.Idle
                    _currentWordIndex.value = null
                }
            )
        } else {
            systemTts.speakEnglish(text)
        }
    }

    fun pauseArticle() {
        if (_playbackState.value != ArticlePlaybackState.Playing) return
        deepgram.pause()
        _playbackState.value = ArticlePlaybackState.Paused
    }

    fun resumeArticle() {
        if (_playbackState.value != ArticlePlaybackState.Paused) return
        deepgram.resume()
        _playbackState.value = ArticlePlaybackState.Playing
    }

    fun speakKannada(text: String) {
        deepgram.stop()
        _playbackState.value = ArticlePlaybackState.Idle
        _currentWordIndex.value = null
        systemTts.speakKannada(text)
    }

    fun stop() {
        deepgram.stop()
        systemTts.stop()
        _playbackState.value = ArticlePlaybackState.Idle
        _currentWordIndex.value = null
    }

    fun shutdown() {
        deepgram.shutdown()
        systemTts.shutdown()
        _playbackState.value = ArticlePlaybackState.Idle
        _currentWordIndex.value = null
    }
}

@Composable
fun rememberArticleSpeaker(): ArticleSpeaker {
    val context = LocalContext.current
    val speaker = remember(context) { ArticleSpeaker(context) }
    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }
    return speaker
}
