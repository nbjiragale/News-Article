package com.niranjan.englisharticle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niranjan.englisharticle.data.ArticleLocalStore
import com.niranjan.englisharticle.data.YouTubeTranscriptService
import com.niranjan.englisharticle.domain.ArticleAiService
import com.niranjan.englisharticle.domain.CleanArticleResult
import com.niranjan.englisharticle.domain.MeaningLookupMode
import com.niranjan.englisharticle.domain.MeaningResult
import com.niranjan.englisharticle.domain.RecentArticle
import com.niranjan.englisharticle.domain.SavedWord
import com.niranjan.englisharticle.domain.articleWordCount
import com.niranjan.englisharticle.ui.state.MeaningUiState
import com.niranjan.englisharticle.ui.state.SelectedWord
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EnglishLearningUiState(
    val draftArticle: String = "",
    val rawArticleForRetry: String = "",
    val cleanedArticle: CleanArticleResult? = null,
    val isCleaningArticle: Boolean = false,
    val isSummarizingArticle: Boolean = false,
    val summaryError: String? = null,
    val cleaningError: String? = null,
    val draftYouTubeUrl: String = "",
    val isImportingYouTube: Boolean = false,
    val youTubeError: String? = null,
    val selected: SelectedWord? = null,
    val meaningState: MeaningUiState = MeaningUiState.Idle
)

sealed interface EnglishNavigationEvent {
    data object OpenReader : EnglishNavigationEvent
}

