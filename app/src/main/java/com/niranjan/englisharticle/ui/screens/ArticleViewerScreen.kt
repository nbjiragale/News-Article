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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.niranjan.englisharticle.domain.ensureParagraphs
import com.niranjan.englisharticle.domain.findSentenceContaining
import com.niranjan.englisharticle.domain.isLikelyHeading
import com.niranjan.englisharticle.domain.toMeaningTokenGroups
import com.niranjan.englisharticle.domain.toWordTokens
import com.niranjan.englisharticle.ui.components.ArticleHeader
import com.niranjan.englisharticle.ui.components.ArticleListenButton
import com.niranjan.englisharticle.ui.components.ArticleSummaryCard
import com.niranjan.englisharticle.ui.components.AppTopBar
import com.niranjan.englisharticle.ui.state.SelectedWord
import com.niranjan.englisharticle.ui.tts.ArticlePlaybackState

@Composable
fun ArticleViewerScreen(
    article: CleanArticleResult,
    isSummarizing: Boolean,
    summaryError: String?,
    onBackToInput: () -> Unit,
    onOpenRecents: () -> Unit,
    onOpenSavedWords: () -> Unit,
    onOpenPractice: () -> Unit,
    onWordTap: (SelectedWord) -> Unit,
    onRequestContext: () -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onSpeakKannada: (String) -> Unit,
    playbackState: ArticlePlaybackState,
    currentWordIndex: Int?,
    onStartListening: (text: String, wordOffset: Int) -> Unit,
    onPauseListening: () -> Unit,
    onResumeListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    val articleBody = remember(article.cleanArticle) { article.cleanArticle.ensureParagraphs() }
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
    val paragraphWordOffsets = remember(paragraphs) {
        var sum = 0
        paragraphs.map { paragraph ->
            val start = sum
            sum += paragraph.tokens.size
            start
        }
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

    LaunchedEffect(currentWordIndex, paragraphs) {
        val wordIndex = currentWordIndex ?: return@LaunchedEffect
        val paragraphIndex = paragraphWordOffsets.indexOfLast { it <= wordIndex }
            .takeIf { it >= 0 } ?: return@LaunchedEffect
        val targetLazyIndex = 1 + paragraphIndex
        val isVisible = listState.layoutInfo.visibleItemsInfo.any { info ->
            info.index == targetLazyIndex && info.offset >= 0 &&
                info.offset + info.size <= listState.layoutInfo.viewportEndOffset
        }
        if (!isVisible) {
            runCatching { listState.animateScrollToItem(targetLazyIndex) }
        }
    }

    fun startFromCurrentScroll() {
        if (paragraphs.isEmpty()) return
        val firstVisibleItem = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.offset + it.size > 0 }
        val firstVisibleLazyIndex = firstVisibleItem?.index ?: 0
        val startParagraphIndex = (firstVisibleLazyIndex - 1).coerceIn(0, paragraphs.size - 1)
        val firstBody = paragraphs.indexOfFirst { idx -> idx.paragraphIndex == startParagraphIndex }
            .takeIf { it >= 0 } ?: 0
        val text = paragraphs.subList(firstBody, paragraphs.size)
            .joinToString("\n\n") { it.text }
        val wordOffset = paragraphWordOffsets.getOrElse(firstBody) { 0 }
        onStartListening(text, wordOffset)
    }

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
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainer,
                drawStopIndicator = {}
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
                        ArticleHeader(article)
                        ArticleSummaryCard(
                            summary = article.summary,
                            isLoading = isSummarizing,
                            errorMessage = summaryError,
                            onRequestContext = onRequestContext,
                            onSpeakEnglish = onSpeakEnglish,
                            onSpeakKannada = onSpeakKannada
                        )
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
                            paragraphFirstWordIndex = paragraphWordOffsets.getOrElse(paraIndex) { 0 },
                            currentWordIndex = currentWordIndex,
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

        ArticleListenButton(
            state = playbackState,
            onToggle = {
                when (playbackState) {
                    ArticlePlaybackState.Idle -> startFromCurrentScroll()
                    ArticlePlaybackState.Playing -> onPauseListening()
                    ArticlePlaybackState.Paused -> onResumeListening()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        )
    }
}

@Composable
private fun ArticleHeading(text: String) {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .drawBehind {
                drawLine(
                    color = accent,
                    start = Offset(-12.dp.toPx(), 0f),
                    end = Offset(-12.dp.toPx(), size.height),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InteractiveParagraph(
    paragraphTokens: List<WordToken>,
    article: CleanArticleResult,
    articleBody: String,
    isFirstBodyParagraph: Boolean,
    paragraphFirstWordIndex: Int,
    currentWordIndex: Int?,
    lookedUpWords: MutableSet<String>,
    onWordTap: (SelectedWord) -> Unit
) {
    val isQuote = paragraphTokens.firstOrNull()?.text?.startsWith("\"") == true
    val tokenGroups = remember(paragraphTokens, article.idiomaticPhrases) {
        paragraphTokens.toMeaningTokenGroups(article.idiomaticPhrases)
    }
    val groupTokenRanges = remember(tokenGroups) {
        var index = 0
        tokenGroups.map { group ->
            val start = index
            index += group.tokens.size
            start until index
        }
    }
    val highlightedGroupIndex = remember(currentWordIndex, paragraphFirstWordIndex, groupTokenRanges) {
        val idx = currentWordIndex ?: return@remember -1
        val relative = idx - paragraphFirstWordIndex
        if (relative < 0) return@remember -1
        groupTokenRanges.indexOfFirst { range -> relative in range }
    }
    val lookedUpSnapshot = lookedUpWords.toSet()
    val highlightBackground = MaterialTheme.colorScheme.primaryContainer
    val highlightForeground = MaterialTheme.colorScheme.onPrimaryContainer
    val renderContent = buildParagraphRenderContent(
        tokenGroups = tokenGroups,
        lookedUpWords = lookedUpSnapshot,
        normalColor = MaterialTheme.colorScheme.onSurface,
        lookedUpColor = MaterialTheme.colorScheme.primary,
        phraseColor = MaterialTheme.colorScheme.tertiary,
        highlightedGroupIndex = highlightedGroupIndex,
        highlightBackground = highlightBackground,
        highlightForeground = highlightForeground
    )
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    fun openMeaning(position: Offset, showSentence: Boolean) {
        val layoutResult = textLayoutResult ?: return
        val offset = layoutResult.getOffsetForPosition(position)
        val range = renderContent.ranges.firstOrNull { offset >= it.start && offset < it.end } ?: return

        lookedUpWords.add(range.lookupText.lowercase())
        onWordTap(
            SelectedWord(
                word = range.lookupText,
                sentence = articleBody.findSentenceContaining(range.sourceStartIndex),
                showSentence = showSentence
            )
        )
    }

    val baseStyle = MaterialTheme.typography.bodyLarge
    val style = if (isFirstBodyParagraph) {
        baseStyle.copy(
            fontSize = androidx.compose.ui.unit.TextUnit(21f, androidx.compose.ui.unit.TextUnitType.Sp),
            lineHeight = androidx.compose.ui.unit.TextUnit(33f, androidx.compose.ui.unit.TextUnitType.Sp)
        )
    } else {
        baseStyle
    }

    Text(
        text = renderContent.text,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = 24.dp)
            .pointerInput(renderContent.ranges) {
                detectTapGestures(
                    onTap = { position -> openMeaning(position, showSentence = false) },
                    onLongPress = { position -> openMeaning(position, showSentence = true) }
                )
            },
        style = style,
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
    phraseColor: Color,
    highlightedGroupIndex: Int,
    highlightBackground: Color,
    highlightForeground: Color
): ParagraphRenderContent {
    return remember(
        tokenGroups,
        lookedUpWords,
        normalColor,
        lookedUpColor,
        phraseColor,
        highlightedGroupIndex,
        highlightBackground,
        highlightForeground
    ) {
        val ranges = mutableListOf<InteractiveTextRange>()
        val text = buildAnnotatedString {
            tokenGroups.forEachIndexed { index, group ->
                if (index > 0) append(" ")

                val lookupText = group.cleanText()
                val start = length
                val isHighlighted = index == highlightedGroupIndex
                val baseColor = when {
                    group.isPhrase -> phraseColor
                    lookupText.lowercase() in lookedUpWords -> lookedUpColor
                    else -> normalColor
                }

                val isLookedUp = lookupText.lowercase() in lookedUpWords
                val style = SpanStyle(
                    color = if (isHighlighted) highlightForeground else baseColor,
                    background = if (isHighlighted) highlightBackground else Color.Unspecified,
                    textDecoration = when {
                        group.isPhrase -> TextDecoration.Underline
                        else -> TextDecoration.None
                    },
                    fontWeight = when {
                        isHighlighted -> FontWeight.SemiBold
                        isLookedUp || group.isPhrase -> FontWeight.SemiBold
                        else -> FontWeight.Normal
                    }
                )
                withStyle(style) {
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

@Suppress("unused")
private fun LazyListState.firstFullyVisibleParagraphIndex(): Int =
    layoutInfo.visibleItemsInfo
        .firstOrNull { it.offset >= 0 }
        ?.index ?: 0
