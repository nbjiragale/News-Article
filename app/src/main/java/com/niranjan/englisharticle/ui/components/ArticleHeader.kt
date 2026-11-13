package com.niranjan.englisharticle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.articleWordCount

/**
 * Hero header rendered above an article body. Big display title, supporting
 * meta, and a circular author avatar — feels like a proper editorial layout.
 *
 * The top app bar lives in [AppTopBar]; this file deliberately does not declare
 * one. Two competing `AppTopBar` declarations previously shared this package,
 * which made every call site an ambiguous overload.
 */
@Composable
fun ArticleHeader(article: CleanArticleResult) {
    // Only real metadata is shown. The header used to invent an author (the app's name),
    // a date ("Ready to read") and a "News" tag for every article, YouTube videos included.
    val author = article.author.trim()
    val published = article.publishedDate.trim()
    val title = article.title.ifBlank { "Untitled article" }
    val wordCount = article.cleanArticle.articleWordCount()
    val readMinutes = (wordCount / 220).coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ArticlePill(
            text = "$readMinutes min read · $wordCount words",
            background = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            // displaySmall (36sp) turned long headlines into a full screen of title text.
            text = title,
            style = if (title.length > 70) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.headlineLarge
            },
            color = MaterialTheme.colorScheme.onSurface
        )
        if (article.subtitle.isNotBlank()) {
            Text(
                text = article.subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (author.isNotEmpty() || published.isNotEmpty()) Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (author.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = author.initials(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                if (author.isNotEmpty()) {
                    Text(
                        text = author,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (published.isNotEmpty()) {
                    Text(
                        text = published,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun ArticlePill(
    text: String,
    background: Color,
    content: Color
) {
    Surface(
        shape = CircleShape,
        color = background,
        contentColor = content
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun String.initials(): String {
    val parts = trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    return parts
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
}
