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

    /** Short English snippets (single words, phrases, sentence lookups). Always uses Android TTS. */
    fun speakEnglish(text: String) {
        deepgram.stop()
        _isArticleSpeaking.value = false
        systemTts.speakEnglish(text)
    }

    /** Long-form article / summary text. Uses Deepgram when configured; falls back to Android TTS. */
    fun speakArticleEnglish(text: String) {
        if (text.isBlank()) return
        systemTts.stop()

        if (deepgram.isConfigured) {
            _isArticleSpeaking.value = true
            deepgram.speak(
                text = text,
                onUnavailable = { fallbackText ->
                    _isArticleSpeaking.value = false
                    systemTts.speakEnglish(fallbackText)
                },
                onComplete = { _isArticleSpeaking.value = false }
            )
        } else {
            systemTts.speakEnglish(text)
        }
    }

    fun speakKannada(text: String) {
        deepgram.stop()
        _isArticleSpeaking.value = false
        systemTts.speakKannada(text)
    }

    fun stop() {
        deepgram.stop()
        systemTts.stop()
        _isArticleSpeaking.value = false
    }

    fun shutdown() {
        deepgram.shutdown()
        systemTts.shutdown()
        _isArticleSpeaking.value = false
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
