package com.niranjan.englisharticle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.articleWordCount
import com.niranjan.englisharticle.ui.theme.ArticleReadingFontFamily

/**
 * The head of the article. It is set like the head of an article: the title in the same
 * serif as the prose below it, the facts underneath in small type, and a short rule to
 * close the block off from the body.
 *
 * Only real metadata appears. An earlier version invented an author, a date and a "News"
 * tag for every article, YouTube transcripts included.
 */
@Composable
fun ArticleHeader(article: CleanArticleResult) {
    val author = article.author.trim()
    val published = article.publishedDate.trim()
    val title = article.title.ifBlank { "Untitled article" }
    val wordCount = article.cleanArticle.articleWordCount()
    val readMinutes = (wordCount / 220).coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            // A long headline set at display size fills the screen before the article
            // has started, so it steps down a size once it runs past two lines' worth.
            text = title,
            style = if (title.length > 70) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.headlineLarge
            },
            color = MaterialTheme.colorScheme.onSurface
        )

        if (article.subtitle.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = article.subtitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = ArticleReadingFontFamily,
                    fontSize = 17.sp,
                    lineHeight = 27.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(14.dp))

        // Facts, in reading order and sentence case. No tracked-out capitals, no pill.
        Text(
            text = buildMeta(author, published, readMinutes, wordCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(20.dp))

        // A short rule, not a full-width divider: it marks the end of the head, it does
        // not cut the page in half.
        Box(
            modifier = Modifier
                .width(56.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.outline)
        )

        Spacer(Modifier.height(4.dp))
    }
}

private fun buildMeta(
    author: String,
    published: String,
    readMinutes: Int,
    wordCount: Int
): String {
    val parts = buildList {
        if (author.isNotEmpty()) add("By $author")
        if (published.isNotEmpty()) add(published)
        add("$readMinutes min read")
        add("$wordCount words")
    }
    return parts.joinToString("  ·  ")
}

