package com.niranjan.englisharticle.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.niranjan.englisharticle.BuildConfig
import com.niranjan.englisharticle.data.ArticleLocalStore
import com.niranjan.englisharticle.data.GNewsApiService
import com.niranjan.englisharticle.data.JinaReaderService
import com.niranjan.englisharticle.data.OpenRouterArticleService
import com.niranjan.englisharticle.data.local.EnglishArticleDatabase
import com.niranjan.englisharticle.data.local.RoomArticleLocalStore
import com.niranjan.englisharticle.domain.ArticleAiService
import com.niranjan.englisharticle.domain.createSavedWordKey
import com.niranjan.englisharticle.ui.components.BottomSheetHandle
import com.niranjan.englisharticle.ui.screens.ArticleInputScreen
import com.niranjan.englisharticle.ui.screens.ArticleViewerScreen
import com.niranjan.englisharticle.ui.screens.BrowseNewsScreen
import com.niranjan.englisharticle.ui.screens.MeaningSheet
import com.niranjan.englisharticle.ui.screens.PracticeScreen
import com.niranjan.englisharticle.ui.screens.RecentArticlesScreen
import com.niranjan.englisharticle.ui.screens.SavedWordsScreen
import com.niranjan.englisharticle.ui.tts.rememberArticleSpeaker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnglishLearningApp(
    articleService: ArticleAiService = rememberDefaultArticleService(),
    localStore: ArticleLocalStore = rememberDefaultLocalStore(),
    readerService: JinaReaderService = rememberDefaultReaderService(),
    sharedArticleText: SharedArticleText? = null,
    onSharedArticleTextHandled: (Long) -> Unit = {}
) {
    val textToSpeech = rememberArticleSpeaker()
    val navController = rememberNavController()
    val viewModel: EnglishLearningViewModel = viewModel(
        factory = remember(articleService, localStore, readerService) {
            EnglishLearningViewModelFactory(
                articleService = articleService,
                localStore = localStore,
                readerService = readerService
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val recentArticles by viewModel.recentArticles.collectAsState()
    val savedWords by viewModel.savedWords.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(navController, viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                EnglishNavigationEvent.OpenReader -> navController.navigateSingleTop(AppRoute.Reader)
            }
        }
    }

    LaunchedEffect(sharedArticleText?.id, navController, viewModel) {
        val sharedArticle = sharedArticleText ?: return@LaunchedEffect
        viewModel.importSharedArticleText(sharedArticle.text)
        navController.navigate(AppRoute.Input) {
            popUpTo(AppRoute.Input) { inclusive = false }
            launchSingleTop = true
        }
        onSharedArticleTextHandled(sharedArticle.id)
    }

    fun navigateBack() {
        if (!navController.navigateUp()) {
            navController.navigateSingleTop(AppRoute.Input)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Input,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Input) {
                ArticleInputScreen(
                    article = uiState.draftArticle,
                    onArticleChange = viewModel::updateDraftArticle,
                    onLoadArticle = viewModel::cleanDraftArticle,
                    isCleaning = uiState.isCleaningArticle,
                    cleaningError = uiState.cleaningError,
                    onRetry = viewModel::retryCleanArticle,
                    onClearArticle = viewModel::clearArticleText,
                    onOpenRecents = { navController.navigateSingleTop(AppRoute.Recents) },
                    onOpenSavedWords = { navController.navigateSingleTop(AppRoute.SavedWords) },
                    onOpenPractice = { navController.navigateSingleTop(AppRoute.Practice) },
                    onOpenNews = { navController.navigateSingleTop(AppRoute.News) }
                )
            }

            composable(AppRoute.News) {
                val newsApiService = rememberNewsApiService()
                val newsVm: NewsViewModel = viewModel(
                    factory = remember(newsApiService) {
                        NewsViewModelFactory(newsApiService)
                    }
                )
                val newsState by newsVm.uiState.collectAsState()
                BrowseNewsScreen(
                    articles = newsState.articles,
                    isLoading = newsState.isLoading,
                    error = newsState.error,
                    searchQuery = newsState.searchQuery,
                    selectedCategory = newsState.selectedCategory,
                    onSearchQueryChange = newsVm::updateSearchQuery,
                    onSearch = newsVm::search,
                    onCategorySelected = newsVm::selectCategory,
                    onArticleClick = { newsArticle ->
                        viewModel.openNewsArticle(newsArticle)
                        navController.navigate(AppRoute.Input) {
                            popUpTo(AppRoute.Input) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onRetry = newsVm::retry,
                    onBack = { navigateBack() }
                )
            }

            composable(AppRoute.Reader) {
                val currentArticle = uiState.cleanedArticle
                if (currentArticle == null) {
                    ArticleInputScreen(
                        article = uiState.draftArticle,
                        onArticleChange = viewModel::updateDraftArticle,
                        onLoadArticle = viewModel::cleanDraftArticle,
                        isCleaning = uiState.isCleaningArticle,
                        cleaningError = uiState.cleaningError,
                        onRetry = viewModel::retryCleanArticle,
                        onClearArticle = viewModel::clearArticleText,
                        onOpenRecents = { navController.navigateSingleTop(AppRoute.Recents) },
                        onOpenSavedWords = { navController.navigateSingleTop(AppRoute.SavedWords) },
                        onOpenPractice = { navController.navigateSingleTop(AppRoute.Practice) },
                        onOpenNews = { navController.navigateSingleTop(AppRoute.News) }
                    )
                } else {
                    val playbackState by textToSpeech.playbackState
                    val currentWordIndex by textToSpeech.currentWordIndex
                    ArticleViewerScreen(
                        article = currentArticle,
                        isSummarizing = uiState.isSummarizingArticle,
                        summaryError = uiState.summaryError,
                        onBackToInput = {
                            textToSpeech.stop()
                            viewModel.clearCurrentArticle()
                            navController.navigate(AppRoute.Input) {
                                popUpTo(AppRoute.Input) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onOpenRecents = { navController.navigateSingleTop(AppRoute.Recents) },
                        onOpenSavedWords = { navController.navigateSingleTop(AppRoute.SavedWords) },
                        onOpenPractice = { navController.navigateSingleTop(AppRoute.Practice) },
                        onWordTap = viewModel::selectWord,
                        onRequestContext = viewModel::requestArticleContext,
                        onSpeakEnglish = textToSpeech::speakArticleEnglish,
                        onSpeakKannada = textToSpeech::speakKannada,
                        playbackState = playbackState,
                        currentWordIndex = currentWordIndex,
                        onStartListening = { text, wordOffset ->
                            textToSpeech.speakArticleEnglish(text, wordOffset)
                        },
                        onPauseListening = textToSpeech::pauseArticle,
                        onResumeListening = textToSpeech::resumeArticle,
                    )
                }
            }

            composable(AppRoute.Recents) {
                RecentArticlesScreen(
                    articles = recentArticles,
                    onBack = { navigateBack() },
                    onOpenArticle = { recentArticle ->
                        viewModel.openRecentArticle(recentArticle)
                        navController.navigateSingleTop(AppRoute.Reader)
                    }
                )
            }

            composable(AppRoute.SavedWords) {
                SavedWordsScreen(
                    savedWords = savedWords,
                    onBack = { navigateBack() },
                    onPractice = { navController.navigateSingleTop(AppRoute.Practice) },
                    onDeleteWord = { savedWord ->
                        viewModel.deleteSavedWord(savedWord.savedKey)
                    }
                )
            }

            composable(AppRoute.Practice) {
                PracticeScreen(
                    savedWords = savedWords,
                    onBack = { navigateBack() },
                    onOpenSavedWords = { navController.navigateSingleTop(AppRoute.SavedWords) },
                    onPracticeAnswer = { savedWord, isCorrect ->
                        viewModel.recordPracticeResult(savedWord.savedKey, isCorrect)
                    }
                )
            }
        }
    }

    uiState.selected?.let { selectedWord ->
        val selectedSavedKey = createSavedWordKey(
            word = selectedWord.word,
            sentence = selectedWord.sentence,
            lookupMode = selectedWord.lookupMode
        )
        val selectedIsSaved = savedWords.any { it.savedKey == selectedSavedKey }

        val activeSpeechText by textToSpeech.currentShortSpeechText
        DisposableEffect(selectedWord.word, selectedWord.sentence, selectedWord.lookupMode) {
            onDispose { textToSpeech.stopShortSpeech() }
        }
        ModalBottomSheet(
            onDismissRequest = {
                textToSpeech.stopShortSpeech()
                viewModel.dismissMeaning()
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
            dragHandle = { BottomSheetHandle() }
        ) {
            MeaningSheet(
                word = selectedWord.word,
                sentence = selectedWord.sentence,
                showSentence = selectedWord.showSentence,
                state = uiState.meaningState,
                isSaved = selectedIsSaved,
                activeSpeechText = activeSpeechText,
                onSpeakEnglish = textToSpeech::toggleEnglish,
                onSpeakKannada = textToSpeech::toggleKannada,
                onSaveWord = viewModel::saveSelectedWord,
                onPractice = { result ->
                    textToSpeech.stopShortSpeech()
                    viewModel.saveSelectedWord(result)
                    viewModel.dismissMeaning()
                    navController.navigateSingleTop(AppRoute.Practice)
                },
                onChangeMode = viewModel::setLookupMode,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun rememberDefaultArticleService(): ArticleAiService {
    return remember {
        OpenRouterArticleService(
            apiKey = BuildConfig.OPENROUTER_API_KEY,
            model = BuildConfig.OPENROUTER_MODEL
        )
    }
}

@Composable
private fun rememberDefaultLocalStore(): ArticleLocalStore {
    val context = LocalContext.current
    return remember(context) {
        val database = EnglishArticleDatabase.getInstance(context)
        RoomArticleLocalStore(
            meaningDao = database.meaningDao(),
            recentArticleDao = database.recentArticleDao(),
            savedWordDao = database.savedWordDao()
        )
    }
}

@Composable
private fun rememberNewsApiService(): GNewsApiService {
    return remember {
        GNewsApiService(apiKey = BuildConfig.GNEWS_API_KEY)
    }
}

@Composable
private fun rememberDefaultReaderService(): JinaReaderService {
    return remember { JinaReaderService() }
}

private object AppRoute {
    const val Input = "input"
    const val Reader = "reader"
    const val Recents = "recents"
    const val SavedWords = "saved_words"
    const val Practice = "practice"
    const val News = "news"
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}
