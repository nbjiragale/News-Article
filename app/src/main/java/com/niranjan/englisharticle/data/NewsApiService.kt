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

class NewsApiService(private val apiKey: String) {

    suspend fun topHeadlines(
        category: NewsCategory = NewsCategory.General,
        country: String = "us",
        page: Int = 1,
        pageSize: Int = 20
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val url = buildString {
            append("$BASE_URL/top-headlines")
            append("?country=$country")
            append("&category=${category.apiValue}")
            append("&page=$page")
            append("&pageSize=$pageSize")
        }
        fetchArticles(url)
    }

    suspend fun searchEverything(
        query: String,
        language: String = "en",
        sortBy: String = "publishedAt",
        page: Int = 1,
        pageSize: Int = 20
    ): List<NewsArticle> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = buildString {
            append("$BASE_URL/everything")
            append("?q=$encoded")
            append("&language=$language")
            append("&sortBy=$sortBy")
            append("&page=$page")
            append("&pageSize=$pageSize")
        }
        fetchArticles(url)
    }

    private fun fetchArticles(urlString: String): List<NewsArticle> {
        if (apiKey.isBlank()) {
            error(
                "News API key is missing. Add 'newsapi.key=YOUR_KEY' to local.properties " +
                    "(get a free key at https://newsapi.org/register)."
            )
        }
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        // NewsAPI is fronted by Cloudflare which 403s requests with the default
        // Java/Android User-Agent. Set an explicit UA and pass the API key via
        // the recommended X-Api-Key header instead of a query parameter.
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("X-Api-Key", apiKey)
        connection.setRequestProperty("Accept", "application/json")

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = stream?.let {
                BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() }
            } ?: ""
            if (code !in 200..299) {
                val errorJson = runCatching { JSONObject(body) }.getOrNull()
                val apiMessage = errorJson?.optString("message")?.takeIf { it.isNotBlank() }
                val message = apiMessage ?: friendlyErrorFor(code)
                error("News API error ($code): $message")
            }
            return parseArticles(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun friendlyErrorFor(code: Int): String = when (code) {
        401 -> "API key is invalid or missing. Check newsapi.key in local.properties."
        403 -> "Request was blocked (HTTP 403). This usually means the API key is " +
            "invalid, disabled, or the free Developer plan is being used outside of " +
            "a development environment. Verify your key at https://newsapi.org/account."
        426 -> "NewsAPI free Developer plan is restricted to local development. " +
            "Upgrade your plan or run a debug build."
        429 -> "Rate limit reached for your NewsAPI plan. Try again later."
        else -> "HTTP $code"
    }

    private fun parseArticles(json: String): List<NewsArticle> {
        val root = JSONObject(json)
        val articles = root.optJSONArray("articles") ?: return emptyList()
        return (0 until articles.length()).mapNotNull { i ->
            val obj = articles.getJSONObject(i)
            val title = obj.optString("title", "").trim()
            if (title.isBlank() || title == "[Removed]") return@mapNotNull null
            val source = obj.optJSONObject("source")
            NewsArticle(
                sourceId = source?.optString("id")?.takeIf { it != "null" },
                sourceName = source?.optString("name", "") ?: "",
                author = obj.optString("author").takeIf { it != "null" && it.isNotBlank() },
                title = title,
                description = obj.optString("description").takeIf { it != "null" && it.isNotBlank() },
                url = obj.optString("url", ""),
                imageUrl = obj.optString("urlToImage").takeIf { it != "null" && it.isNotBlank() },
                publishedAt = obj.optString("publishedAt", ""),
                content = obj.optString("content").takeIf { it != "null" && it.isNotBlank() }
            )
        }
    }

    companion object {
        private const val BASE_URL = "https://newsapi.org/v2"
        private const val USER_AGENT = "EnglishArticleApp/1.0 (Android)"
    }
}
