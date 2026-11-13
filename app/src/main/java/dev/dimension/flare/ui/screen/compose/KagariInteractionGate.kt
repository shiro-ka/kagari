package dev.dimension.flare.ui.screen.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Globe
import compose.icons.fontawesomeicons.solid.Lock
import compose.icons.fontawesomeicons.solid.QuoteLeft
import compose.icons.fontawesomeicons.solid.Users
import dev.dimension.flare.R
import dev.dimension.flare.data.datasource.microblog.InteractionGate
import dev.dimension.flare.data.datasource.microblog.InteractionGateConfig
import dev.dimension.flare.data.datasource.microblog.InteractionGateList
import dev.dimension.flare.ui.component.FAIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class KagariInteractionGateState(
    private val configProvider: () -> InteractionGateConfig?,
    private val scope: CoroutineScope,
) {
    var gate by mutableStateOf(InteractionGate())
    var lists by mutableStateOf<List<InteractionGateList>?>(null)
        private set
    var showDialog by mutableStateOf(false)
        private set

    val enabled: Boolean
        get() = configProvider() != null

    fun composeValue(): InteractionGate? = if (enabled) gate else null

    fun toggleQuote() {
        gate = gate.copy(allowQuote = !gate.allowQuote)
    }

    fun openDialog() {
        val config = configProvider() ?: return
        showDialog = true
        if (lists == null) {
            scope.launch {
                lists = runCatching { config.loadLists() }.getOrDefault(emptyList())
            }
        }
    }

    fun dismissDialog() {
        showDialog = false
    }

    fun applyReply(
        reply: InteractionGate.Reply,
        saveAsDefault: Boolean,
    ) {
        gate = gate.copy(reply = reply)
        showDialog = false
        if (saveAsDefault) {
            val config = configProvider() ?: return
            val value = gate
            scope.launch {
                runCatching { config.saveDefault(value) }
            }
        }
    }
}

@Composable
internal fun rememberKagariInteractionGateState(config: InteractionGateConfig?): KagariInteractionGateState {
    val currentConfig by rememberUpdatedState(config)
    val scope = rememberCoroutineScope()
    val state = remember { KagariInteractionGateState({ currentConfig }, scope) }
    val hasConfig = config != null
    LaunchedEffect(hasConfig) {
        val value = currentConfig ?: return@LaunchedEffect
        runCatching { value.loadDefault() }.onSuccess { state.gate = it }
    }
    return state
}

