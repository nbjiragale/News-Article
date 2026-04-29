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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    youTubeUrl: String = "",
    onYouTubeUrlChange: (String) -> Unit = {},
    onImportYouTube: () -> Unit = {},
    isImportingYouTube: Boolean = false,
    youTubeError: String? = null,
    onDismissYouTubeError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AppTopBar(showBack = false)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeroIntro()

            RecentsLink(onOpenRecents = onOpenRecents)

            YouTubeImportCard(
                url = youTubeUrl,
                onUrlChange = onYouTubeUrlChange,
                onImport = onImportYouTube,
                isImporting = isImportingYouTube,
                error = youTubeError,
                onDismissError = onDismissYouTubeError
            )

            if (cleaningError != null) {
                ErrorCard(message = cleaningError, onRetry = onRetry, isRetrying = isCleaning)
            }

            OutlinedTextField(
                value = article,
                onValueChange = onArticleChange,
                modifier = Modifier
                    .heightIn(min = 220.dp)
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
private fun YouTubeImportCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onImport: () -> Unit,
    isImporting: Boolean,
    error: String?,
    onDismissError: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Read a YouTube video",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Paste a YouTube link to fetch its captions and read the video as an article.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("YouTube link") },
                placeholder = { Text("https://www.youtube.com/watch?v=…") },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedContainerColor = AppSurfaceContainerLowest,
                    unfocusedContainerColor = AppSurfaceContainerLowest
                )
            )
            if (error != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onDismissError) {
                            Text("Dismiss", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Button(
                onClick = onImport,
                enabled = url.isNotBlank() && !isImporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isImporting) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text(
                            "Fetching transcript",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    Text(
                        "Fetch transcript",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Read English,\nlearn in Kannada.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Paste an article or a YouTube link. Tap any word for meaning, definition, and an example you can save.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentsLink(onOpenRecents: () -> Unit) {
    Surface(
        onClick = onOpenRecents,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_history),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Recent articles",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Reopen anything you've read.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            onOpenNews = {},
            youTubeUrl = "",
            onYouTubeUrlChange = {},
            onImportYouTube = {},
            isImportingYouTube = false,
            youTubeError = null,
            onDismissYouTubeError = {}
        )
    }
}
