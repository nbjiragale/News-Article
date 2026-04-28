package com.niranjan.englisharticle.domain

data class NewsArticle(
    val sourceId: String?,
    val sourceName: String,
    val author: String?,
    val title: String,
    val description: String?,
    val url: String,
    val imageUrl: String?,
    val publishedAt: String,
    val content: String?
)

enum class NewsCategory(val apiValue: String, val label: String) {
    General("general", "General"),
    Business("business", "Business"),
    Technology("technology", "Technology"),
    Science("science", "Science"),
    Health("health", "Health"),
    Sports("sports", "Sports"),
    Entertainment("entertainment", "Entertainment")
}
