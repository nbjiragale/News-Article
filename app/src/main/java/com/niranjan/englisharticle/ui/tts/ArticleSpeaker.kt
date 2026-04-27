package com.niranjan.englisharticle.ui.tts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.niranjan.englisharticle.BuildConfig

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

    private val _isArticleSpeaking = mutableStateOf(false)
    val isArticleSpeaking: State<Boolean> = _isArticleSpeaking

    private val _currentWordIndex = mutableStateOf<Int?>(null)
    val currentWordIndex: State<Int?> = _currentWordIndex

    /** Short English snippets (single words, phrases, sentence lookups). Always uses Android TTS. */
    fun speakEnglish(text: String) {
        deepgram.stop()
        _isArticleSpeaking.value = false
        _currentWordIndex.value = null
        systemTts.speakEnglish(text)
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
            _isArticleSpeaking.value = true
            _currentWordIndex.value = null
            deepgram.speak(
                text = text,
                onUnavailable = { fallbackText ->
                    _isArticleSpeaking.value = false
                    _currentWordIndex.value = null
                    systemTts.speakEnglish(fallbackText)
                },
                onWordIndex = { relative ->
                    _currentWordIndex.value = relative?.let { it + globalWordOffset }
                },
                onComplete = {
                    _isArticleSpeaking.value = false
                    _currentWordIndex.value = null
                }
            )
        } else {
            systemTts.speakEnglish(text)
        }
    }

    fun speakKannada(text: String) {
        deepgram.stop()
        _isArticleSpeaking.value = false
        _currentWordIndex.value = null
        systemTts.speakKannada(text)
    }

    fun stop() {
        deepgram.stop()
        systemTts.stop()
        _isArticleSpeaking.value = false
        _currentWordIndex.value = null
    }

    fun shutdown() {
        deepgram.shutdown()
        systemTts.shutdown()
        _isArticleSpeaking.value = false
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
