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
 * Markdown / plain text suitable for downstream processing by the
 * existing OpenRouter article-cleaning pipeline.
 */
class JinaReaderService {

    suspend fun fetchReadableText(articleUrl: String): String = withContext(Dispatchers.IO) {
        val cleaned = articleUrl.trim()
        if (cleaned.isBlank()) error("Article URL is empty.")

        val readerUrl = "$READER_BASE_URL/$cleaned"
        val connection = URL(readerUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 20_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "text/plain")
        // Ask Jina Reader to return plain text without the embedded
        // image placeholders / link annotations so the LLM gets clean
        // prose to work with.
        connection.setRequestProperty("X-Return-Format", "text")

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() }
            } ?: ""
            if (code !in 200..299) {
                error("Jina Reader error ($code): ${body.take(200).ifBlank { "no body" }}")
            }
            return@withContext body
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val READER_BASE_URL = "https://r.jina.ai"
        private const val USER_AGENT = "EnglishArticleApp/1.0 (Android)"
    }
}
