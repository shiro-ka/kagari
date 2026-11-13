package dev.dimension.flare.ui.component.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_menu_gate_anyone
import dev.dimension.flare.compose.ui.kagari_menu_gate_followers
import dev.dimension.flare.compose.ui.kagari_menu_gate_following
import dev.dimension.flare.compose.ui.kagari_menu_gate_lists
import dev.dimension.flare.compose.ui.kagari_menu_gate_mentioned
import dev.dimension.flare.compose.ui.kagari_menu_gate_nobody
import dev.dimension.flare.compose.ui.kagari_menu_gate_reply
import dev.dimension.flare.compose.ui.kagari_menu_gate_separator
import dev.dimension.flare.compose.ui.kagari_menu_gate_title
import dev.dimension.flare.data.datasource.microblog.ActionMenu
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.theme.PlatformTheme
import org.jetbrains.compose.resources.stringResource

internal fun ActionMenu.Item.Text.isKagariGate(): Boolean =
    this is ActionMenu.Item.Text.Localized && type == ActionMenu.Item.Text.Localized.Type.KagariReplyGate

@Composable
internal fun KagariGateMenuItemText(
    text: ActionMenu.Item.Text,
    color: Color,
) {
    val parameters = (text as? ActionMenu.Item.Text.Localized)?.parameters.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        PlatformText(
            text = stringResource(Res.string.kagari_menu_gate_title),
            color = color,
        )
        kagariReplySummary(parameters)?.let {
            PlatformText(
                text = it,
                style = PlatformTheme.typography.caption,
                color = PlatformTheme.colorScheme.caption,
            )
        }
    }
}

@Composable
internal fun kagariGateMenuText(
    type: ActionMenu.Item.Text.Localized.Type,
    parameters: List<String>,
): String {
    val title = stringResource(Res.string.kagari_menu_gate_title)
    val summary = kagariReplySummary(parameters)
    return if (summary == null) title else "$title $summary"
}

@Composable
private fun kagariReplySummary(parameters: List<String>): String? {
    if (parameters.isEmpty() || parameters == listOf("reply-post")) return null
    val separator = stringResource(Res.string.kagari_menu_gate_separator)
    val summary =
        parameters
            .map { token ->
                when {
                    token == "nobody" -> stringResource(Res.string.kagari_menu_gate_nobody)
                    token == "followers" -> stringResource(Res.string.kagari_menu_gate_followers)
                    token == "following" -> stringResource(Res.string.kagari_menu_gate_following)
                    token == "mentioned" -> stringResource(Res.string.kagari_menu_gate_mentioned)
                    token.startsWith("lists:") ->
                        stringResource(Res.string.kagari_menu_gate_lists, token.removePrefix("lists:"))
                    else -> stringResource(Res.string.kagari_menu_gate_anyone)
                }
            }.joinToString(separator)
    return stringResource(Res.string.kagari_menu_gate_reply, summary)
}
