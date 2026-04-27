package com.niranjan.englisharticle.ui.tts

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class DeepgramTextToSpeech(
    context: Context,
    private val apiKey: String,
    private val voice: String,
    private val audioOffsetMs: Int = 0
) {
    private val appContext = context.applicationContext
    private val cacheDir: File = File(appContext.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var activeJob: Job? = null
    private var player: MediaPlayer? = null

    @Volatile
    private var paused: Boolean = false

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    val isPaused: Boolean
        get() = paused && player != null

    val isActive: Boolean
        get() = activeJob?.isActive == true

    fun pause() {
        paused = true
        player?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.pause()
            }
        }
    }

    fun resume() {
        paused = false
        player?.let { mp ->
            runCatching {
                if (!mp.isPlaying) mp.start()
            }
        }
    }

    fun speak(
        text: String,
        onUnavailable: (String) -> Unit,
        onWordIndex: (Int?) -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        val cleaned = text.trim()
        if (cleaned.isBlank()) return
        if (!isConfigured) {
            onUnavailable(cleaned)
            return
        }

        stop()

        activeJob = scope.launch {
            try {
                val chunks = cleaned.chunkForTts(MAX_CHARS_PER_REQUEST)
                val chunkPlans = buildChunkPlans(chunks)

                val synthesized = try {
                    coroutineScope {
                        chunkPlans.map { plan ->
                            async(Dispatchers.IO) { synthesizeChunk(plan) }
                        }.awaitAll()
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    Log.w(TAG, "Deepgram synthesis failed; falling back to system TTS", error)
                    onUnavailable(cleaned)
                    return@launch
                }

                playSequentially(synthesized, chunkPlans, onWordIndex)
            } finally {
                onWordIndex(null)
                onComplete()
            }
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
        paused = false
        player?.let { existing ->
            runCatching { existing.stop() }
            runCatching { existing.reset() }
            runCatching { existing.release() }
        }
        player = null
    }

    fun shutdown() {
        stop()
        scope.cancel()
    }

    private suspend fun synthesizeChunk(plan: ChunkPlan): SynthesizedChunk = withContext(Dispatchers.IO) {
        val mp3File = ensureMp3(plan.text)
        val timingsFile = timingsCacheFileFor(plan.text)

        val cachedStarts = if (timingsFile.exists()) loadTimings(timingsFile, plan.sourceWords.size) else null
        if (cachedStarts != null) {
            Log.i(
                TAG,
                "Timings cache HIT (chunk=${plan.text.take(40).replace('\n', ' ')}\u2026 words=${plan.sourceWords.size})"
            )
            return@withContext SynthesizedChunk(mp3File, cachedStarts)
        }

        // Cache miss — try Deepgram STT for accurate per-word timing.
        Log.i(
            TAG,
            "Timings cache MISS, calling STT (chunk=${plan.text.take(40).replace('\n', ' ')}\u2026 words=${plan.sourceWords.size} mp3=${mp3File.length()}B)"
        )
        val realStarts = try {
            val sttWords = fetchSttWordTimings(mp3File)
            Log.i(TAG, "STT returned ${sttWords.size} words for ${plan.sourceWords.size} source words")
            val (aligned, matchedCount) = alignSttWithSourceVerbose(plan.sourceWords, sttWords)
            if (aligned == null) {
                Log.w(
                    TAG,
                    "Alignment fell below quality threshold (matched=$matchedCount/${plan.sourceWords.size}); using estimated timings"
                )
            } else {
                Log.i(
                    TAG,
                    "Alignment OK (matched=$matchedCount/${plan.sourceWords.size}, ratio=${(matchedCount * 100) / plan.sourceWords.size.coerceAtLeast(1)}%)"
                )
            }
            aligned
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Log.w(TAG, "Deepgram STT failed; will fall back to estimated word timings", error)
            null
        }

        if (realStarts != null) {
            runCatching { saveTimings(timingsFile, realStarts) }
        }
        SynthesizedChunk(mp3File, realStarts)
    }

    private suspend fun ensureMp3(chunk: String): File = withContext(Dispatchers.IO) {
        val cacheFile = mp3CacheFileFor(chunk)
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            return@withContext cacheFile
        }

        val tempFile = File(cacheDir, cacheFile.name + ".tmp")
        tempFile.delete()

        val connection = (URL(deepgramSpeakUrl()).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Authorization", "Token $apiKey")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "audio/mpeg")
        }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(JSONObject().put("text", chunk).toString())
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw DeepgramException("Deepgram speak returned $responseCode: $errorBody")
            }

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }

            if (!tempFile.renameTo(cacheFile)) {
                tempFile.copyTo(cacheFile, overwrite = true)
                tempFile.delete()
            }
            cacheFile
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun fetchSttWordTimings(mp3File: File): List<SttWord> = withContext(Dispatchers.IO) {
        val connection = (URL(deepgramListenUrl()).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Authorization", "Token $apiKey")
            setRequestProperty("Content-Type", "audio/mpeg")
            setFixedLengthStreamingMode(mp3File.length())
        }
        try {
            mp3File.inputStream().use { input ->
                connection.outputStream.use { output -> input.copyTo(output) }
            }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw DeepgramException("Deepgram listen returned $responseCode: $body")
            }
            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            parseSttWords(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun playSequentially(
        synthesized: List<SynthesizedChunk>,
        plans: List<ChunkPlan>,
        onWordIndex: (Int?) -> Unit
    ) {
        for ((index, chunk) in synthesized.withIndex()) {
            playFile(chunk, plans[index], onWordIndex)
        }
    }

    private suspend fun playFile(
        chunk: SynthesizedChunk,
        plan: ChunkPlan,
        onWordIndex: (Int?) -> Unit
    ) = withContext(Dispatchers.Main) {
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer

        try {
            mediaPlayer.setDataSource(chunk.file.absolutePath)
            mediaPlayer.prepare()
            val durationMs = mediaPlayer.duration.coerceAtLeast(1)
            val rawStarts = chunk.realStartsMs ?: plan.estimatedWordStartsForDuration(durationMs)
            val starts = applyAudioOffset(rawStarts, audioOffsetMs)
            Log.i(
                TAG,
                "Playing chunk (real=${chunk.realStartsMs != null} duration=${durationMs}ms words=${starts.size} offset=${audioOffsetMs}ms)"
            )
            val wordTimings = WordTimings(starts)

            val tracker = scope.launch {
                var lastEmitted: Int = Int.MIN_VALUE
                while (isActive) {
                    val pos = runCatching { mediaPlayer.currentPosition }.getOrDefault(0)
                    val relative = wordTimings.indexForPosition(pos)
                    val global = if (relative >= 0) plan.firstGlobalWordIndex + relative else -1
                    if (global != lastEmitted) {
                        lastEmitted = global
                        onWordIndex(if (global < 0) null else global)
                    }
                    delay(WORD_TICK_MS)
                }
            }

            try {
                kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
                    mediaPlayer.setOnCompletionListener {
                        if (continuation.isActive) continuation.resumeWith(Result.success(Unit))
                    }
                    mediaPlayer.setOnErrorListener { _, what, extra ->
                        if (continuation.isActive) {
                            continuation.resumeWith(
                                Result.failure(DeepgramException("MediaPlayer error: what=$what extra=$extra"))
                            )
                        }
                        true
                    }
                    continuation.invokeOnCancellation {
                        runCatching { mediaPlayer.stop() }
                        runCatching { mediaPlayer.reset() }
                        runCatching { mediaPlayer.release() }
                    }
                    mediaPlayer.start()
                    if (paused) {
                        runCatching { mediaPlayer.pause() }
                    }
                }
            } finally {
                tracker.cancel()
            }
        } finally {
            if (player === mediaPlayer) {
                runCatching { mediaPlayer.reset() }
                runCatching { mediaPlayer.release() }
                player = null
            }
        }
    }

    private fun mp3CacheFileFor(chunk: String): File =
        File(cacheDir, "${hashKey(chunk)}.mp3")

    private fun timingsCacheFileFor(chunk: String): File =
        File(cacheDir, "${hashKey(chunk)}.timings.json")

    private fun hashKey(chunk: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$voice|$chunk".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun loadTimings(file: File, expectedSize: Int): IntArray? {
        return try {
            val text = file.readText()
            val obj = JSONObject(text)
            if (obj.optInt("v", -1) != TIMINGS_FORMAT_VERSION) return null
            val arr = obj.optJSONArray("starts") ?: return null
            if (arr.length() != expectedSize) return null
            IntArray(arr.length()) { arr.getInt(it) }
        } catch (error: Throwable) {
            Log.w(TAG, "Failed to load cached timings; ignoring", error)
            null
        }
    }

    private fun saveTimings(file: File, starts: IntArray) {
        val arr = JSONArray()
        for (s in starts) arr.put(s)
        val obj = JSONObject()
            .put("v", TIMINGS_FORMAT_VERSION)
            .put("starts", arr)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(obj.toString())
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun deepgramSpeakUrl(): String =
        "https://api.deepgram.com/v1/speak?model=$voice&encoding=mp3"

    private fun deepgramListenUrl(): String =
        "https://api.deepgram.com/v1/listen?model=$STT_MODEL&punctuate=true&smart_format=false&filler_words=false"

    private class DeepgramException(message: String) : RuntimeException(message)

    companion object {
        private const val TAG = "DeepgramTts"
        private const val CACHE_DIR_NAME = "deepgram_tts"
        private const val MAX_CHARS_PER_REQUEST = 1800
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val WORD_TICK_MS = 40L
        private const val STT_MODEL = "nova-3"

        // Bumped when the alignment algorithm changes so older cached timings are recomputed.
        private const val TIMINGS_FORMAT_VERSION = 2
    }
}

internal data class SynthesizedChunk(
    val file: File,
    /** Per-source-word start timestamps in milliseconds. Null when only a duration-based estimate is available. */
    val realStartsMs: IntArray?
)

internal data class SttWord(
    val word: String,
    val startMs: Int,
    val endMs: Int
)

internal data class ChunkPlan(
    val text: String,
    /** Index in the global word list of the first word in this chunk. */
    val firstGlobalWordIndex: Int,
    /** Source words from the chunk text, in order. */
    val sourceWords: List<String>
) {
    /** Character length of each word (in order); used by [estimatedWordStartsForDuration]. */
    val wordCharLengths: IntArray = IntArray(sourceWords.size) { sourceWords[it].length }

    /** Fallback: distribute [durationMs] proportionally across words by character length. */
    fun estimatedWordStartsForDuration(durationMs: Int): IntArray {
        if (sourceWords.isEmpty()) return IntArray(0)
        val totalChars = wordCharLengths.sum().coerceAtLeast(1)
        val starts = IntArray(sourceWords.size)
        var accumulated = 0
        for (i in sourceWords.indices) {
            starts[i] = (accumulated.toLong() * durationMs / totalChars).toInt()
            accumulated += wordCharLengths[i]
        }
        return starts
    }
}

internal data class WordTimings(val startMs: IntArray) {
    /**
     * Returns the index of the word that should be highlighted at [positionMs],
     * or -1 if before the first word.
     */
    fun indexForPosition(positionMs: Int): Int {
        if (startMs.isEmpty()) return -1
        if (positionMs < startMs[0]) return -1
        var lo = 0
        var hi = startMs.size - 1
        while (lo < hi) {
            val mid = (lo + hi + 1) ushr 1
            if (startMs[mid] <= positionMs) lo = mid else hi = mid - 1
        }
        return lo
    }
}

internal fun buildChunkPlans(chunks: List<String>): List<ChunkPlan> {
    val plans = mutableListOf<ChunkPlan>()
    var globalIndex = 0
    for (chunk in chunks) {
        val words = WORD_REGEX.findAll(chunk).map { it.value }.toList()
        plans += ChunkPlan(
            text = chunk,
            firstGlobalWordIndex = globalIndex,
            sourceWords = words
        )
        globalIndex += words.size
    }
    return plans
}

internal fun String.chunkForTts(maxChars: Int): List<String> {
    val trimmed = trim()
    if (trimmed.length <= maxChars) return listOf(trimmed)

    val sentences = SENTENCE_SPLIT_REGEX.split(trimmed)
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    val chunks = mutableListOf<String>()
    val current = StringBuilder()

    fun flush() {
        if (current.isNotEmpty()) {
            chunks += current.toString().trim()
            current.clear()
        }
    }

    for (sentence in sentences) {
        if (sentence.length > maxChars) {
            flush()
            var index = 0
            while (index < sentence.length) {
                val end = minOf(index + maxChars, sentence.length)
                chunks += sentence.substring(index, end)
                index = end
            }
            continue
        }

        val needsSpace = current.isNotEmpty()
        val projected = current.length + (if (needsSpace) 1 else 0) + sentence.length
        if (projected > maxChars) {
            flush()
        }
        if (current.isNotEmpty()) current.append(' ')
        current.append(sentence)
    }
    flush()

    return chunks
}

/**
 * Aligns Deepgram STT word output with the source words from the chunk text and returns one
 * start-millisecond timestamp per source word. Returns null if alignment quality is too poor to
 * trust (so the caller can fall back to estimation).
 *
 * Strategy: greedy forward-walk pairing on normalized tokens (lowercased, alphanumeric-only).
 * Allows STT to merge or split a few words via a small lookahead window. Gaps are linearly
 * interpolated between matched neighbors.
 */
internal fun alignSttWithSource(
    sourceWords: List<String>,
    sttWords: List<SttWord>
): IntArray? = alignSttWithSourceVerbose(sourceWords, sttWords).first

/**
 * Same as [alignSttWithSource] but also returns the count of source words that were matched
 * directly to an STT word (vs. being interpolated from neighbors). Useful for diagnostics.
 */
internal fun alignSttWithSourceVerbose(
    sourceWords: List<String>,
    sttWords: List<SttWord>
): Pair<IntArray?, Int> {
    val n = sourceWords.size
    if (n == 0) return IntArray(0) to 0
    if (sttWords.isEmpty()) return null to 0

    val starts = IntArray(n) { -1 }
    var sttIdx = 0

    for (srcIdx in sourceWords.indices) {
        val srcNorm = normalizeForAlignment(sourceWords[srcIdx])
        if (srcNorm.isEmpty()) continue
        val windowEnd = minOf(sttIdx + ALIGNMENT_LOOKAHEAD, sttWords.size)
        var matched = -1
        for (j in sttIdx until windowEnd) {
            val sttNorm = normalizeForAlignment(sttWords[j].word)
            if (sttNorm.isEmpty()) continue
            val isMatch = sttNorm == srcNorm ||
                (sttNorm.length >= 3 && srcNorm.length >= 3 &&
                    (sttNorm.contains(srcNorm) || srcNorm.contains(sttNorm))) ||
                (sttNorm.length >= 4 && srcNorm.length >= 4 &&
                    sttNorm.commonPrefixWith(srcNorm).length >= minOf(sttNorm.length, srcNorm.length) - 1)
            if (isMatch) {
                matched = j
                break
            }
        }
        if (matched >= 0) {
            starts[srcIdx] = sttWords[matched].startMs
            sttIdx = matched + 1
        }
    }

    val matchedCount = starts.count { it >= 0 }
    // If fewer than half of the source words could be aligned, the STT output is likely off.
    if (matchedCount < (n + 1) / 2) return null to matchedCount

    fillAlignmentGaps(starts, sttWords)
    enforceMonotonic(starts)
    return starts to matchedCount
}

internal fun applyAudioOffset(starts: IntArray, offsetMs: Int): IntArray {
    if (offsetMs == 0) return starts
    val out = IntArray(starts.size)
    for (i in starts.indices) out[i] = (starts[i] + offsetMs).coerceAtLeast(0)
    return out
}

private fun normalizeForAlignment(word: String): String =
    word.lowercase().filter { it.isLetterOrDigit() }

private fun fillAlignmentGaps(starts: IntArray, sttWords: List<SttWord>) {
    val n = starts.size
    val totalAudioMs = sttWords.lastOrNull()?.endMs ?: 0
    var i = 0
    while (i < n) {
        if (starts[i] >= 0) {
            i++
            continue
        }
        var prev = i - 1
        while (prev >= 0 && starts[prev] < 0) prev--
        var next = i + 1
        while (next < n && starts[next] < 0) next++

        val prevTime = if (prev >= 0) starts[prev] else 0
        val nextTime = if (next < n) starts[next] else totalAudioMs.coerceAtLeast(prevTime)
        val gapEnd = if (next < n) next else n
        val gapStart = if (prev >= 0) prev else -1
        val span = (gapEnd - gapStart).coerceAtLeast(1)
        for (k in (gapStart + 1) until gapEnd) {
            val ratio = (k - gapStart).toDouble() / span
            starts[k] = (prevTime + ratio * (nextTime - prevTime)).toInt().coerceAtLeast(0)
        }
        i = gapEnd
    }
}

private fun enforceMonotonic(starts: IntArray) {
    for (i in 1 until starts.size) {
        if (starts[i] < starts[i - 1]) starts[i] = starts[i - 1]
    }
}

internal fun parseSttWords(jsonText: String): List<SttWord> {
    val root = JSONObject(jsonText)
    val results = root.optJSONObject("results") ?: return emptyList()
    val channels = results.optJSONArray("channels") ?: return emptyList()
    if (channels.length() == 0) return emptyList()
    val firstChannel = channels.optJSONObject(0) ?: return emptyList()
    val alternatives = firstChannel.optJSONArray("alternatives") ?: return emptyList()
    if (alternatives.length() == 0) return emptyList()
    val firstAlt = alternatives.optJSONObject(0) ?: return emptyList()
    val words = firstAlt.optJSONArray("words") ?: return emptyList()
    val list = mutableListOf<SttWord>()
    for (i in 0 until words.length()) {
        val w = words.optJSONObject(i) ?: continue
        val text = w.optString("punctuated_word", w.optString("word", ""))
        val start = w.optDouble("start", 0.0)
        val end = w.optDouble("end", start)
        list += SttWord(
            word = text,
            startMs = (start * 1000.0).toInt().coerceAtLeast(0),
            endMs = (end * 1000.0).toInt().coerceAtLeast(0)
        )
    }
    return list
}

// Wide enough to absorb cases where Deepgram STT spells numbers / abbreviations as multiple
// tokens (e.g. "2024" → "twenty twenty four") without losing alignment for the next source word.
private const val ALIGNMENT_LOOKAHEAD = 8

private val SENTENCE_SPLIT_REGEX = Regex("(?<=[.!?])\\s+")
private val WORD_REGEX = Regex("\\S+")
