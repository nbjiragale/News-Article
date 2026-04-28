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
            append("&apiKey=$apiKey")
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
            append("&apiKey=$apiKey")
        }
        fetchArticles(url)
    }

    private fun fetchArticles(urlString: String): List<NewsArticle> {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000

        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val body = BufferedReader(InputStreamReader(stream)).use { it.readText() }
            if (code !in 200..299) {
                val errorJson = runCatching { JSONObject(body) }.getOrNull()
                val message = errorJson?.optString("message") ?: "HTTP $code"
                error("News API error: $message")
            }
            return parseArticles(body)
        } finally {
            connection.disconnect()
        }
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
    }
}
