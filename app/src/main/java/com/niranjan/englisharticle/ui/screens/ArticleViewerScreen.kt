package com.niranjan.englisharticle.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.WordToken
import com.niranjan.englisharticle.domain.WordTokenGroup
import com.niranjan.englisharticle.domain.findSentenceContaining
import com.niranjan.englisharticle.domain.isLikelyHeading
import com.niranjan.englisharticle.domain.toMeaningTokenGroups
import com.niranjan.englisharticle.domain.toWordTokens
import com.niranjan.englisharticle.ui.components.ArticleHeader
import com.niranjan.englisharticle.ui.components.ArticleHeroImage
import com.niranjan.englisharticle.ui.components.AppTopBar
import com.niranjan.englisharticle.ui.state.SelectedWord

@Composable
fun ArticleViewerScreen(
    article: CleanArticleResult,
    onBackToInput: () -> Unit,
    onOpenRecents: () -> Unit,
    onOpenSavedWords: () -> Unit,
    onOpenPractice: () -> Unit,
    onWordTap: (SelectedWord) -> Unit,
    modifier: Modifier = Modifier
) {
    val articleBody = article.cleanArticle
    val tokens = remember(articleBody) { articleBody.toWordTokens() }
    val paragraphs = remember(tokens) {
        tokens.groupBy { it.paragraphIndex }
            .entries
            .sortedBy { it.key }
            .map { (paragraphIndex, paragraphTokens) ->
                val text = paragraphTokens.joinToString(" ") { it.text }
                ArticleParagraph(
                    paragraphIndex = paragraphIndex,
                    tokens = paragraphTokens,
                    text = text,
                    isHeading = text.isLikelyHeading(paragraphIndex)
                )
            }
    }
    val firstBodyParagraphIndex = remember(paragraphs) {
        paragraphs.indexOfFirst { !it.isHeading }
    }
    val lookedUpWords = remember { mutableStateSetOf<String>() }
    val listState = rememberLazyListState()
    val readingProgress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems <= 1) {
                0f
            } else {
                listState.firstVisibleItemIndex.toFloat() / (totalItems - 1).toFloat()
            }
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = readingProgress,
        animationSpec = tween(durationMillis = 150),
        label = "reading_progress"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AppTopBar(
                showBack = true,
                onBack = onBackToInput,
                onRecentsClick = onOpenRecents,
                onSavedWordsClick = onOpenSavedWords,
                onPracticeClick = onOpenPractice
            )
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 32.dp, bottom = 128.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item(key = "article_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ArticleHeroImage()
                        ArticleHeader(article)
                    }
                }

                itemsIndexed(
                    items = paragraphs,
                    key = { _, paragraph -> "paragraph_${paragraph.paragraphIndex}" }
                ) { paraIndex, paragraph ->
                    if (paragraph.isHeading) {
                        ArticleHeading(text = paragraph.text)
                    } else {
                        InteractiveParagraph(
                            paragraphTokens = paragraph.tokens,
                            article = article,
                            articleBody = articleBody,
                            isFirstBodyParagraph = paraIndex == firstBodyParagraphIndex,
                            lookedUpWords = lookedUpWords,
                            onWordTap = onWordTap
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )
    }
}

