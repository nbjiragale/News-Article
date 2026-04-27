package com.niranjan.englisharticle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.niranjan.englisharticle.ui.EnglishLearningApp
import com.niranjan.englisharticle.ui.SharedArticleText
import com.niranjan.englisharticle.ui.theme.EnglishArticleTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val sharedArticleText = MutableStateFlow<SharedArticleText?>(null)
    private var nextSharedArticleId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
        setContent {
            val sharedArticle by sharedArticleText.collectAsState()

            EnglishArticleTheme {
                EnglishLearningApp(
                    sharedArticleText = sharedArticle,
                    onSharedArticleTextHandled = ::markSharedArticleTextHandled
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val sharedText = intent
            ?.extractSharedArticleText(this)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return

        sharedArticleText.value = SharedArticleText(
            id = ++nextSharedArticleId,
            text = sharedText
        )
    }

    private fun markSharedArticleTextHandled(id: Long) {
        if (sharedArticleText.value?.id == id) {
            sharedArticleText.value = null
        }
    }
}

private fun Intent.extractSharedArticleText(context: Context): String? {
    fun MutableList<String>.addExtraText(extraName: String) {
        val text = this@extractSharedArticleText
            .getCharSequenceExtra(extraName)
            ?.toString()
        if (!text.isNullOrBlank()) add(text)
    }

    val parts = when (action) {
        Intent.ACTION_SEND -> buildList {
            addExtraText(Intent.EXTRA_TITLE)
            addExtraText(Intent.EXTRA_SUBJECT)
            addExtraText(Intent.EXTRA_TEXT)
            addAll(clipDataTexts(context))
        }

        Intent.ACTION_PROCESS_TEXT -> buildList {
            addExtraText(Intent.EXTRA_PROCESS_TEXT)
        }

        else -> emptyList()
    }

    return parts
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinctBy { it.normalizedForDuplicateCheck() }
        .joinToString(separator = "\n\n")
        .takeIf { it.isNotBlank() }
}

private fun Intent.clipDataTexts(context: Context): List<String> {
    val data = clipData ?: return emptyList()
    return (0 until data.itemCount).mapNotNull { index ->
        val item = data.getItemAt(index)
        item.text?.toString()
            ?: item.uri?.toString()
            ?: item.intent?.extractSharedArticleText(context)
            ?: item.coerceToText(context)?.toString()
    }
}

private fun String.normalizedForDuplicateCheck(): String {
    return lowercase().replace(Regex("\\s+"), " ").trim()
}
