package com.niranjan.englisharticle.data

import com.niranjan.englisharticle.domain.NewsArticle
import com.niranjan.englisharticle.domain.NewsCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class GNewsApiService(private val apiKey: String) {

    suspend fun topHeadlines(
        category: NewsCategory = NewsCategory.General,
        language: String = "en",
        country: String = "us",
        max: Int = 20
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val url = buildString {
            append("$BASE_URL/top-headlines")
            append("?category=${category.apiValue}")
            append("&lang=$language")
            append("&country=$country")
            append("&max=$max")
        }
        fetchArticles(url)
    }

    suspend fun searchEverything(
        query: String,
        language: String = "en",
        sortBy: String = "publishedAt",
        max: Int = 20
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = buildString {
            append("$BASE_URL/search")
            append("?q=$encoded")
            append("&lang=$language")
            append("&sortby=$sortBy")
            append("&max=$max")
        }
        fetchArticles(url)
    }

    private fun fetchArticles(urlString: String): List<NewsArticle> {
        if (apiKey.isBlank()) {
            error(
                "GNews API key is missing. Add 'gnews.key=YOUR_KEY' to local.properties " +
                    "(get a free key at https://gnews.io/register)."
            )
        }
        // GNews accepts the key only via the apikey query param.
        val finalUrl = if (urlString.contains("?")) {
            "$urlString&apikey=$apiKey"
        } else {
            "$urlString?apikey=$apiKey"
        }
        val connection = URL(finalUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/json")

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() }
            } ?: ""
            if (code !in 200..299) {
                val errorJson = runCatching { JSONObject(body) }.getOrNull()
                val apiMessage = errorJson?.optStringOrNull("errors")
                    ?: errorJson?.optStringOrNull("message")
                val message = apiMessage ?: friendlyErrorFor(code)
                error("GNews error ($code): $message")
            }
            return parseArticles(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyErrorFor(code: Int): String = when (code) {
        400 -> "GNews rejected the request. Check the parameters."
        401, 403 -> "GNews API key is invalid or unauthorized. Verify the key at https://gnews.io/dashboard."
        429 -> "Daily request quota reached for your GNews plan. Try again tomorrow."
        500 -> "GNews server error. Try again shortly."
        else -> "HTTP $code"
    }

    private fun parseArticles(json: String): List<NewsArticle> {
        val root = JSONObject(json)
        val articles = root.optJSONArray("articles") ?: return emptyList()
        return (0 until articles.length()).mapNotNull { i ->
            val obj = articles.getJSONObject(i)
            val title = obj.optString("title", "").trim()
            if (title.isBlank()) return@mapNotNull null
            val source = obj.optJSONObject("source")
            NewsArticle(
                sourceId = null,
                sourceName = source?.optString("name", "")?.takeIf { it.isNotBlank() }.orEmpty(),
                author = null,
                title = title,
                description = obj.optStringOrNull("description"),
                url = obj.optString("url", ""),
                imageUrl = obj.optStringOrNull("image"),
                publishedAt = obj.optString("publishedAt", ""),
                content = obj.optStringOrNull("content")
            )
        }
    }

    companion object {
        private const val BASE_URL = "https://gnews.io/api/v4"
        private const val USER_AGENT = "EnglishArticleApp/1.0 (Android)"
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    optString(key, "").takeIf { it.isNotBlank() && it != "null" }
