package dev.dimension.flare.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Trash
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.bluesky.KagariMuteWord
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.BackButton
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.profile.KagariMuteWordsPresenter
import moe.tlaster.precompose.molecule.producePresenter
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private enum class KagariMuteDuration(
    val duration: Duration?,
    val label: Int,
) {
    Forever(null, R.string.kagari_mute_words_forever),
    Day(24.hours, R.string.kagari_mute_words_24h),
    Week(7.days, R.string.kagari_mute_words_7d),
    Month(30.days, R.string.kagari_mute_words_30d),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KagariMuteWordsScreen(
    accountKey: MicroBlogKey,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by producePresenter(key = "KagariMuteWords_$accountKey") {
        remember(accountKey) { KagariMuteWordsPresenter(accountKey) }.invoke()
    }
    var input by remember { mutableStateOf("") }
    var tagOnly by remember { mutableStateOf(false) }
    var duration by remember { mutableStateOf(KagariMuteDuration.Forever) }
    var excludeFollowing by remember { mutableStateOf(false) }
    val failed = stringResource(R.string.kagari_mute_words_failed)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.kagari_mute_words_title)) },
                navigationIcon = { BackButton(onBack = onBack) },
            )
        },
    ) { padding ->
        when (val words = state.words) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = failed)
                }
            }

            is UiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                label = { Text(text = stringResource(R.string.kagari_mute_words_input)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(text = stringResource(R.string.kagari_mute_words_target), style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = !tagOnly,
                                    onClick = { tagOnly = false },
                                    label = { Text(text = stringResource(R.string.kagari_mute_words_content_and_tags)) },
                                )
                                FilterChip(
                                    selected = tagOnly,
                                    onClick = { tagOnly = true },
                                    label = { Text(text = stringResource(R.string.kagari_mute_words_tags_only)) },
                                )
                            }
                            Text(text = stringResource(R.string.kagari_mute_words_duration), style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                KagariMuteDuration.entries.forEach { item ->
                                    FilterChip(
                                        selected = duration == item,
                                        onClick = { duration = item },
                                        label = { Text(text = stringResource(item.label)) },
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = excludeFollowing, onCheckedChange = { excludeFollowing = it })
                                Text(text = stringResource(R.string.kagari_mute_words_exclude_following))
                            }
                            Button(
                                enabled = input.isNotBlank() && !state.saving,
                                onClick = {
                                    val word =
                                        KagariMuteWord(
                                            id = null,
                                            value = input.trim(),
                                            tagOnly = tagOnly,
                                            excludeFollowing = excludeFollowing,
                                            expiresAt = duration.duration?.let { Clock.System.now() + it },
                                        )
                                    state.add(word) { ok ->
                                        if (ok) {
                                            input = ""
                                        } else {
                                            Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text(text = stringResource(R.string.kagari_mute_words_add))
                            }
                        }
                        HorizontalDivider()
                    }
                    if (words.data.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.kagari_mute_words_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    items(words.data) { word ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = word.value, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = kagariMuteWordSummary(word),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                enabled = !state.saving,
                                onClick = {
                                    state.remove(word) { ok ->
                                        if (!ok) Toast.makeText(context, failed, Toast.LENGTH_SHORT).show()
                                    }
                                },
                            ) {
                                FAIcon(imageVector = FontAwesomeIcons.Solid.Trash, contentDescription = null)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun kagariMuteWordSummary(word: KagariMuteWord): String {
    val parts = mutableListOf<String>()
    parts +=
        stringResource(
            if (word.tagOnly) R.string.kagari_mute_words_tags_only else R.string.kagari_mute_words_content_and_tags,
        )
    val expiresAt = word.expiresAt
    if (expiresAt == null) {
        parts += stringResource(R.string.kagari_mute_words_forever)
    } else {
        val left = expiresAt - Clock.System.now()
        parts +=
            if (left.isNegative()) {
                stringResource(R.string.kagari_mute_words_expired)
            } else if (left.inWholeDays >= 1) {
                stringResource(R.string.kagari_mute_words_days_left, left.inWholeDays.toInt())
            } else {
                stringResource(R.string.kagari_mute_words_hours_left, left.inWholeHours.toInt().coerceAtLeast(1))
            }
    }
    if (word.excludeFollowing) parts += stringResource(R.string.kagari_mute_words_exclude_following)
    return parts.joinToString("・")
}
