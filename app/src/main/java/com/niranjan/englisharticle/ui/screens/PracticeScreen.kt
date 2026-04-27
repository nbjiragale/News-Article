package com.niranjan.englisharticle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.R
import com.niranjan.englisharticle.domain.SavedWord
import androidx.compose.material3.LinearProgressIndicator
import com.niranjan.englisharticle.ui.components.AppTopBar
import com.niranjan.englisharticle.ui.components.EmptyState
import com.niranjan.englisharticle.ui.components.Pill
import kotlin.random.Random

@Composable
fun PracticeScreen(
    savedWords: List<SavedWord>,
    onBack: () -> Unit,
    onOpenSavedWords: () -> Unit,
    onPracticeAnswer: (SavedWord, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var deck by remember { mutableStateOf<List<SavedWord>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var selectedCorrect by remember { mutableStateOf<Boolean?>(null) }
    var sessionCorrect by remember { mutableIntStateOf(0) }
    var sessionAnswered by remember { mutableIntStateOf(0) }
    val savedKeys = remember(savedWords) { savedWords.joinToString("|") { it.savedKey } }

    fun restartPractice() {
        deck = savedWords.shuffled()
        currentIndex = 0
        selectedKey = null
        selectedCorrect = null
        sessionCorrect = 0
        sessionAnswered = 0
    }

    LaunchedEffect(savedKeys) {
        restartPractice()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AppTopBar(
            showBack = true,
            onBack = onBack,
            onSavedWordsClick = onOpenSavedWords
        )

        if (savedWords.isEmpty()) {
            EmptyPracticeState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
            return@Column
        }

        val current = deck.getOrNull(currentIndex)
        if (current == null) {
            PracticeCompleteState(
                correct = sessionCorrect,
                answered = sessionAnswered,
                onRestart = ::restartPractice,
                onOpenSavedWords = onOpenSavedWords,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
            return@Column
        }

        val choices = remember(current.savedKey, savedKeys) {
            buildPracticeChoices(current, savedWords)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 96.dp)),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PracticeHeader(
                currentIndex = currentIndex,
                totalCount = deck.size,
                sessionCorrect = sessionCorrect,
                sessionAnswered = sessionAnswered
            )
            PromptCard(savedWord = current)

            if (choices.size <= 1) {
                SingleCardPractice(
                    savedWord = current,
                    hasAnswered = selectedCorrect != null,
                    onAnswer = { isCorrect ->
                        if (selectedCorrect == null) {
                            selectedCorrect = isCorrect
                            sessionAnswered += 1
                            if (isCorrect) sessionCorrect += 1
                            onPracticeAnswer(current, isCorrect)
                        }
                    }
                )
            } else {
                MultipleChoicePractice(
                    current = current,
                    choices = choices,
                    selectedKey = selectedKey,
                    selectedCorrect = selectedCorrect,
                    onSelect = { choice ->
                        if (selectedCorrect == null) {
                            val isCorrect = choice.savedKey == current.savedKey
                            selectedKey = choice.savedKey
                            selectedCorrect = isCorrect
                            sessionAnswered += 1
                            if (isCorrect) sessionCorrect += 1
                            onPracticeAnswer(current, isCorrect)
                        }
                    }
                )
            }

            if (selectedCorrect != null) {
                FeedbackCard(
                    isCorrect = selectedCorrect == true,
                    correctAnswer = current.meaning.meaningKannada
                )
                Button(
                    onClick = {
                        currentIndex += 1
                        selectedKey = null
                        selectedCorrect = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (currentIndex == deck.lastIndex) "Finish" else "Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PracticeHeader(
    currentIndex: Int,
    totalCount: Int,
    sessionCorrect: Int,
    sessionAnswered: Int
) {
    val progress = if (totalCount == 0) 0f else (currentIndex + 1).toFloat() / totalCount
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill(
                text = "Card ${currentIndex + 1} of $totalCount",
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Pill(
                text = "$sessionCorrect / $sessionAnswered correct",
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainer,
            drawStopIndicator = {}
        )
    }
}

@Composable
private fun PromptCard(savedWord: SavedWord) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "CHOOSE THE KANNADA MEANING",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = savedWord.word,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (savedWord.sentence.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f))
                Text(
                    text = "“${savedWord.sentence}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun MultipleChoicePractice(
    current: SavedWord,
    choices: List<SavedWord>,
    selectedKey: String?,
    selectedCorrect: Boolean?,
    onSelect: (SavedWord) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        choices.forEach { choice ->
            val isSelected = selectedKey == choice.savedKey
            val isCorrectChoice = selectedCorrect != null && choice.savedKey == current.savedKey
            val containerColor = when {
                isCorrectChoice -> androidx.compose.ui.graphics.Color(0xFFD3F1E2)
                isSelected -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            }
            val outline = when {
                isCorrectChoice -> androidx.compose.ui.graphics.Color(0xFF1F9D6F)
                isSelected -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            Surface(
                onClick = {
                    if (selectedCorrect == null) onSelect(choice)
                },
                enabled = selectedCorrect == null,
                shape = RoundedCornerShape(20.dp),
                color = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
            ) {
                Text(
                    text = choice.meaning.meaningKannada,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SingleCardPractice(
    savedWord: SavedWord,
    hasAnswered: Boolean,
    onAnswer: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = savedWord.meaning.meaningKannada,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = savedWord.meaning.simpleEnglish,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onAnswer(false) },
                    enabled = !hasAnswered,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Still learning")
                }
                Button(
                    onClick = { onAnswer(true) },
                    enabled = !hasAnswered,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I knew it")
                }
            }
        }
    }
}

@Composable
private fun FeedbackCard(
    isCorrect: Boolean,
    correctAnswer: String
) {
    val container = if (isCorrect) {
        androidx.compose.ui.graphics.Color(0xFFD3F1E2)
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val accent = if (isCorrect) {
        androidx.compose.ui.graphics.Color(0xFF1F9D6F)
    } else {
        MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = container,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isCorrect) "Correct" else "Review this one",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Text(
                text = correctAnswer,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EmptyPracticeState(
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        EmptyState(
            iconRes = R.drawable.ic_school,
            title = "No practice deck yet",
            body = "Save a few words or phrases from an article and they'll appear here as flashcards.",
            iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun PracticeCompleteState(
    correct: Int,
    answered: Int,
    onRestart: () -> Unit,
    onOpenSavedWords: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = if (answered == 0) 0 else (correct * 100) / answered
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Pill(
                        text = "Session complete",
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        content = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$correct correct out of $answered",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    "Practice Again",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            OutlinedButton(
                onClick = onOpenSavedWords,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    "Review saved words",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private fun buildPracticeChoices(
    current: SavedWord,
    allWords: List<SavedWord>
): List<SavedWord> {
    if (allWords.size <= 1) return listOf(current)

    val distractors = mutableListOf<SavedWord>()
    val usedKeys = mutableSetOf(current.savedKey)
    val usedMeanings = mutableSetOf(current.meaning.meaningKannada)
    val maxAttempts = allWords.size * 3
    var attempts = 0

    while (distractors.size < 3 && attempts < maxAttempts) {
        attempts += 1
        val candidate = allWords[Random.nextInt(allWords.size)]
        if (
            candidate.savedKey !in usedKeys &&
            candidate.meaning.meaningKannada !in usedMeanings
        ) {
            distractors += candidate
            usedKeys += candidate.savedKey
            usedMeanings += candidate.meaning.meaningKannada
        }
    }

    if (distractors.size < 3) {
        allWords.forEach { candidate ->
            if (
                distractors.size < 3 &&
                candidate.savedKey !in usedKeys &&
                candidate.meaning.meaningKannada !in usedMeanings
            ) {
                distractors += candidate
                usedKeys += candidate.savedKey
                usedMeanings += candidate.meaning.meaningKannada
            }
        }
    }

    return (listOf(current) + distractors).shuffled()
}
