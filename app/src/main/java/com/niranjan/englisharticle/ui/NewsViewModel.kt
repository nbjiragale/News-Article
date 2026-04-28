package com.niranjan.englisharticle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.niranjan.englisharticle.data.NewsApiService
import com.niranjan.englisharticle.domain.NewsArticle
import com.niranjan.englisharticle.domain.NewsCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewsUiState(
    val articles: List<NewsArticle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: NewsCategory = NewsCategory.General,
    val isSearchMode: Boolean = false
)

class NewsViewModel(
    private val newsApiService: NewsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        fetchHeadlines()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun search() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(isSearchMode = false) }
            fetchHeadlines()
            return
        }
        _uiState.update { it.copy(isSearchMode = true) }
        fetchJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        fetchJob = viewModelScope.launch {
            try {
                val results = newsApiService.searchEverything(query)
                _uiState.update {
                    it.copy(articles = results, isLoading = false, error = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Search failed.")
                }
            }
        }
    }

    fun selectCategory(category: NewsCategory) {
        if (category == _uiState.value.selectedCategory && !_uiState.value.isSearchMode) return
        _uiState.update {
            it.copy(selectedCategory = category, searchQuery = "", isSearchMode = false)
        }
        fetchHeadlines()
    }

    fun retry() {
        if (_uiState.value.isSearchMode) {
            search()
        } else {
            fetchHeadlines()
        }
    }

    private fun fetchHeadlines() {
        fetchJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        fetchJob = viewModelScope.launch {
            try {
                val results = newsApiService.topHeadlines(
                    category = _uiState.value.selectedCategory
                )
                _uiState.update {
                    it.copy(articles = results, isLoading = false, error = null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Could not load headlines.")
                }
            }
        }
    }
}

class NewsViewModelFactory(
    private val newsApiService: NewsApiService
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            return NewsViewModel(newsApiService) as T
        }
        error("Unknown ViewModel class: ${modelClass.name}")
    }
}
