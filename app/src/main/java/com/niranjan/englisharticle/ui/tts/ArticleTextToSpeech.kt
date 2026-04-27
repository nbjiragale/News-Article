package com.niranjan.englisharticle.ui.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class ArticleTextToSpeech(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var engine: TextToSpeech? = null
    private var isReady = false
    private var isInitializing = false
    private var pendingSpeech: Pair<String, Locale>? = null

    private val _currentSpeechText = mutableStateOf<String?>(null)
    val currentSpeechText: State<String?> = _currentSpeechText

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            // currentSpeechText is set synchronously in `speak`; nothing to do here.
        }

        override fun onDone(utteranceId: String?) {
            clearIfMatches(utteranceId)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            clearIfMatches(utteranceId)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            clearIfMatches(utteranceId)
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            clearIfMatches(utteranceId)
        }
    }

    private var lastUtteranceId: String? = null

    private fun clearIfMatches(utteranceId: String?) {
        if (utteranceId == null || utteranceId == lastUtteranceId) {
            _currentSpeechText.value = null
        }
    }

    override fun onInit(status: Int) {
        isInitializing = false
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) {
            engine?.setSpeechRate(0.92f)
            engine?.setOnUtteranceProgressListener(progressListener)
            pendingSpeech?.let { (text, locale) -> speak(text, locale) }
            pendingSpeech = null
        }
    }

    fun speakEnglish(text: String) {
        speak(text = text, locale = Locale.US)
    }

    fun speakKannada(text: String) {
        speak(
            text = text,
            locale = Locale.Builder()
                .setLanguage("kn")
                .setRegion("IN")
                .build()
        )
    }

    fun stop() {
        engine?.stop()
        pendingSpeech = null
        _currentSpeechText.value = null
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        isReady = false
        isInitializing = false
        pendingSpeech = null
        _currentSpeechText.value = null
    }

    private fun speak(text: String, locale: Locale) {
        val cleanedText = text.trim()
        if (cleanedText.isBlank()) return

        ensureEngine()
        if (!isReady) {
            pendingSpeech = cleanedText to locale
            _currentSpeechText.value = cleanedText
            return
        }

        val ttsEngine = engine ?: return
        val languageResult = ttsEngine.setLanguage(locale)
        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            _currentSpeechText.value = null
            return
        }

        val utteranceId = "arthareader-${System.currentTimeMillis()}"
        lastUtteranceId = utteranceId
        _currentSpeechText.value = cleanedText
        ttsEngine.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun ensureEngine() {
        if (engine != null || isInitializing) return

        isInitializing = true
        engine = TextToSpeech(appContext, this).also { tts ->
            // Set listener early in case init succeeds before onInit hooks it up.
            tts.setOnUtteranceProgressListener(progressListener)
        }
    }
}

@Composable
fun rememberArticleTextToSpeech(): ArticleTextToSpeech {
    val context = LocalContext.current
    val controller = remember(context) { ArticleTextToSpeech(context) }
    DisposableEffect(controller) {
        onDispose { controller.shutdown() }
    }
    return controller
}
