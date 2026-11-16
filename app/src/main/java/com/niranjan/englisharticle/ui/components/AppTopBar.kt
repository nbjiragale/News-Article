package com.niranjan.englisharticle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niranjan.englisharticle.R

/**
 * The page's chrome. It stays quiet: the article is the loud thing.
 *
 * A hairline rule closes the bar off from the page below — the one structural
 * device here, and it earns its place by marking where the app stops and the
 * content starts.
 */
@Composable
fun AppTopBar(
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    onRecentsClick: (() -> Unit)? = null,
    onSavedWordsClick: (() -> Unit)? = null,
    onPracticeClick: (() -> Unit)? = null,
    showRule: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack && onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Brandmark(modifier = Modifier.padding(start = 8.dp))
            }

            Box(modifier = Modifier.weight(1f))

            if (onRecentsClick != null) {
                BarIcon(R.drawable.ic_history, "Reading history", onRecentsClick)
            }
            if (onSavedWordsClick != null) {
                BarIcon(R.drawable.ic_bookmark, "Saved words", onSavedWordsClick)
            }
            if (onPracticeClick != null) {
                BarIcon(R.drawable.ic_school, "Practice", onPracticeClick)
            }
        }

        if (showRule) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

/**
 * ಅ — the first letter of ಅರ್ಥ (artha, "meaning"). The app is bilingual, so its
 * mark is too, and the Kannada letter does the work a generic glyph would not.
 */
@Composable
fun Brandmark(
    modifier: Modifier = Modifier,
    showWordmark: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ಅ",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (showWordmark) {
            Text(
                text = "ArthaReader",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BarIcon(iconRes: Int, label: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
fun BottomSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}
