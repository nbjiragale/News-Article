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
        articleCleaningJob?.cancel()
        meaningLookupJob?.cancel()
        _uiState.update {
            it.copy(
                cleanedArticle = null,
                isCleaningArticle = false,
                selected = null,
                meaningState = MeaningUiState.Idle
            )
        }
    }

    fun clearArticleText() {
        articleCleaningJob?.cancel()
        meaningLookupJob?.cancel()
        youTubeImportJob?.cancel()
        _uiState.value = EnglishLearningUiState()
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

        articleCleaningJob?.cancel()
        meaningLookupJob?.cancel()
        summaryJob?.cancel()
        youTubeImportJob?.cancel()

        _uiState.update {
            it.copy(
                isImportingYouTube = true,
                youTubeError = null,
                cleaningError = null,
                selected = null,
                meaningState = MeaningUiState.Idle
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

            val resolved = CleanArticleResult(
                title = transcript.title,
                subtitle = "",
                author = transcript.author,
                publishedDate = "",
                cleanArticle = transcript.transcript
            )

            _uiState.update {
                it.copy(
                    cleanedArticle = resolved,
                    isImportingYouTube = false,
                    youTubeError = null,
                    draftYouTubeUrl = "",
                    rawArticleForRetry = transcript.transcript
                )
            }
            _navigationEvents.emit(EnglishNavigationEvent.OpenReader)

            val detectedPhrases = try {
                articleService.extractIdiomaticPhrases(resolved.cleanArticle)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                emptyList()
            }
            val articleToSave = if (detectedPhrases.isEmpty()) {
                resolved
            } else {
                resolved.copy(idiomaticPhrases = detectedPhrases)
            }

            if (detectedPhrases.isNotEmpty()) {
                _uiState.update { current ->
                    if (current.cleanedArticle?.cleanArticle == resolved.cleanArticle) {
                        current.copy(cleanedArticle = articleToSave)
                    } else {
                        current
                    }
                }
            }

            runCatching { localStore.saveRecentArticle(articleToSave) }
        }
    }

    fun importSharedArticleText(article: String) {
        val sharedArticle = article.trim()
        if (sharedArticle.isBlank()) return

        articleCleaningJob?.cancel()
        meaningLookupJob?.cancel()
        _uiState.value = EnglishLearningUiState(
            draftArticle = sharedArticle
        )
    }

    fun openRecentArticle(article: RecentArticle) {
        articleCleaningJob?.cancel()
        meaningLookupJob?.cancel()
        summaryJob?.cancel()
        _uiState.update {
            it.copy(
                cleanedArticle = article.toCleanArticleResult(),
                isCleaningArticle = false,
                isSummarizingArticle = false,
                summaryError = null,
                selected = null,
                meaningState = MeaningUiState.Idle
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

            val updated = current.copy(summary = summary)
            _uiState.update { state ->
                if (state.cleanedArticle?.cleanArticle == current.cleanArticle) {
                    state.copy(
                        cleanedArticle = updated,
                        isSummarizingArticle = false,
                        summaryError = null
                    )
                } else {
                    state.copy(
                        isSummarizingArticle = false,
                        summaryError = null
                    )
                }
            }

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

    private fun cleanRawArticle(rawText: String) {
        val rawArticle = rawText.trim()
        if (rawArticle.isBlank()) return
        if (rawArticle.length > MAX_ARTICLE_CHARS) {
            _uiState.update { it.copy(cleaningError = tooLongMessage(rawArticle.length)) }
            return
        }

        articleCleaningJob?.cancel()
        meaningLookupJob?.cancel()
        _uiState.update {
            it.copy(
                rawArticleForRetry = rawArticle,
                isCleaningArticle = true,
                cleaningError = null,
                selected = null,
                meaningState = MeaningUiState.Idle
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
                    isCleaningArticle = false
                )
            }
            _navigationEvents.emit(EnglishNavigationEvent.OpenReader)

            val detectedPhrases = try {
                articleService.extractIdiomaticPhrases(displayResult.cleanArticle)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                emptyList()
            }
            val articleToSave = if (detectedPhrases.isEmpty()) {
                displayResult
            } else {
                displayResult.copy(idiomaticPhrases = detectedPhrases)
            }

            if (detectedPhrases.isNotEmpty()) {
                _uiState.update { current ->
                    if (current.cleanedArticle?.cleanArticle == displayResult.cleanArticle) {
                        current.copy(cleanedArticle = articleToSave)
                    } else {
                        current
                    }
                }
            }

            runCatching { localStore.saveRecentArticle(articleToSave) }
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
