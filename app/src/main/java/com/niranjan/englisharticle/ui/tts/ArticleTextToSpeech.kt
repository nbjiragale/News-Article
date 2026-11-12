package com.niranjan.englisharticle.ui.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
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
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var isReady = false
    private var isInitializing = false
    private var pendingSpeech: Pair<String, Locale>? = null

    private val _currentSpeechText = mutableStateOf<String?>(null)
    val currentSpeechText: State<String?> = _currentSpeechText

    /**
     * Long-form playback (a whole article). Android TTS refuses a single utterance longer than
     * [TextToSpeech.getMaxSpeechInputLength] (4,000 characters), so the text is queued as short
     * sentence-group utterances. That also gives pause/resume a place to restart from and
     * lets word highlighting map the engine's character ranges back to article words.
     *
     * Only touched on the main thread; engine callbacks are posted there.
     */
    private var longSession: LongSession? = null
    private var nextLongSessionId = 0

    private class LongSession(
        val id: Int,
        val plans: List<ChunkPlan>,
        val wordStarts: List<IntArray>,
        val onWordIndex: (Int?) -> Unit,
        val onComplete: () -> Unit
    ) {
        var currentChunk = 0
        var paused = false
        var waitingForEngine = false
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            mainHandler.post { onLongChunkStarted(utteranceId) }
        }

        override fun onDone(utteranceId: String?) {
            mainHandler.post {
                clearIfMatches(utteranceId)
                onLongChunkFinished(utteranceId)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            mainHandler.post {
                clearIfMatches(utteranceId)
                onLongChunkFinished(utteranceId)
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            mainHandler.post {
                clearIfMatches(utteranceId)
                onLongChunkFinished(utteranceId)
            }
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            // A stop is always started by us (pause, a new utterance, stop()), and that code
            // already updates the long session; treating it as "finished" would end playback.
            mainHandler.post { clearIfMatches(utteranceId) }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            mainHandler.post { onLongRangeStart(utteranceId, start) }
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
        // Some engines call onInit from inside the TextToSpeech constructor, before
        // ensureEngine() has stored the engine. ensureEngine() finishes the job in that case.
        if (engine == null) return
        if (isReady) onEngineReady() else onEngineFailed()
    }

    private fun onEngineReady() {
        engine?.setSpeechRate(0.92f)
        engine?.setOnUtteranceProgressListener(progressListener)
        val pending = pendingSpeech
        pendingSpeech = null
        pending?.let { (text, locale) -> speak(text, locale) }
        longSession?.takeIf { it.waitingForEngine }?.let { session ->
            session.waitingForEngine = false
            if (!session.paused) speakLongFrom(session, session.currentChunk)
        }
    }

    /**
     * The engine will never become usable. Drop everything waiting on it so no button stays
     * stuck in its "speaking" state, and let the next request try a fresh engine. Before, the
     * dead engine was kept, so every later request queued up behind it forever.
     */
    private fun onEngineFailed() {
        engine?.shutdown()
        engine = null
        isReady = false
        pendingSpeech = null
        _currentSpeechText.value = null
        finishLongSession()
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

    /**
     * Speaks a whole article. [onWordIndex] receives the index (among the `\S+` words of [text])
     * of the word being spoken, when the engine reports progress (API 26+); [onComplete] is
     * called once when the last part finishes or playback fails. Neither is called after
     * [stop], or after another speak call replaced this one.
     */
    fun speakLongEnglish(
        text: String,
        onWordIndex: (Int?) -> Unit,
        onComplete: () -> Unit
    ) {
        val cleanedText = text.trim()
        if (cleanedText.isBlank()) {
            onComplete()
            return
        }
        stop()

        val plans = buildChunkPlans(cleanedText.chunkForTts(LONG_CHUNK_CHARS))
        val session = LongSession(
            id = ++nextLongSessionId,
            plans = plans,
            wordStarts = plans.map { plan ->
                WORD_START_REGEX.findAll(plan.text).map { it.range.first }.toList().toIntArray()
            },
            onWordIndex = onWordIndex,
            onComplete = onComplete
        )
        longSession = session
        speakLongFrom(session, 0)
    }

    /** Pauses long playback. Android TTS cannot pause, so it stops and remembers the part. */
    fun pauseLong() {
        val session = longSession ?: return
        session.paused = true
        engine?.stop()
    }

    /** Resumes long playback from the start of the part that was playing. */
    fun resumeLong() {
        val session = longSession ?: return
        if (!session.paused) return
        session.paused = false
        speakLongFrom(session, session.currentChunk)
    }

    fun stop() {
        engine?.stop()
        pendingSpeech = null
        _currentSpeechText.value = null
        longSession = null
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        isReady = false
        isInitializing = false
        pendingSpeech = null
        longSession = null
        _currentSpeechText.value = null
    }

    private fun speak(text: String, locale: Locale) {
        val cleanedText = text.trim()
        if (cleanedText.isBlank()) return

        // A short utterance replaces any article playback (QUEUE_FLUSH drops its queue).
        longSession = null

        ensureEngine()
        if (engine == null) {
            // The engine failed to start during ensureEngine(); nothing can be spoken.
            _currentSpeechText.value = null
            return
        }
        if (!isReady) {
            pendingSpeech = cleanedText to locale
            _currentSpeechText.value = cleanedText
            return
        }

        val ttsEngine = engine ?: return
        if (!ttsEngine.trySetLanguage(locale)) {
            _currentSpeechText.value = null
            return
        }

        val utteranceId = "arthareader-${System.currentTimeMillis()}"
        lastUtteranceId = utteranceId
        _currentSpeechText.value = cleanedText
        ttsEngine.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun speakLongFrom(session: LongSession, startChunk: Int) {
        if (longSession !== session) return
        if (startChunk >= session.plans.size) {
            finishLongSession()
            return
        }

        ensureEngine()
        // ensureEngine() may have failed the engine and ended this session already.
        if (longSession !== session) return
        val ttsEngine = engine
        if (ttsEngine == null) {
            finishLongSession()
            return
        }
        if (!isReady) {
            session.waitingForEngine = true
            return
        }
        if (!ttsEngine.trySetLanguage(Locale.US)) {
            finishLongSession()
            return
        }

        session.currentChunk = startChunk
        for (index in startChunk until session.plans.size) {
            val mode = if (index == startChunk) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val result = ttsEngine.speak(
                session.plans[index].text,
                mode,
                null,
                longUtteranceId(session.id, index)
            )
            if (result != TextToSpeech.SUCCESS) {
                finishLongSession()
                return
            }
        }
        session.plans.getOrNull(startChunk)?.let { session.onWordIndex(it.firstGlobalWordIndex) }
    }

    private fun onLongChunkStarted(utteranceId: String?) {
        val (session, index) = longChunkFor(utteranceId) ?: return
        session.currentChunk = index
        session.onWordIndex(session.plans[index].firstGlobalWordIndex)
    }

    private fun onLongRangeStart(utteranceId: String?, start: Int) {
        val (session, index) = longChunkFor(utteranceId) ?: return
        val starts = session.wordStarts[index]
        var wordInChunk = -1
        for (i in starts.indices) {
            if (starts[i] <= start) wordInChunk = i else break
        }
        if (wordInChunk >= 0) {
            session.onWordIndex(session.plans[index].firstGlobalWordIndex + wordInChunk)
        }
    }

    private fun onLongChunkFinished(utteranceId: String?) {
        val (session, index) = longChunkFor(utteranceId) ?: return
        if (session.paused) return
        if (index >= session.plans.lastIndex) finishLongSession()
    }

    private fun longChunkFor(utteranceId: String?): Pair<LongSession, Int>? {
        val session = longSession ?: return null
        if (utteranceId == null || !utteranceId.startsWith(LONG_UTTERANCE_PREFIX)) return null
        val parts = utteranceId.removePrefix(LONG_UTTERANCE_PREFIX).split('-')
        if (parts.size != 2) return null
        val sessionId = parts[0].toIntOrNull() ?: return null
        val index = parts[1].toIntOrNull() ?: return null
        if (sessionId != session.id || index !in session.plans.indices) return null
        return session to index
    }

    private fun finishLongSession() {
        val session = longSession ?: return
        longSession = null
        session.onWordIndex(null)
        session.onComplete()
    }

    private fun longUtteranceId(sessionId: Int, index: Int) = "$LONG_UTTERANCE_PREFIX$sessionId-$index"

    private fun TextToSpeech.trySetLanguage(locale: Locale): Boolean {
        val languageResult = setLanguage(locale)
        return languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED
    }

    private fun ensureEngine() {
        if (engine != null || isInitializing) return

        isInitializing = true
        val tts = TextToSpeech(appContext, this)
        // Set listener early in case init succeeds before onInit hooks it up.
        tts.setOnUtteranceProgressListener(progressListener)
        engine = tts
        if (!isInitializing) {
            // onInit already ran inside the constructor; act on its result now.
            if (isReady) onEngineReady() else onEngineFailed()
        }
    }

    private companion object {
        /** Small enough that resume repeats little; far under the 4,000-character engine limit. */
        const val LONG_CHUNK_CHARS = 300
        const val LONG_UTTERANCE_PREFIX = "arthareader-long-"
        val WORD_START_REGEX = Regex("\\S+")
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
