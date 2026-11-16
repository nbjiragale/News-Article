package com.niranjan.englisharticle.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.R
import com.niranjan.englisharticle.ui.tts.ArticlePlaybackState

/**
 * Reading aloud, offered from the page's bottom corner.
 *
 * The label changes with the state and names what pressing it will do, so the control
 * reads the same way in every state: "Listen" starts, "Pause" pauses, "Resume" resumes.
 * Once playing it steps back to a quiet surface — the article is the thing being
 * attended to, not the button.
 */
@Composable
fun ArticleListenButton(
    state: ArticlePlaybackState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (iconRes, label) = when (state) {
        ArticlePlaybackState.Idle -> R.drawable.ic_volume_2 to "Listen"
        ArticlePlaybackState.Playing -> R.drawable.ic_pause to "Pause"
        ArticlePlaybackState.Paused -> R.drawable.ic_play to "Resume"
    }
    val isPlaying = state == ArticlePlaybackState.Playing
    val containerColor = if (isPlaying) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.primary
    }
    val contentColor = if (isPlaying) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Button(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 13.dp),
        modifier = modifier
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(9.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}