@Composable
private fun ArticleHeading(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = 24.dp)
            .drawBehind {
                drawLine(
                    color = Color(0xFF6650A4),
                    start = Offset(-12.dp.toPx(), 0f),
                    end = Offset(-12.dp.toPx(), size.height),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun InteractiveParagraph(
    paragraphTokens: List<WordToken>,
    article: CleanArticleResult,
    articleBody: String,
    isFirstBodyParagraph: Boolean,
    lookedUpWords: MutableSet<String>,
    onWordTap: (SelectedWord) -> Unit
) {
    val isQuote = paragraphTokens.firstOrNull()?.text?.startsWith("\"") == true
    val tokenGroups = remember(paragraphTokens, article.idiomaticPhrases) {
        paragraphTokens.toMeaningTokenGroups(article.idiomaticPhrases)
    }
    val lookedUpSnapshot = lookedUpWords.toSet()
    val renderContent = buildParagraphRenderContent(
        tokenGroups = tokenGroups,
        lookedUpWords = lookedUpSnapshot,
        normalColor = MaterialTheme.colorScheme.onSurface,
        lookedUpColor = MaterialTheme.colorScheme.primary,
        phraseColor = Color(0xFF7C6FE8)
    )
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val doubleTapTracker = remember { DoubleTapTracker() }

    fun openMeaning(position: Offset) {
        val layoutResult = textLayoutResult ?: return
        val offset = layoutResult.getOffsetForPosition(position)
        val range = renderContent.ranges.firstOrNull { offset >= it.start && offset < it.end } ?: return
        val showSentence = doubleTapTracker.isDoubleTap(range.lookupText)

        lookedUpWords.add(range.lookupText.lowercase())
        onWordTap(
            SelectedWord(
                word = range.lookupText,
                sentence = articleBody.findSentenceContaining(range.sourceStartIndex),
                showSentence = showSentence
            )
        )
    }

    Text(
        text = renderContent.text,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = 24.dp)
            .pointerInput(renderContent.ranges) {
                detectTapGestures(
                    onTap = { position -> openMeaning(position) }
                )
            },
        style = if (isFirstBodyParagraph) {
            MaterialTheme.typography.bodyLarge.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize)
        } else {
            MaterialTheme.typography.bodyLarge
        },
        fontStyle = if (isQuote) FontStyle.Italic else FontStyle.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        onTextLayout = { textLayoutResult = it }
    )
}

@Composable
private fun buildParagraphRenderContent(
    tokenGroups: List<WordTokenGroup>,
    lookedUpWords: Set<String>,
    normalColor: Color,
    lookedUpColor: Color,
    phraseColor: Color
): ParagraphRenderContent {
    return remember(tokenGroups, lookedUpWords, normalColor, lookedUpColor, phraseColor) {
        val ranges = mutableListOf<InteractiveTextRange>()
        val text = buildAnnotatedString {
            tokenGroups.forEachIndexed { index, group ->
                if (index > 0) append(" ")

                val lookupText = group.cleanText()
                val start = length
                val color = when {
                    group.isPhrase -> phraseColor
                    lookupText.lowercase() in lookedUpWords -> lookedUpColor
                    else -> normalColor
                }

                withStyle(
                    SpanStyle(
                        color = color,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = if (lookupText.lowercase() in lookedUpWords) {
                            FontWeight.Medium
                        } else {
                            FontWeight.Normal
                        }
                    )
                ) {
                    append(group.text)
                }

                val end = length
                if (lookupText.isNotBlank()) {
                    ranges += InteractiveTextRange(
                        start = start,
                        end = end,
                        lookupText = lookupText,
                        sourceStartIndex = group.startIndex
                    )
                }
            }
        }

        ParagraphRenderContent(text = text, ranges = ranges)
    }
}

private data class ParagraphRenderContent(
    val text: AnnotatedString,
    val ranges: List<InteractiveTextRange>
)

private data class InteractiveTextRange(
    val start: Int,
    val end: Int,
    val lookupText: String,
    val sourceStartIndex: Int
)

private data class ArticleParagraph(
    val paragraphIndex: Int,
    val tokens: List<WordToken>,
    val text: String,
    val isHeading: Boolean
)

private class DoubleTapTracker {
    private var lastWord: String = ""
    private var lastTimeMs: Long = 0L

    fun isDoubleTap(word: String): Boolean {
        val now = System.currentTimeMillis()
        val isDouble = lastWord == word && now - lastTimeMs < DOUBLE_TAP_TIMEOUT_MS
        if (isDouble) {
            lastWord = ""
            lastTimeMs = 0L
        } else {
            lastWord = word
            lastTimeMs = now
        }
        return isDouble
    }

    private companion object {
        const val DOUBLE_TAP_TIMEOUT_MS = 400L
    }
}
