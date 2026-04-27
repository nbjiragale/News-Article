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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
    onSpeakEnglish: (String) -> Unit,
    onSpeakKannada: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (summary == null && !isLoading) return

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_a),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onTertiary
                    )
                }
                Text(
                    text = "Article Context",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            if (isLoading && summary == null) {
                LoadingSummaryRows()
            } else if (summary != null) {
                SummarySection(
                    label = "WHAT HAPPENED",
                    english = summary.whatHappenedEnglish,
                    kannada = summary.whatHappenedKannada,
                    onSpeakEnglish = onSpeakEnglish,
                    onSpeakKannada = onSpeakKannada
                )
                if (summary.gistEnglish.isNotBlank() || summary.gistKannada.isNotBlank()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.18f)
                    )
                    SummarySection(
                        label = "WHAT THIS ARTICLE IS ABOUT",
                        english = summary.gistEnglish,
                        kannada = summary.gistKannada,
                        onSpeakEnglish = onSpeakEnglish,
                        onSpeakKannada = onSpeakKannada
                    )
                }
            }
        }
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
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
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
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                }
            )
        }
        IconButton(
            onClick = onSpeak,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_2),
                contentDescription = "Read aloud",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun LoadingSummaryRows() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "WHAT HAPPENED",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            cornerRadius = 6
        )
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(18.dp),
            cornerRadius = 6
        )
        Text(
            text = "Generating Kannada summary…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
    }
}
