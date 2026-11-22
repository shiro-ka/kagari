package dev.dimension.flare.ui.screen.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.SquarePollHorizontal
import compose.icons.fontawesomeicons.solid.Xmark
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.microblog.ComposeData
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.theme.screenHorizontalPadding
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private const val KAGARI_TOKIMEKI_MIN_OPTIONS = 2
private const val KAGARI_TOKIMEKI_MAX_OPTIONS = 4

internal enum class KagariTokimekiPollDuration(
    val textId: Int,
    val duration: Duration,
) {
    Hours1(R.string.kagari_tokimeki_compose_1_hour, 1.hours),
    Hours6(R.string.kagari_tokimeki_compose_6_hours, 6.hours),
    Hours12(R.string.kagari_tokimeki_compose_12_hours, 12.hours),
    Days1(R.string.kagari_tokimeki_compose_1_day, 1.days),
    Days3(R.string.kagari_tokimeki_compose_3_days, 3.days),
    Days7(R.string.kagari_tokimeki_compose_7_days, 7.days),
    ;

    companion object {
        fun fromMillis(millis: Long): KagariTokimekiPollDuration =
            entries.firstOrNull { it.duration.inWholeMilliseconds == millis } ?: Days1
    }
}

@Immutable
internal data class KagariTokimekiPollDraft(
    val options: List<String>,
    val duration: KagariTokimekiPollDuration,
)

@Stable
internal class KagariTokimekiPollComposeState {
    var poll by mutableStateOf<KagariTokimekiPollDraft?>(null)
    var editing by mutableStateOf(false)

    fun restore(value: ComposeData.KagariTokimekiPoll?) {
        poll =
            value?.let {
                KagariTokimekiPollDraft(
                    options = it.options,
                    duration = KagariTokimekiPollDuration.fromMillis(it.durationMillis),
                )
            }
    }

    fun composeValue(
        enabled: Boolean,
        label: String,
    ): ComposeData.KagariTokimekiPoll? =
        poll?.takeIf { enabled }?.let {
            ComposeData.KagariTokimekiPoll(
                options = it.options,
                durationMillis = it.duration.duration.inWholeMilliseconds,
                label = label,
            )
        }
}

@Composable
internal fun rememberKagariTokimekiPollState(): KagariTokimekiPollComposeState = remember { KagariTokimekiPollComposeState() }

@Composable
internal fun KagariTokimekiPollButton(state: KagariTokimekiPollComposeState) {
    IconButton(onClick = { state.editing = true }) {
        FAIcon(
            imageVector = FontAwesomeIcons.Solid.SquarePollHorizontal,
            contentDescription = stringResource(R.string.kagari_tokimeki_compose_add_poll),
        )
    }
}

@Composable
internal fun KagariTokimekiPollPreview(
    state: KagariTokimekiPollComposeState,
    hasMedia: Boolean,
) {
    val poll = state.poll ?: return
    Column(
        modifier =
            Modifier
                .padding(horizontal = screenHorizontalPadding)
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { state.editing = true }
                    .padding(start = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.kagari_tokimeki_compose_poll),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { state.editing = true }) {
                    Text(stringResource(R.string.kagari_tokimeki_compose_edit))
                }
                IconButton(onClick = { state.poll = null }) {
                    FAIcon(
                        imageVector = FontAwesomeIcons.Solid.Xmark,
                        contentDescription = stringResource(R.string.compose_disable_poll),
                    )
                }
            }
            poll.options.forEach { option ->
                Text(
                    text = option,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .padding(end = 8.dp)
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
            Text(
                text =
                    stringResource(
                        R.string.kagari_tokimeki_compose_duration_value,
                        stringResource(poll.duration.textId),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasMedia) {
            Text(
                text = stringResource(R.string.kagari_tokimeki_compose_media_conflict),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
internal fun KagariTokimekiPollDialog(state: KagariTokimekiPollComposeState) {
    if (!state.editing) {
        return
    }
    val initial = state.poll
    val options =
        remember {
            mutableStateListOf<String>().apply {
                addAll(initial?.options.orEmpty())
                while (size < KAGARI_TOKIMEKI_MIN_OPTIONS) {
                    add("")
                }
            }
        }
    var duration by remember { mutableStateOf(initial?.duration ?: KagariTokimekiPollDuration.Days1) }
    val trimmed = options.map { it.trim() }.filter { it.isNotEmpty() }
    AlertDialog(
        onDismissRequest = { state.editing = false },
        title = { Text(stringResource(R.string.kagari_tokimeki_compose_add_poll)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { options[index] = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(stringResource(R.string.kagari_tokimeki_compose_option, index + 1))
                        },
                        trailingIcon =
                            if (index >= KAGARI_TOKIMEKI_MIN_OPTIONS) {
                                {
                                    IconButton(onClick = { options.removeAt(index) }) {
                                        FAIcon(
                                            imageVector = FontAwesomeIcons.Solid.Xmark,
                                            contentDescription = stringResource(R.string.compose_remove_poll_option, index + 1),
                                        )
                                    }
                                }
                            } else {
                                null
                            },
                    )
                }
                if (options.size < KAGARI_TOKIMEKI_MAX_OPTIONS) {
                    TextButton(onClick = { options.add("") }) {
                        FAIcon(
                            imageVector = FontAwesomeIcons.Solid.Plus,
                            contentDescription = null,
                        )
                        Text(
                            text = stringResource(R.string.kagari_tokimeki_compose_add_option),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.kagari_tokimeki_compose_duration),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KagariTokimekiPollDuration.entries.forEach { entry ->
                        FilterChip(
                            selected = duration == entry,
                            onClick = { duration = entry },
                            label = { Text(stringResource(entry.textId)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    state.poll = KagariTokimekiPollDraft(options = trimmed, duration = duration)
                    state.editing = false
                },
                enabled = trimmed.size >= KAGARI_TOKIMEKI_MIN_OPTIONS,
            ) {
                Text(stringResource(R.string.kagari_tokimeki_compose_done))
            }
        },
        dismissButton = {
            TextButton(onClick = { state.editing = false }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}
