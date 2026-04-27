package com.niranjan.englisharticle.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.niranjan.englisharticle.R
import com.niranjan.englisharticle.ui.tts.ArticlePlaybackState

@Composable
fun ArticleListenButton(
    state: ArticlePlaybackState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (iconRes, label) = when (state) {
        ArticlePlaybackState.Idle -> R.drawable.ic_volume_2 to "Listen to article"
        ArticlePlaybackState.Playing -> R.drawable.ic_pause to "Pause"
        ArticlePlaybackState.Paused -> R.drawable.ic_play to "Resume"
    }
    val containerColor = when (state) {
        ArticlePlaybackState.Idle -> MaterialTheme.colorScheme.primaryContainer
        ArticlePlaybackState.Playing -> MaterialTheme.colorScheme.tertiaryContainer
        ArticlePlaybackState.Paused -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = when (state) {
        ArticlePlaybackState.Idle -> MaterialTheme.colorScheme.onPrimaryContainer
        ArticlePlaybackState.Playing -> MaterialTheme.colorScheme.onTertiaryContainer
        ArticlePlaybackState.Paused -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Button(
        onClick = onToggle,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
