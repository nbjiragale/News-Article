package com.niranjan.englisharticle.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Extracts the readable body of any web page using Jina Reader
 * (https://r.jina.ai). The endpoint is free and does not require
 * authentication for basic use; calls return the article body as
 * plain text suitable for downstream processing by the existing
 * OpenRouter article-cleaning pipeline.
 *
 * Some publishers reject generic HTTP scrapers with 403. When that
 * happens we automatically retry with Jina's headless-browser engine
 * (`X-Engine: browser`), which fetches the page through a real
 * Chromium instance and gets past most anti-scraping checks.
 */
class JinaReaderService {

    sealed class Result {
        data class Success(val text: String) : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun fetchReadableText(articleUrl: String): Result = withContext(Dispatchers.IO) {
        val cleaned = articleUrl.trim()
        if (cleaned.isBlank()) return@withContext Result.Failure("Article URL is empty.")

        val direct = runCatching { request(cleaned, useBrowserEngine = false) }.getOrElse { error ->
            return@withContext Result.Failure(error.message ?: "Jina Reader request failed.")
        }
        if (direct is Result.Success || !shouldRetryWithBrowser(direct)) {
            return@withContext direct
        }

        runCatching { request(cleaned, useBrowserEngine = true) }.getOrElse { error ->
            Result.Failure(error.message ?: direct.failureMessage())
        }
    }

    private fun request(articleUrl: String, useBrowserEngine: Boolean): Result {
        val readerUrl = "$READER_BASE_URL/$articleUrl"
        val connection = URL(readerUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 20_000
        // Browser-engine fetches load the full page; allow more time.
        connection.readTimeout = if (useBrowserEngine) 60_000 else 30_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "text/plain")
        connection.setRequestProperty("X-Return-Format", "text")
        if (useBrowserEngine) {
            connection.setRequestProperty("X-Engine", "browser")
        }

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() }
            } ?: ""
            return if (code in 200..299 && body.isNotBlank()) {
                Result.Success(body)
            } else {
                val snippet = body.take(240).ifBlank { "no body" }
                Result.Failure("Jina Reader error ($code): $snippet")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun shouldRetryWithBrowser(result: Result): Boolean {
        if (result !is Result.Failure) return false
        val message = result.message.lowercase()
        return "403" in message ||
            "forbidden" in message ||
            "target url" in message ||
            "timeout" in message ||
            "blocked" in message
    }

    private fun Result.failureMessage(): String = when (this) {
        is Result.Failure -> message
        is Result.Success -> "Jina Reader returned empty content."
    }

    companion object {
        private const val READER_BASE_URL = "https://r.jina.ai"
        private const val USER_AGENT = "EnglishArticleApp/1.0 (Android)"
    }
}