@Composable
internal fun KagariReplyGateRow(
    state: KagariInteractionGateState,
    modifier: Modifier = Modifier,
) {
    if (!state.enabled) return
    val reply = state.gate.reply
    val restricted = reply !is InteractionGate.Reply.Anyone && !(reply is InteractionGate.Reply.Some && reply.isEmpty)
    val color =
        if (restricted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.kagari_gate_reply_title)) { state.openDialog() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FAIcon(
            imageVector =
                when (reply) {
                    InteractionGate.Reply.Nobody -> FontAwesomeIcons.Solid.Lock
                    is InteractionGate.Reply.Some -> if (reply.isEmpty) FontAwesomeIcons.Solid.Globe else FontAwesomeIcons.Solid.Users
                    InteractionGate.Reply.Anyone -> FontAwesomeIcons.Solid.Globe
                },
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = color,
        )
        Text(
            text = stringResource(R.string.kagari_gate_reply_label, replySummary(reply)),
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
    if (state.showDialog) {
        KagariGateDialog(
            initial = state.gate,
            lists = state.lists,
            showReply = true,
            showQuote = false,
            showSaveDefault = true,
            onDismiss = state::dismissDialog,
            onConfirm = { gate, saveAsDefault -> state.applyReply(gate.reply, saveAsDefault) },
        )
    }
}

@Composable
internal fun KagariQuoteToggleButton(state: KagariInteractionGateState) {
    if (!state.enabled) return
    val allowQuote = state.gate.allowQuote
    val tint =
        if (allowQuote) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.primary
        }
    val slashBackground = MaterialTheme.colorScheme.surface
    IconButton(onClick = state::toggleQuote) {
        FAIcon(
            imageVector = FontAwesomeIcons.Solid.QuoteLeft,
            contentDescription =
                stringResource(
                    if (allowQuote) R.string.kagari_gate_quote_allowed else R.string.kagari_gate_quote_disabled,
                ),
            tint = tint,
            modifier =
                Modifier.drawWithContent {
                    drawContent()
                    if (!allowQuote) {
                        drawLine(
                            color = slashBackground,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 5.dp.toPx(),
                        )
                        drawLine(
                            color = tint,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
        )
    }
}

@Composable
private fun replySummary(reply: InteractionGate.Reply): String =
    when (reply) {
        InteractionGate.Reply.Anyone -> stringResource(R.string.kagari_gate_anyone)
        InteractionGate.Reply.Nobody -> stringResource(R.string.kagari_gate_nobody)
        is InteractionGate.Reply.Some ->
            if (reply.isEmpty) {
                stringResource(R.string.kagari_gate_anyone)
            } else {
                buildList {
                    if (reply.followers) add(stringResource(R.string.kagari_gate_summary_followers))
                    if (reply.following) add(stringResource(R.string.kagari_gate_summary_following))
                    if (reply.mentioned) add(stringResource(R.string.kagari_gate_summary_mentioned))
                    if (reply.lists.isNotEmpty()) add(stringResource(R.string.kagari_gate_summary_lists, reply.lists.size))
                }.joinToString(stringResource(R.string.kagari_gate_summary_separator))
            }
    }

@Composable
internal fun KagariGateDialog(
    initial: InteractionGate,
    lists: List<InteractionGateList>?,
    showReply: Boolean,
    showQuote: Boolean,
    showSaveDefault: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (InteractionGate, Boolean) -> Unit,
    saving: Boolean = false,
) {
    var reply by remember { mutableStateOf(initial.reply) }
    var allowQuote by remember { mutableStateOf(initial.allowQuote) }
    var saveAsDefault by remember { mutableStateOf(false) }
    var showLists by remember { mutableStateOf(false) }
    val some = reply as? InteractionGate.Reply.Some ?: InteractionGate.Reply.Some()
    val ruleCount =
        listOf(some.followers, some.following, some.mentioned).count { it } + some.lists.size
    val full = ruleCount >= InteractionGate.MAX_RULES

    fun update(next: InteractionGate.Reply.Some) {
        reply = if (next.isEmpty) InteractionGate.Reply.Anyone else next
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(if (showQuote) R.string.kagari_gate_edit_title else R.string.kagari_gate_reply_title),
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (showReply) {
                if (showQuote) {
                    Text(
                        text = stringResource(R.string.kagari_gate_reply_title),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = reply is InteractionGate.Reply.Anyone,
                        onClick = { reply = InteractionGate.Reply.Anyone },
                        label = { Text(stringResource(R.string.kagari_gate_anyone)) },
                    )
                    FilterChip(
                        selected = reply is InteractionGate.Reply.Nobody,
                        onClick = { reply = InteractionGate.Reply.Nobody },
                        label = { Text(stringResource(R.string.kagari_gate_nobody)) },
                    )
                }
                GateCheckRow(
                    label = stringResource(R.string.kagari_gate_followers),
                    checked = some.followers,
                    enabled = some.followers || !full,
                    onCheckedChange = { update(some.copy(followers = it)) },
                )
                GateCheckRow(
                    label = stringResource(R.string.kagari_gate_following),
                    checked = some.following,
                    enabled = some.following || !full,
                    onCheckedChange = { update(some.copy(following = it)) },
                )
                GateCheckRow(
                    label = stringResource(R.string.kagari_gate_mentioned),
                    checked = some.mentioned,
                    enabled = some.mentioned || !full,
                    onCheckedChange = { update(some.copy(mentioned = it)) },
                )
                TextButton(onClick = { showLists = !showLists }) {
                    Text(stringResource(R.string.kagari_gate_lists, some.lists.size))
                }
                if (showLists) {
                    when {
                        lists == null -> CircularProgressIndicator(modifier = Modifier.padding(8.dp).size(24.dp))
                        lists.isEmpty() ->
                            Text(
                                text = stringResource(R.string.kagari_gate_lists_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        else ->
                            lists.forEach { item ->
                                val checked = item.uri in some.lists
                                GateCheckRow(
                                    label = item.name,
                                    checked = checked,
                                    enabled = checked || !full,
                                    onCheckedChange = { value ->
                                        update(
                                            some.copy(
                                                lists = if (value) some.lists + item.uri else some.lists - item.uri,
                                            ),
                                        )
                                    },
                                )
                            }
                    }
                }
                }
                if (showQuote) {
                    if (showReply) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    GateCheckRow(
                        label = stringResource(R.string.kagari_gate_quote_allow),
                        checked = allowQuote,
                        enabled = true,
                        onCheckedChange = { allowQuote = it },
                    )
                }
                if (showSaveDefault) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    GateCheckRow(
                        label = stringResource(R.string.kagari_gate_save_default),
                        checked = saveAsDefault,
                        enabled = true,
                        onCheckedChange = { saveAsDefault = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = { onConfirm(InteractionGate(reply = reply, allowQuote = allowQuote), saveAsDefault) },
            ) {
                Text(stringResource(R.string.kagari_gate_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun GateCheckRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onValueChange = onCheckedChange,
                ).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
