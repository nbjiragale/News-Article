package com.niranjan.englisharticle.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun HighlightedSentence(
    text: String,
    word: String,
    style: TextStyle,
    color: Color
) {
    val startIndex = text.indexOf(word, ignoreCase = true)
    val annotated = buildAnnotatedString {
        if (startIndex == -1 || word.isBlank()) {
            append(text)
        } else {
            append(text.take(startIndex))
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                append(text.substring(startIndex, startIndex + word.length))
            }
            append(text.drop(startIndex + word.length))
        }
    }
    Text(text = annotated, style = style, color = color)
}
