package dev.dimension.flare.ui.component.status

import androidx.compose.runtime.Composable
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_menu_gate_anyone
import dev.dimension.flare.compose.ui.kagari_menu_gate_followers
import dev.dimension.flare.compose.ui.kagari_menu_gate_following
import dev.dimension.flare.compose.ui.kagari_menu_gate_lists
import dev.dimension.flare.compose.ui.kagari_menu_gate_mentioned
import dev.dimension.flare.compose.ui.kagari_menu_gate_nobody
import dev.dimension.flare.compose.ui.kagari_menu_gate_quote
import dev.dimension.flare.compose.ui.kagari_menu_gate_quote_allowed
import dev.dimension.flare.compose.ui.kagari_menu_gate_quote_disabled
import dev.dimension.flare.compose.ui.kagari_menu_gate_reply
import dev.dimension.flare.compose.ui.kagari_menu_gate_separator
import dev.dimension.flare.data.datasource.microblog.ActionMenu
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun kagariGateMenuText(
    type: ActionMenu.Item.Text.Localized.Type,
    parameters: List<String>,
): String =
    when (type) {
        ActionMenu.Item.Text.Localized.Type.KagariQuoteGate ->
            stringResource(
                Res.string.kagari_menu_gate_quote,
                stringResource(
                    if (parameters.firstOrNull() == "disabled") {
                        Res.string.kagari_menu_gate_quote_disabled
                    } else {
                        Res.string.kagari_menu_gate_quote_allowed
                    },
                ),
            )
        else -> {
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
                    }.ifEmpty { listOf(stringResource(Res.string.kagari_menu_gate_anyone)) }
                    .joinToString(separator)
            stringResource(Res.string.kagari_menu_gate_reply, summary)
        }
    }
