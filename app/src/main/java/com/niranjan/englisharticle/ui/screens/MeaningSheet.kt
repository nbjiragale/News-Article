package com.niranjan.englisharticle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.R
import com.niranjan.englisharticle.BuildConfig
import com.niranjan.englisharticle.domain.MeaningResult
import com.niranjan.englisharticle.ui.components.HighlightedSentence
import com.niranjan.englisharticle.ui.state.MeaningUiState
import com.niranjan.englisharticle.ui.theme.AppPrimaryContainer
import com.niranjan.englisharticle.ui.theme.AppSurfaceContainerLow

@Composable
fun MeaningSheet(
    word: String,
    sentence: String,
    showSentence: Boolean,
    state: MeaningUiState,
    isSaved: Boolean,
    onSpeakEnglish: (String) -> Unit,
    onSpeakKannada: (String) -> Unit,
    onSaveWord: (MeaningResult) -> Unit,
    onPractice: (MeaningResult) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        when (state) {
            MeaningUiState.Idle,
            MeaningUiState.Loading -> {
                MeaningSheetHeader(
                    word = word,
                    partOfSpeech = "",
                    onSpeak = { onSpeakEnglish(word) }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            is MeaningUiState.Error -> {
                MeaningSheetHeader(
                    word = word,
                    partOfSpeech = "",
                    onSpeak = { onSpeakEnglish(word) }
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                if (BuildConfig.OPENROUTER_API_KEY.isBlank()) {
                    Text(
                        text = "Add openrouter.api.key=YOUR_API_KEY to local.properties and rebuild the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is MeaningUiState.Success -> {
                MeaningSheetHeader(
                    word = word,
                    partOfSpeech = state.result.partOfSpeech.ifBlank { "Word" },
                    onSpeak = { onSpeakEnglish(word) }
                )
                MeaningSummary(
                    result = state.result,
                    showSentence = showSentence,
                    onSpeakEnglish = onSpeakEnglish,
                    onSpeakKannada = onSpeakKannada
                )
                MeaningContextCard(
                    word = word,
                    sentence = sentence,
                    showSentence = showSentence,
                    result = state.result,
                    onSpeakEnglish = onSpeakEnglish,
                    onSpeakKannada = onSpeakKannada
                )
                MeaningActions(
                    isSaved = isSaved,
                    onSaveWord = { onSaveWord(state.result) },
                    onPractice = { onPractice(state.result) }
                )
            }
        }
    }
}

@Composable
private fun MeaningSheetHeader(
    word: String,
    partOfSpeech: String,
    onSpeak: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (partOfSpeech.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = partOfSpeech.uppercase(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        IconButton(
            onClick = onSpeak,
            enabled = word.isNotBlank(),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_volume_2),
                contentDescription = "Pronounce word",
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun MeaningSummary(
    result: MeaningResult,
    showSentence: Boolean,
    onSpeakEnglish: (String) -> Unit,
    onSpeakKannada: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MeaningInfoCard(
            label = if (showSentence) "Tapped Word Meaning" else "Kannada Meaning",
            value = result.meaningKannada,
            emphasize = true,
            onSpeak = { onSpeakKannada(result.meaningKannada) }
        )
        MeaningInfoCard(
            label = "Definition",
            value = result.simpleEnglish.ifBlank { result.explanationKannada },
            onSpeak = {
                val text = result.simpleEnglish.ifBlank { result.explanationKannada }
                if (result.simpleEnglish.isNotBlank()) onSpeakEnglish(text) else onSpeakKannada(text)
            }
        )
    }
}

@Composable
private fun MeaningInfoCard(
    label: String,
    value: String,
    emphasize: Boolean = false,
    onSpeak: (() -> Unit)? = null
) {
    if (value.isBlank()) return
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppSurfaceContainerLow,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 92.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.uppercase(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                if (onSpeak != null) {
                    SmallSpeakButton(
                        contentDescription = "Read $label",
                        onClick = onSpeak
                    )
                }
            }
            SelectionContainer {
                Text(
                    text = value,
                    style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                    color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun MeaningContextCard(
    word: String,
    sentence: String,
    showSentence: Boolean,
    result: MeaningResult,
    onSpeakEnglish: (String) -> Unit,
    onSpeakKannada: (String) -> Unit
) {
    val englishContext = if (showSentence && sentence.isNotBlank()) sentence else result.exampleEnglish
    val kannadaContext = result.exampleKannada.ifBlank { result.explanationKannada }
    val sectionTitle = if (showSentence) "SENTENCE TRANSLATION" else "EXAMPLE IN CONTEXT"

    if (englishContext.isBlank() && kannadaContext.isBlank()) return

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AppPrimaryContainer.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, AppPrimaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_a),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = sectionTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (englishContext.isNotBlank()) {
                    SmallSpeakButton(
                        contentDescription = "Read English context",
                        onClick = { onSpeakEnglish(englishContext) }
                    )
                }
            }
            if (englishContext.isNotBlank()) {
                SelectionContainer {
                    HighlightedSentence(
                        text = englishContext,
                        word = word,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (kannadaContext.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        if (showSentence) {
                            Text(
                                text = kannadaContext,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            HighlightedSentence(
                                text = kannadaContext,
                                word = result.meaningKannada.substringBefore(" ").ifBlank { word },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    SmallSpeakButton(
                        contentDescription = "Read Kannada context",
                        onClick = { onSpeakKannada(kannadaContext) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallSpeakButton(
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_volume_2),
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MeaningActions(
    isSaved: Boolean,
    onSaveWord: () -> Unit,
    onPractice: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSaveWord,
            enabled = !isSaved,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bookmark_plus),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isSaved) "Saved" else "Save Word",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Button(
            onClick = onPractice,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_school),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text("Practice", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