class EnglishLearningViewModel(
    private val articleService: ArticleAiService,
    private val localStore: ArticleLocalStore,
    private val youTubeTranscriptService: YouTubeTranscriptService = YouTubeTranscriptService()
) : ViewModel() {
    private val _uiState = MutableStateFlow(EnglishLearningUiState())
    val uiState: StateFlow<EnglishLearningUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<EnglishNavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    val recentArticles: StateFlow<List<RecentArticle>> = localStore
        .observeRecentArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedWords: StateFlow<List<SavedWord>> = localStore
        .observeSavedWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var meaningLookupJob: Job? = null
    private var articleCleaningJob: Job? = null
    private var summaryJob: Job? = null
    private var youTubeImportJob: Job? = null

    fun updateDraftArticle(article: String) {
        _uiState.update {
            it.copy(
                draftArticle = article,
                cleaningError = null
            )
        }
    }

    fun cleanDraftArticle() {
        cleanRawArticle(uiState.value.draftArticle)
    }

    fun retryCleanArticle() {
        val currentState = uiState.value
        cleanRawArticle(currentState.rawArticleForRetry.ifBlank { currentState.draftArticle })
    }

    fun clearCurrentArticle() {
        stopArticleWork()
        _uiState.update {
            it.copy(
                cleanedArticle = null,
                summaryError = null
            )
        }
    }

    fun clearArticleText() {
        stopArticleWork()
        _uiState.value = EnglishLearningUiState()
    }

    /**
     * Cancels every job that loads or enriches the current article (cleaning, YouTube import,
     * summary, word lookup) and clears their busy flags in the same step.
     *
     * Each entry point used to cancel only some of these and leave the matching flag set. A
     * cancelled job never reaches the line that clears its flag, so the spinner stayed on and
     * the button stayed disabled until the app was restarted. A YouTube import that was not
     * cancelled could also finish later and replace the article the user had just opened.
     *
     * Idiom extraction is deliberately left running: it only adds data to an article that is
     * already saved, and [enrichShownArticle] ignores it if another article is on screen.
     */
    private fun stopArticleWork() {
        articleCleaningJob?.cancel()
        youTubeImportJob?.cancel()
        summaryJob?.cancel()
        meaningLookupJob?.cancel()
        _uiState.update {
            it.copy(
                isCleaningArticle = false,
                isImportingYouTube = false,
                isSummarizingArticle = false,
                selected = null,
                meaningState = MeaningUiState.Idle
            )
        }
    }

    fun updateDraftYouTubeUrl(url: String) {
        _uiState.update {
            it.copy(
                draftYouTubeUrl = url,
                youTubeError = null
            )
        }
    }

    fun dismissYouTubeError() {
        _uiState.update { it.copy(youTubeError = null) }
    }

    fun importYouTubeUrl() {
        val url = uiState.value.draftYouTubeUrl.trim()
        if (url.isBlank()) return
        if (uiState.value.isImportingYouTube) return

        stopArticleWork()
        _uiState.update {
            it.copy(
                isImportingYouTube = true,
                youTubeError = null,
                cleaningError = null
            )
        }

        youTubeImportJob = viewModelScope.launch {
            val transcript = try {
                youTubeTranscriptService.fetchTranscript(url)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isImportingYouTube = false,
                        youTubeError = error.message ?: "Could not load the transcript."
                    )
                }
                return@launch
            }

            if (transcript.transcript.length > MAX_ARTICLE_CHARS) {
                _uiState.update {
                    it.copy(
                        isImportingYouTube = false,
                        youTubeError = tooLongMessage(transcript.transcript.length)
                    )
                }
                return@launch
            }

            val resolved = formatTranscriptOrRaw(
                rawTranscript = transcript.transcript,
                title = transcript.title,
                author = transcript.author
            )

            _uiState.update {
                it.copy(
                    cleanedArticle = resolved,
                    isImportingYouTube = false,
                    youTubeError = null,
                    summaryError = null,
                    draftYouTubeUrl = "",
                    rawArticleForRetry = transcript.transcript
                )
            }
            _navigationEvents.emit(EnglishNavigationEvent.OpenReader)
            saveAndExtractPhrases(resolved)
        }
    }

    /**
     * Turns a raw caption track into readable text. Auto-generated captions have no
     * punctuation and no paragraphs, so the reader showed one giant block, sentence lookups
     * sent the whole video as one "sentence", and read-aloud broke words in half.
     *
     * The AI formatter adds punctuation and paragraphs. It is only used for transcripts up to
     * [MAX_AI_TRANSCRIPT_CHARS], because it must echo the whole text back within the request
     * timeout. Longer transcripts, failures, and answers that dropped too much text fall back
     * to the raw text, which the reader splits into short paragraphs locally.
     */
    private suspend fun formatTranscriptOrRaw(
        rawTranscript: String,
        title: String,
        author: String
    ): CleanArticleResult {
        val raw = CleanArticleResult(
            title = title,
            subtitle = "",
            author = author,
            publishedDate = "",
            cleanArticle = rawTranscript
        )
        if (rawTranscript.length > MAX_AI_TRANSCRIPT_CHARS) return raw

        val formatted = try {
            articleService.formatTranscript(rawTranscript, title, author)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            return raw
        }
        val keptEnoughText = formatted.cleanArticle.articleWordCount() >=
            rawTranscript.articleWordCount() * MIN_TRANSCRIPT_KEPT_RATIO
        if (formatted.cleanArticle.isBlank() || !keptEnoughText) return raw

        return formatted.copy(
            title = formatted.title.ifBlank { title },
            author = formatted.author.ifBlank { author }
        )
    }

    /**
     * Saves a newly loaded article to history right away, then adds idiomatic phrases in a
     * separate job.
     *
     * The save used to happen only after phrase extraction, inside the loading job. Going back
     * from the reader cancels that job, so an article the user left quickly was never saved.
     */
    private suspend fun saveAndExtractPhrases(article: CleanArticleResult) {
        runCatching { localStore.saveRecentArticle(article) }

        viewModelScope.launch {
            val detectedPhrases = try {
                articleService.extractIdiomaticPhrases(article.cleanArticle)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                emptyList()
            }
            if (detectedPhrases.isEmpty()) return@launch

            val articleToSave = enrichShownArticle(article) { shown ->
                shown.copy(idiomaticPhrases = detectedPhrases)
            }
            runCatching { localStore.saveRecentArticle(articleToSave) }
        }
    }

    fun importSharedArticleText(article: String) {
        val sharedArticle = article.trim()
        if (sharedArticle.isBlank()) return

        stopArticleWork()
        _uiState.value = EnglishLearningUiState(
            draftArticle = sharedArticle
        )
    }

    fun openRecentArticle(article: RecentArticle) {
        stopArticleWork()
        _uiState.update {
            it.copy(
                cleanedArticle = article.toCleanArticleResult(),
                summaryError = null
            )
        }
    }

    fun requestArticleContext() {
        val current = uiState.value.cleanedArticle ?: return
        if (current.cleanArticle.isBlank()) return
        if (current.summary != null && current.summary.isNotBlank()) return
        if (uiState.value.isSummarizingArticle) return

        summaryJob?.cancel()
        _uiState.update {
            it.copy(
                isSummarizingArticle = true,
                summaryError = null
            )
        }

        summaryJob = viewModelScope.launch {
            val summary = try {
                articleService.summarizeArticle(current.cleanArticle).takeIf { it.isNotBlank() }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isSummarizingArticle = false,
                        summaryError = error.message ?: "Could not generate context."
                    )
                }
                return@launch
            }

            if (summary == null) {
                _uiState.update {
                    it.copy(
                        isSummarizingArticle = false,
                        summaryError = "The model returned an empty summary."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isSummarizingArticle = false, summaryError = null) }
            val updated = enrichShownArticle(current) { article -> article.copy(summary = summary) }

            runCatching { localStore.saveRecentArticle(updated) }
        }
    }

    fun dismissSummaryError() {
        _uiState.update { it.copy(summaryError = null) }
    }

    fun selectWord(tapped: SelectedWord) {
        val currentArticle = uiState.value.cleanedArticle ?: return

        meaningLookupJob?.cancel()
        _uiState.update {
            it.copy(
                selected = tapped,
                meaningState = MeaningUiState.Loading
            )
        }

        meaningLookupJob = viewModelScope.launch {
            val cachedMeaning = localStore.getMeaning(
                word = tapped.word,
                sentence = tapped.sentence,
                lookupMode = tapped.lookupMode
            )
            val cachedMeaningIsValid = cachedMeaning != null &&
                cachedMeaning.hasValidKannadaFields() &&
                (
                    tapped.lookupMode == MeaningLookupMode.Word ||
                        cachedMeaning.hasUsableSentenceTranslation(tapped.sentence)
                    )

            if (cachedMeaning != null && cachedMeaningIsValid) {
                _uiState.updateMeaningIfStillSelected(
                    selected = tapped,
                    meaningState = MeaningUiState.Success(cachedMeaning)
                )
                return@launch
            }

            val nextMeaningState = runCatching {
                articleService.fetchMeaning(
                    articleText = currentArticle.contextForMeaning(),
                    sentence = tapped.sentence,
                    word = tapped.word,
                    lookupMode = tapped.lookupMode
                )
            }.fold(
                onSuccess = { meaning ->
                    runCatching {
                        localStore.saveMeaning(
                            word = tapped.word,
                            sentence = tapped.sentence,
                            lookupMode = tapped.lookupMode,
                            meaning = meaning
                        )
                    }
                    MeaningUiState.Success(meaning)
                },
                onFailure = { MeaningUiState.Error(it.message ?: "Could not load meaning.") }
            )

            _uiState.updateMeaningIfStillSelected(
                selected = tapped,
                meaningState = nextMeaningState
            )
        }
    }

    fun dismissMeaning() {
        meaningLookupJob?.cancel()
        _uiState.update {
            it.copy(
                selected = null,
                meaningState = MeaningUiState.Idle
            )
        }
    }

    fun setLookupMode(showSentence: Boolean) {
        val current = uiState.value.selected ?: return
        if (current.showSentence == showSentence) return
        selectWord(current.copy(showSentence = showSentence))
    }

    fun saveSelectedWord(result: MeaningResult) {
        val currentState = uiState.value
        val tapped = currentState.selected ?: return

        viewModelScope.launch {
            localStore.saveWord(
                word = tapped.word,
                sentence = tapped.sentence,
                lookupMode = tapped.lookupMode,
                articleTitle = currentState.cleanedArticle?.title.orEmpty(),
                meaning = result
            )
        }
    }

    fun deleteRecentArticle(id: Long) {
        viewModelScope.launch {
            localStore.deleteRecentArticle(id)
        }
    }

    fun clearRecentArticles() {
        viewModelScope.launch {
            localStore.clearRecentArticles()
        }
    }

    fun deleteSavedWord(savedKey: String) {
        viewModelScope.launch {
            localStore.deleteSavedWord(savedKey)
        }
    }

    fun recordPracticeResult(savedKey: String, isCorrect: Boolean) {
        viewModelScope.launch {
            localStore.recordPracticeResult(savedKey, isCorrect)
        }
    }

    /**
     * Applies [enrich] (add a summary, add idioms) to the article on screen and returns the
     * enriched copy to persist.
     *
     * The summary and the idiom extraction run as separate background jobs. Each one used to
     * copy the article it started from and write that copy back, so whichever finished last
     * threw away the other's result, on screen and in history. Enriching the *current* shown
     * article instead keeps both. If the user has moved to another article, [base] is enriched
     * and only persisted; the store merges it with what is already saved.
     */
    private fun enrichShownArticle(
        base: CleanArticleResult,
        enrich: (CleanArticleResult) -> CleanArticleResult
    ): CleanArticleResult {
        var result = enrich(base)
        _uiState.update { state ->
            val shown = state.cleanedArticle
            if (shown != null && shown.isSameArticleAs(base)) {
                result = enrich(shown)
                state.copy(cleanedArticle = result)
            } else {
                result = enrich(base)
                state
            }
        }
        return result
    }

    private fun cleanRawArticle(rawText: String) {
        val rawArticle = rawText.trim()
        if (rawArticle.isBlank()) return
        if (rawArticle.length > MAX_ARTICLE_CHARS) {
            _uiState.update { it.copy(cleaningError = tooLongMessage(rawArticle.length)) }
            return
        }

        stopArticleWork()
        _uiState.update {
            it.copy(
                rawArticleForRetry = rawArticle,
                isCleaningArticle = true,
                cleaningError = null,
                youTubeError = null
            )
        }

        articleCleaningJob = viewModelScope.launch {
            val result = try {
                articleService.cleanArticle(rawArticle)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        cleaningError = error.message ?: "Could not clean article.",
                        isCleaningArticle = false
                    )
                }
                return@launch
            }

            val displayResult = withContext(Dispatchers.Default) {
                result.withLocalArticleFallback(rawArticle)
            }
            if (displayResult.cleanArticle.isBlank()) {
                _uiState.update {
                    it.copy(
                        cleaningError = "The article cleaner returned empty content.",
                        isCleaningArticle = false
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    cleanedArticle = displayResult,
                    isCleaningArticle = false,
                    summaryError = null
                )
            }
            _navigationEvents.emit(EnglishNavigationEvent.OpenReader)
            saveAndExtractPhrases(displayResult)
        }
    }
}

