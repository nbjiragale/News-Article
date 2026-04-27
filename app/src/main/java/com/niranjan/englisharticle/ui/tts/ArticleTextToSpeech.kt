package com.niranjan.englisharticle.ui.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class ArticleTextToSpeech(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var engine: TextToSpeech? = null
    private var isReady = false
    private var isInitializing = false
    private var pendingSpeech: Pair<String, Locale>? = null

    override fun onInit(status: Int) {
        isInitializing = false
        isReady = status == TextToSpeech.SUCCESS
        if (isReady) {
            engine?.setSpeechRate(0.92f)
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
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        isReady = false
        isInitializing = false
        pendingSpeech = null
    }

    private fun speak(text: String, locale: Locale) {
        val cleanedText = text.trim()
        if (cleanedText.isBlank()) return

        ensureEngine()
        if (!isReady) {
            pendingSpeech = cleanedText to locale
            return
        }

        val ttsEngine = engine ?: return
        val languageResult = ttsEngine.setLanguage(locale)
        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            return
        }

        ttsEngine.speak(
            cleanedText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "arthareader-${System.currentTimeMillis()}"
        )
    }

    private fun ensureEngine() {
        if (engine != null || isInitializing) return

        isInitializing = true
        engine = TextToSpeech(appContext, this)
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
