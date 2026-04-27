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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class DeepgramTextToSpeech(
    context: Context,
    private val apiKey: String,
    private val voice: String
) {
    private val appContext = context.applicationContext
    private val cacheDir: File = File(appContext.cacheDir, CACHE_DIR_NAME).apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var activeJob: Job? = null
    private var player: MediaPlayer? = null

    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    fun speak(
        text: String,
        onUnavailable: (String) -> Unit,
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
                val files = try {
                    coroutineScope {
                        chunks.map { chunk -> async(Dispatchers.IO) { synthesizeChunk(chunk) } }.awaitAll()
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Throwable) {
                    Log.w(TAG, "Deepgram synthesis failed; falling back to system TTS", error)
                    onUnavailable(cleaned)
                    return@launch
                }

                playSequentially(files)
            } finally {
                onComplete()
            }
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
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

    private suspend fun synthesizeChunk(chunk: String): File = withContext(Dispatchers.IO) {
        val cacheFile = cacheFileFor(chunk)
        if (cacheFile.exists() && cacheFile.length() > 0L) {
            return@withContext cacheFile
        }

        val tempFile = File(cacheDir, cacheFile.name + ".tmp")
        tempFile.delete()

        val connection = (URL(deepgramUrl()).openConnection() as HttpURLConnection).apply {
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
                throw DeepgramException("Deepgram returned $responseCode: $errorBody")
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

    private suspend fun playSequentially(files: List<File>) {
        for (file in files) {
            playFile(file)
        }
    }

    private suspend fun playFile(file: File) = withContext(Dispatchers.Main) {
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer

        try {
            mediaPlayer.setDataSource(file.absolutePath)
            mediaPlayer.prepare()

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
            }
        } finally {
            if (player === mediaPlayer) {
                runCatching { mediaPlayer.reset() }
                runCatching { mediaPlayer.release() }
                player = null
            }
        }
    }

    private fun cacheFileFor(chunk: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest("$voice|$chunk".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(cacheDir, "$hash.mp3")
    }

    private fun deepgramUrl(): String =
        "https://api.deepgram.com/v1/speak?model=$voice&encoding=mp3"

    private class DeepgramException(message: String) : RuntimeException(message)

    companion object {
        private const val TAG = "DeepgramTts"
        private const val CACHE_DIR_NAME = "deepgram_tts"
        private const val MAX_CHARS_PER_REQUEST = 1800
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
    }
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

private val SENTENCE_SPLIT_REGEX = Regex("(?<=[.!?])\\s+")
