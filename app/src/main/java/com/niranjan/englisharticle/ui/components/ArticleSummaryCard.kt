package com.niranjan.englisharticle.ui.components

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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.R
import com.niranjan.englisharticle.domain.ArticleSummary

@Composable
fun ArticleSummaryCard(
    summary: ArticleSummary?,
    isLoading: Boolean,
    errorMessage: String?,
    onRequestContext: () -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onSpeakKannada: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasSummary = summary != null && summary.isNotBlank()

    if (!hasSummary && !isLoading) {
        CompactOffer(
            errorMessage = errorMessage,
            onRequestContext = onRequestContext,
            modifier = modifier
        )
        return
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SummaryHeader()

            if (hasSummary) {
                SummaryContent(
                    summary = summary!!,
                    onSpeakEnglish = onSpeakEnglish,
                    onSpeakKannada = onSpeakKannada
                )
            } else {
                LoadingPlaceholder()
            }
        }
    }
}

/**
 * The offer, before it has been taken up: one row the width of the text, so the article
 * below it stays the first thing on the page.
 */
@Composable
private fun CompactOffer(
    errorMessage: String?,
    onRequestContext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = onRequestContext,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_a),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (errorMessage.isNullOrBlank()) {
                        "Summarise this in English and Kannada"
                    } else {
                        "Try the summary again"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun SummaryHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_book_a),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Article context",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Quick understanding in English and Kannada",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryContent(
    summary: ArticleSummary,
    onSpeakEnglish: (String) -> Unit,
    onSpeakKannada: (String) -> Unit
) {
    SummarySection(
        label = "What happened",
        english = summary.whatHappenedEnglish,
        kannada = summary.whatHappenedKannada,
        onSpeakEnglish = onSpeakEnglish,
        onSpeakKannada = onSpeakKannada
    )
    if (summary.gistEnglish.isNotBlank() || summary.gistKannada.isNotBlank()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
        SummarySection(
            label = "What this is about",
            english = summary.gistEnglish,
            kannada = summary.gistKannada,
            onSpeakEnglish = onSpeakEnglish,
            onSpeakKannada = onSpeakKannada
        )
    }
}

@Composable
private fun SummarySection(
    label: String,
    english: String,
    kannada: String,
    onSpeakEnglish: (String) -> Unit,
    onSpeakKannada: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        if (english.isNotBlank()) {
            SummaryLine(
                text = english,
                emphasized = true,
                onSpeak = { onSpeakEnglish(english) }
            )
        }
        if (kannada.isNotBlank()) {
            SummaryLine(
                text = kannada,
                emphasized = false,
                onSpeak = { onSpeakKannada(kannada) }
            )
        }
    }
}

@Composable
private fun SummaryLine(
    text: String,
    emphasized: Boolean,
    onSpeak: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectionContainer(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = if (emphasized) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = if (emphasized) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        // 40dp: 32dp was below a comfortable touch size for a button this close to text.
        IconButton(
            onClick = onSpeak,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_2),
                contentDescription = "Read aloud",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Generating summary…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            cornerRadius = 6
        )
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(16.dp),
            cornerRadius = 6
        )
        Box(modifier = Modifier.height(4.dp).width(1.dp))
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(16.dp),
            cornerRadius = 6
        )
    }
}
