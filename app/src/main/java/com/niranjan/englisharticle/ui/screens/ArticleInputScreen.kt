package com.niranjan.englisharticle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.R
import com.niranjan.englisharticle.ui.components.AppTopBar
import com.niranjan.englisharticle.ui.theme.AppSurfaceContainerLowest
import com.niranjan.englisharticle.ui.theme.EnglishArticleTheme

@Composable
fun ArticleInputScreen(
    article: String,
    onArticleChange: (String) -> Unit,
    onLoadArticle: () -> Unit,
    isCleaning: Boolean,
    cleaningError: String?,
    onRetry: () -> Unit,
    onClearArticle: () -> Unit,
    onOpenRecents: () -> Unit,
    onOpenSavedWords: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenNews: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AppTopBar(
            showBack = false,
            onRecentsClick = onOpenRecents,
            onSavedWordsClick = onOpenSavedWords,
            onPracticeClick = onOpenPractice
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeroIntro()

            QuickActionRow(
                onOpenRecents = onOpenRecents,
                onOpenSavedWords = onOpenSavedWords,
                onOpenPractice = onOpenPractice,
                onOpenNews = onOpenNews
            )

            if (cleaningError != null) {
                ErrorCard(message = cleaningError, onRetry = onRetry, isRetrying = isCleaning)
            }

            OutlinedTextField(
                value = article,
                onValueChange = onArticleChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = { Text("Article text") },
                placeholder = { Text("Paste a full article — headlines, body, the works.") },
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = AppSurfaceContainerLowest,
                    unfocusedContainerColor = AppSurfaceContainerLowest,
                    errorContainerColor = AppSurfaceContainerLowest
                )
            )

            ActionRow(
                onLoadArticle = onLoadArticle,
                onClearArticle = onClearArticle,
                article = article,
                isCleaning = isCleaning
            )
        }
    }
}

@Composable
private fun HeroIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                text = "READ · LOOKUP · LEARN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
        Text(
            text = "Read English news,\nlearn in Kannada.",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Paste a full article and tap any word for an instant contextual meaning, definition, and example — saved to your practice deck.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionRow(
    onOpenRecents: () -> Unit,
    onOpenSavedWords: () -> Unit,
    onOpenPractice: () -> Unit,
    onOpenNews: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionTile(
            iconRes = R.drawable.ic_globe,
            label = "News",
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onOpenNews,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            iconRes = R.drawable.ic_history,
            label = "Recent",
            container = MaterialTheme.colorScheme.secondaryContainer,
            content = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onOpenRecents,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            iconRes = R.drawable.ic_bookmark,
            label = "Saved",
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onOpenSavedWords,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            iconRes = R.drawable.ic_school,
            label = "Practice",
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = MaterialTheme.colorScheme.onSurface,
            onClick = onOpenPractice,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionTile(
    iconRes: Int,
    label: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = container,
        contentColor = content,
        modifier = modifier.height(86.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(content.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit, isRetrying: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(
                onClick = onRetry,
                enabled = !isRetrying
            ) {
                Text("Retry", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ActionRow(
    onLoadArticle: () -> Unit,
    onClearArticle: () -> Unit,
    article: String,
    isCleaning: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onLoadArticle,
            enabled = article.isNotBlank() && !isCleaning,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (isCleaning) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Text("Preparing", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Text("Open Article", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Surface(
            onClick = onClearArticle,
            enabled = article.isNotBlank() && !isCleaning,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_circle_x),
                    contentDescription = "Clear",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    Spacer(Modifier.size(0.dp))
}

@Preview(showBackground = true)
@Composable
private fun ArticleInputPreview() {
    EnglishArticleTheme {
        ArticleInputScreen(
            article = "",
            onArticleChange = {},
            onLoadArticle = {},
            isCleaning = false,
            cleaningError = null,
            onRetry = {},
            onClearArticle = {},
            onOpenRecents = {},
            onOpenSavedWords = {},
            onOpenPractice = {},
            onOpenNews = {}
        )
    }
}
