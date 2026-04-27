package com.niranjan.englisharticle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Article Viewer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Paste a news article and open a clean reading page.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (cleaningError != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = cleaningError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(
                            onClick = onRetry,
                            enabled = !isCleaning
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            OutlinedTextField(
                value = article,
                onValueChange = onArticleChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                label = { Text("Article text") },
                placeholder = { Text("Paste full article here") },
                shape = RoundedCornerShape(12.dp),
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
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
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
                            Text("Preparing")
                        }
                    } else {
                        Text("Open Article", style = MaterialTheme.typography.labelLarge)
                    }
                }
                OutlinedButton(
                    onClick = onClearArticle,
                    enabled = article.isNotBlank() && !isCleaning,
                    modifier = Modifier
                        .height(48.dp)
                        .padding(top = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
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
            onOpenPractice = {}
        )
    }
}
