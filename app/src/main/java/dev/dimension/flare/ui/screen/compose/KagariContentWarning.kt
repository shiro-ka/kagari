package dev.dimension.flare.ui.screen.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Shield
import dev.dimension.flare.R
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.presenter.compose.ComposeStatus

private val kagariAdultLabels = listOf("sexual", "nudity", "porn")
private const val KAGARI_GRAPHIC_LABEL = "graphic-media"
private val kagariLinkPattern = Regex("https?://\\S")

internal fun kagariHasLinkCard(
    text: CharSequence,
    hasMedia: Boolean,
    status: ComposeStatus?,
): Boolean = !hasMedia && status !is ComposeStatus.Quote && kagariLinkPattern.containsMatchIn(text)

internal class KagariSelfLabelState {
    var labels by mutableStateOf(emptyList<String>())
    var showDialog by mutableStateOf(false)

    fun composeValue(enabled: Boolean): List<String> = if (enabled) labels else emptyList()

    fun selectAdult(label: String) {
        val others = labels.filterNot { it in kagariAdultLabels }
        labels = if (label in labels) others else others + label
    }

    fun toggleGraphic() {
        labels =
            if (KAGARI_GRAPHIC_LABEL in labels) {
                labels - KAGARI_GRAPHIC_LABEL
            } else {
                labels + KAGARI_GRAPHIC_LABEL
            }
    }
}

@Composable
internal fun rememberKagariSelfLabelState(): KagariSelfLabelState = remember { KagariSelfLabelState() }

@Composable
private fun kagariLabelName(label: String): String =
    when (label) {
        "sexual" -> stringResource(R.string.kagari_cw_sexual)
        "nudity" -> stringResource(R.string.kagari_cw_nudity)
        "porn" -> stringResource(R.string.kagari_cw_porn)
        else -> stringResource(R.string.kagari_cw_graphic)
    }

@Composable
private fun kagariLabelDescription(label: String): String =
    when (label) {
        "sexual" -> stringResource(R.string.kagari_cw_sexual_description)
        "nudity" -> stringResource(R.string.kagari_cw_nudity_description)
        "porn" -> stringResource(R.string.kagari_cw_porn_description)
        else -> stringResource(R.string.kagari_cw_graphic_description)
    }

@Composable
internal fun KagariContentWarningRow(
    state: KagariSelfLabelState,
    modifier: Modifier = Modifier,
) {
    val selected = state.labels.isNotEmpty()
    val separator = stringResource(R.string.kagari_gate_summary_separator)
    val summary =
        if (selected) {
            state.labels.map { kagariLabelName(it) }.joinToString(separator)
        } else {
            stringResource(R.string.kagari_cw_none)
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClickLabel = stringResource(R.string.kagari_cw_title)) { state.showDialog = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FAIcon(
            imageVector = FontAwesomeIcons.Solid.Shield,
            contentDescription = null,
        )
        Text(text = stringResource(R.string.kagari_cw_title))
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = summary,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (state.showDialog) {
        KagariContentWarningDialog(state)
    }
}

@Composable
private fun KagariLabelCheckbox(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onClick() }),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Text(text = kagariLabelName(label), modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp))
    }
}

@Composable
private fun KagariContentWarningDialog(state: KagariSelfLabelState) {
    AlertDialog(
        onDismissRequest = { state.showDialog = false },
        title = { Text(stringResource(R.string.kagari_cw_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.kagari_cw_dialog_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.kagari_cw_adult),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                kagariAdultLabels.forEach { label ->
                    KagariLabelCheckbox(label, label in state.labels) { state.selectAdult(label) }
                }
                state.labels.firstOrNull { it in kagariAdultLabels }?.let {
                    Text(
                        text = kagariLabelDescription(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.kagari_cw_other),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp),
                )
                KagariLabelCheckbox(KAGARI_GRAPHIC_LABEL, KAGARI_GRAPHIC_LABEL in state.labels) { state.toggleGraphic() }
                if (KAGARI_GRAPHIC_LABEL in state.labels) {
                    Text(
                        text = kagariLabelDescription(KAGARI_GRAPHIC_LABEL),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { state.showDialog = false }) {
                Text(stringResource(R.string.kagari_cw_done))
            }
        },
    )
}
