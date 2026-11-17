package dev.dimension.flare.ui.screen.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dimension.flare.data.datasource.microblog.KagariMentionSearch
import dev.dimension.flare.ui.component.AvatarComponent
import dev.dimension.flare.ui.model.UiProfile
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import kotlinx.coroutines.delay

internal data class KagariMentionQuery(
    val text: String,
    val start: Int,
    val end: Int,
)

internal fun kagariMentionQuery(
    text: CharSequence,
    selection: TextRange,
): KagariMentionQuery? {
    if (!selection.collapsed) return null
    val end = selection.start
    if (end > text.length) return null
    var index = end - 1
    while (index >= 0 && text[index].isKagariHandleChar()) {
        index--
    }
    if (index < 0 || text[index] != '@') return null
    if (index > 0 && !text[index - 1].isWhitespace()) return null
    val query = text.substring(index + 1, end)
    if (query.isEmpty()) return null
    return KagariMentionQuery(text = query, start = index, end = end)
}

private fun Char.isKagariHandleChar(): Boolean = code < 128 && (isLetterOrDigit() || this == '.' || this == '-' || this == '_')

@Composable
internal fun KagariMentionSuggestions(
    textFieldState: TextFieldState,
    search: KagariMentionSearch?,
) {
    if (search == null) return
    val query = kagariMentionQuery(textFieldState.text, textFieldState.selection)
    var results by remember { mutableStateOf<List<UiProfile>>(emptyList()) }
    LaunchedEffect(query?.text) {
        results = emptyList()
        val text = query?.text ?: return@LaunchedEffect
        delay(250)
        results = runCatching { search.search(text) }.getOrDefault(emptyList())
    }
    if (query == null || results.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider()
        results.forEach { profile ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val current = kagariMentionQuery(textFieldState.text, textFieldState.selection) ?: return@clickable
                            val inserted = "@${profile.handle.raw} "
                            textFieldState.edit {
                                replace(current.start, current.end, inserted)
                                selection = TextRange(current.start + inserted.length)
                            }
                            results = emptyList()
                        }.padding(horizontal = screenHorizontalPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarComponent(data = profile.avatar, size = 36.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.name.raw,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = profile.handle.display,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        HorizontalDivider()
    }
}
