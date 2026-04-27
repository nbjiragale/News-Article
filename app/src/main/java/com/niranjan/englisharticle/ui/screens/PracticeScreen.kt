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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.R
import com.niranjan.englisharticle.domain.SavedWord
import com.niranjan.englisharticle.ui.components.AppTopBar
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
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = if (currentIndex == deck.lastIndex) "Finish" else "Next",
                        style = MaterialTheme.typography.labelLarge
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Practice",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Card ${currentIndex + 1} of $totalCount | $sessionCorrect/$sessionAnswered correct",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PromptCard(savedWord: SavedWord) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose the Kannada meaning",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = savedWord.word,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (savedWord.sentence.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Text(
                    text = savedWord.sentence,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            OutlinedButton(
                onClick = {
                    if (selectedCorrect == null) onSelect(choice)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = when {
                        isCorrectChoice -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        isSelected -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
                        else -> MaterialTheme.colorScheme.surface
                    },
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = choice.meaning.meaningKannada,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCorrect) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isCorrect) "Correct" else "Review this one",
                style = MaterialTheme.typography.titleLarge,
                color = if (isCorrect) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
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
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_school),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "No practice deck yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Save a few words or phrases from an article first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_school),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Practice complete",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$correct/$answered correct",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Practice Again")
                }
                OutlinedButton(
                    onClick = onOpenSavedWords,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Review Saved Words")
                }
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
