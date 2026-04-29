package com.niranjan.englisharticle.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.R
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.articleWordCount

/**
 * Top app bar used across screens.
 *
 * Uses tonal surface from the theme so it adapts to light/dark cleanly. The
 * brand mark + product name are pinned start; navigation actions are pinned
 * end. Height matches M3 large component spec.
 */
@Composable
fun AppTopBar(
    showBack: Boolean,
    onBack: () -> Unit = {},
    onRecentsClick: (() -> Unit)? = null,
    onSavedWordsClick: (() -> Unit)? = null,
    onPracticeClick: (() -> Unit)? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(40.dp), contentAlignment = Alignment.CenterStart) {
                if (showBack) {
                    RoundIconButton(
                        iconRes = R.drawable.ic_move_left,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                }
            }
            if (!showBack) {
                Image(
                    painter = painterResource(R.drawable.arthareader_logo),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (showBack) 4.dp else 10.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onSavedWordsClick != null) {
                    RoundIconButton(
                        iconRes = R.drawable.ic_bookmark,
                        contentDescription = "Saved words",
                        onClick = onSavedWordsClick
                    )
                }
                if (onPracticeClick != null) {
                    RoundIconButton(
                        iconRes = R.drawable.ic_school,
                        contentDescription = "Practice",
                        onClick = onPracticeClick
                    )
                }
                if (onRecentsClick != null) {
                    RoundIconButton(
                        iconRes = R.drawable.ic_history,
                        contentDescription = "Recent articles",
                        onClick = onRecentsClick
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit = {}
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Hero header rendered above an article body. Big display title, supporting
 * meta, and a circular author avatar — feels like a proper editorial layout.
 */
@Composable
fun ArticleHeader(article: CleanArticleResult) {
    val author = article.author.ifBlank { stringResource(R.string.app_name) }
    val published = article.publishedDate.ifBlank { "Ready to read" }
    val title = article.title.ifBlank { "Clean Reading Article" }
    val readMinutes = (article.cleanArticle.articleWordCount() / 220).coerceAtLeast(1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ArticlePill(
                text = "News",
                background = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            )
            ArticlePill(
                text = "$readMinutes min read",
                background = MaterialTheme.colorScheme.surfaceContainer,
                content = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (article.subtitle.isNotBlank()) {
            Text(
                text = article.subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = author.initials(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = published,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@Composable
fun BottomSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

fun Modifier.interactiveWordUnderline(enabled: Boolean): Modifier {
    if (!enabled) return this
    return this
}

private fun String.initials(): String {
    val parts = trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    return parts
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "LN" }
}
