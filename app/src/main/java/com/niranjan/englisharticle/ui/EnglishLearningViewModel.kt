package com.niranjan.englisharticle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niranjan.englisharticle.data.ArticleLocalStore
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EnglishLearningUiState(
    val draftArticle: String = "",
    val rawArticleForRetry: String = "",
    val cleanedArticle: CleanArticleResult? = null,
    val isCleaningArticle: Boolean = false,
    val isSummarizingArticle: Boolean = false,
    val cleaningError: String? = null,
    val selected: SelectedWord? = null,
    val meaningState: MeaningUiState = MeaningUiState.Idle
)

sealed interface EnglishNavigationEvent {
    data object OpenReader : EnglishNavigationEvent
}

class EnglishLearningViewModel(
    private val articleService: ArticleAiService,
    private val localStore: ArticleLocalStore
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
        _uiState.value = EnglishLearningUiState()
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
        _uiState.update {
            it.copy(
                cleanedArticle = article.toCleanArticleResult(),
                isCleaningArticle = false,
                selected = null,
                meaningState = MeaningUiState.Idle
            )
        }
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

            _uiState.update { it.copy(isSummarizingArticle = true) }

            val (detectedPhrases, summary) = coroutineScope {
                val phrasesDeferred = async {
                    runCatching {
                        articleService.extractIdiomaticPhrases(displayResult.cleanArticle)
                    }.getOrDefault(emptyList())
                }
                val summaryDeferred = async {
                    runCatching {
                        articleService.summarizeArticle(displayResult.cleanArticle)
                    }.getOrNull()?.takeIf { it.isNotBlank() }
                }
                phrasesDeferred.await() to summaryDeferred.await()
            }

            val articleToSave = displayResult.copy(
                idiomaticPhrases = detectedPhrases.ifEmpty { displayResult.idiomaticPhrases },
                summary = summary ?: displayResult.summary
            )

            _uiState.update { current ->
                if (current.cleanedArticle?.cleanArticle == displayResult.cleanArticle) {
                    current.copy(
                        cleanedArticle = articleToSave,
                        isSummarizingArticle = false
                    )
                } else {
                    current.copy(isSummarizingArticle = false)
                }
            }

            runCatching { localStore.saveRecentArticle(articleToSave) }
        }
    }
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