/**
 * Upper bound on text accepted into the reader, applied to both pasted articles
 * and imported YouTube transcripts.
 *
 * This is a cost guard, not a technical limit. The app ships its own OpenRouter
 * and Deepgram keys (see the Distribution section of the README), so every
 * request is billed to the person who built it, and article length drives the
 * bill directly: cleaning echoes the body back, and summarizing and phrase
 * extraction each send it whole. Without a ceiling, one stray paste of a very
 * large document is an unbounded charge with no warning.
 *
 * 100k characters is roughly 15,000 words -- longer than any news article and
 * long enough for a feature-length YouTube transcript, so it should never bite
 * in normal use.
 *
 * Note this bounds per-article cost only. Per-tap cost is a separate problem:
 * word lookup currently sends the entire article as context on every tap. That
 * is tracked as H3 in docs/PRODUCTION_READINESS_REVIEW.md.
 */
private const val MAX_ARTICLE_CHARS = 100_000

/**
 * Longest transcript sent to the AI formatter. The formatter echoes the whole text back, so a
 * much longer one would not finish inside the request timeout. About 2,500 words, or a
 * 15-minute video; longer videos use the raw captions.
 */
private const val MAX_AI_TRANSCRIPT_CHARS = 15_000

/** The formatter may drop filler words; dropping more than this means it summarized. */
private const val MIN_TRANSCRIPT_KEPT_RATIO = 0.8

private fun tooLongMessage(actualLength: Int): String {
    val actualK = actualLength / 1_000
    val maxK = MAX_ARTICLE_CHARS / 1_000
    return "That text is too long to process (${actualK}k characters; the limit is ${maxK}k). " +
        "Try a single article rather than a whole page."
}

class EnglishLearningViewModelFactory(
    private val articleService: ArticleAiService,
    private val localStore: ArticleLocalStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EnglishLearningViewModel::class.java)) {
            return EnglishLearningViewModel(articleService, localStore) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun MutableStateFlow<EnglishLearningUiState>.updateMeaningIfStillSelected(
    selected: SelectedWord,
    meaningState: MeaningUiState
) {
    update { current ->
        if (current.selected == selected) {
            current.copy(meaningState = meaningState)
        } else {
            current
        }
    }
}

private fun CleanArticleResult.isSameArticleAs(other: CleanArticleResult): Boolean {
    return title == other.title && cleanArticle == other.cleanArticle
}
