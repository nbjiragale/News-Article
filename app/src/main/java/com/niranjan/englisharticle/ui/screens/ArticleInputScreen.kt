package com.niranjan.englisharticle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.R
import com.niranjan.englisharticle.ui.components.AppTopBar

/**
 * Home — the page you land on.
 *
 * Two things happen here and they are not equal: most visits continue the article you
 * were already reading, and some start a new one. So continuing comes first and is the
 * only object on the page; starting something new is the plain working area below it.
 *
 * The old layout led with a disabled grey "Open article" slab that could not be pressed
 * until you had pasted something. Nothing here is shown disabled — the primary button is
 * whatever you can actually do right now.
 */
@Composable
fun ArticleInputScreen(
    article: String,
    onArticleChange: (String) -> Unit,
    onLoadArticle: () -> Unit,
    isCleaning: Boolean,
    cleaningError: String?,
    onRetry: () -> Unit,
    onClearArticle: () -> Unit,
    onOpenRecents: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onOpenSavedWords: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onOpenPractice: () -> Unit = {},
    latestArticleTitle: String? = null,
    onContinueReading: () -> Unit = {},
    youTubeUrl: String = "",
    onYouTubeUrlChange: (String) -> Unit = {},
    onImportYouTube: () -> Unit = {},
    isImportingYouTube: Boolean = false,
    youTubeError: String? = null,
    onDismissYouTubeError: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 0 = Paste text  |  1 = YouTube
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Once there is something to open, the page stops selling the idea and gets out of
    // the way: the standfirst goes, so the article and its button fit on one screen.
    val isComposing = article.isNotBlank() || isCleaning

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AppTopBar(showBack = false, onRecentsClick = onOpenRecents)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(28.dp))

            // ── Masthead ───────────────────────────────────────
            // The second line is in Kannada because the promise is bilingual, and
            // saying it in one script only would make that an afterthought.
            Text(
                text = "Read English.",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Understand in ಕನ್ನಡ.",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!isComposing) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Tap any word for its meaning, a definition, and an example you can keep.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(if (isComposing) 24.dp else 32.dp))

            if (latestArticleTitle != null) {
                ContinueReadingBlock(
                    title = latestArticleTitle.ifBlank { "Untitled article" },
                    onClick = onContinueReading
                )
                Spacer(Modifier.height(32.dp))
            }

            // ── Start something new ────────────────────────────
            RuledLabel(text = "Start something new")
            Spacer(Modifier.height(14.dp))

            SourceTabs(selected = selectedTab, onSelect = { selectedTab = it })
            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                PasteTabContent(
                    article = article,
                    onArticleChange = onArticleChange,
                    isCleaning = isCleaning,
                    onLoadArticle = onLoadArticle,
                    onClearArticle = onClearArticle,
                    cleaningError = cleaningError,
                    onRetry = onRetry
                )
            } else {
                YouTubeTabContent(
                    url = youTubeUrl,
                    onUrlChange = onYouTubeUrlChange,
                    onImport = onImportYouTube,
                    isImporting = isImportingYouTube,
                    error = youTubeError,
                    onDismissError = onDismissYouTubeError
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

/**
 * The last article, offered as one object. This is the likeliest reason the app was
 * opened, so it sits above everything else and is the only thing on the page with a
 * surface of its own.
 */
@Composable
private fun ContinueReadingBlock(
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Continue reading",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** A label with a rule running out from it, so it sits on the page instead of floating alone. */
@Composable
private fun RuledLabel(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

/**
 * Source picker: two words with a rule under the live one. A tab drawn as a tab, rather
 * than two pill buttons competing with the real action below them.
 */
@Composable
private fun SourceTabs(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        SourceTab("Paste text", selected == 0) { onSelect(0) }
        SourceTab("YouTube", selected == 1) { onSelect(1) }
    }
}

@Composable
private fun SourceTab(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (isSelected) 26.dp else 0.dp)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                )
        )
    }
}

@Composable
private fun PasteTabContent(
    article: String,
    onArticleChange: (String) -> Unit,
    isCleaning: Boolean,
    onLoadArticle: () -> Unit,
    onClearArticle: () -> Unit,
    cleaningError: String?,
    onRetry: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var clipboardEmpty by remember { mutableStateOf(false) }
    val hasText = article.isNotBlank()

    Column {
        // The writing surface is recessed into the page rather than raised off it:
        // text goes in here, it is not a card sitting on top.
        OutlinedTextField(
            value = article,
            onValueChange = onArticleChange,
            // Empty, this is a prompt and should take a prompt's worth of room; the
            // old full-height box was a grey void that dominated the page before you
            // had done anything. It grows as the article goes in.
            modifier = Modifier
                .heightIn(min = if (hasText) 210.dp else 116.dp, max = 260.dp)
                .fillMaxWidth(),
            placeholder = {
                Text(
                    "Paste a full article — headline, body, the works.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(12.dp),
            minLines = if (hasText) 7 else 3,
            maxLines = 11,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        Spacer(Modifier.height(8.dp))

        if (hasText) {
            val wordCount = article.trim().split(Regex("\\s+")).size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (wordCount == 1) "1 word" else "$wordCount words",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClearArticle, enabled = !isCleaning) {
                    Text("Clear", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (cleaningError != null) {
            Spacer(Modifier.height(6.dp))
            ErrorNote(message = cleaningError, actionLabel = "Try again", onAction = onRetry)
        }

        Spacer(Modifier.height(14.dp))

        // The primary action is whatever can be done now, so the page never shows a dead
        // grey button waiting for input.
        if (hasText || isCleaning) {
            PrimaryAction(
                label = if (isCleaning) "Preparing the article" else "Open article",
                busy = isCleaning,
                onClick = onLoadArticle
            )
        } else {
            PrimaryAction(
                label = "Paste from clipboard",
                busy = false,
                iconRes = R.drawable.ic_newspaper,
                onClick = {
                    val copied = clipboard.getText()?.text.orEmpty()
                    clipboardEmpty = copied.isBlank()
                    if (copied.isNotBlank()) onArticleChange(copied)
                }
            )
            if (clipboardEmpty) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Nothing is copied yet. Copy an article in your browser, then come back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun YouTubeTabContent(
    url: String,
    onUrlChange: (String) -> Unit,
    onImport: () -> Unit,
    isImporting: Boolean,
    error: String?,
    onDismissError: () -> Unit
) {
    val canImport = url.isNotBlank() && !isImporting

    Column {
        OutlinedTextField(
            value = url,
            onValueChange = {
                onUrlChange(it)
                if (error != null) onDismissError()
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(
                    "youtube.com/watch?v=…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(onGo = { if (canImport) onImport() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = "The video's transcript becomes the article. A video with no captions can't be imported.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            ErrorNote(message = error, actionLabel = "Dismiss", onAction = onDismissError)
        }

        if (canImport || isImporting) {
            Spacer(Modifier.height(16.dp))
            PrimaryAction(
                label = if (isImporting) "Fetching the transcript" else "Import transcript",
                busy = isImporting,
                onClick = onImport
            )
        }
    }
}

@Composable
private fun PrimaryAction(
    label: String,
    busy: Boolean,
    onClick: () -> Unit,
    iconRes: Int? = null
) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(10.dp))
        } else if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * A failure says what happened and what to do about it. The container carries the error
 * colour; the message itself stays readable ink rather than shouting in red.
 */
@Composable
private fun ErrorNote(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = onAction) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
