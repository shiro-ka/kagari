package dev.dimension.flare.ui.component.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_embed_separator
import dev.dimension.flare.compose.ui.kagari_mado_answer
import dev.dimension.flare.compose.ui.kagari_mado_closed
import dev.dimension.flare.compose.ui.kagari_mado_name
import dev.dimension.flare.compose.ui.kagari_mado_open
import dev.dimension.flare.compose.ui.kagari_mado_send_voice
import dev.dimension.flare.compose.ui.kagari_mado_voice
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.component.DateTimeText
import dev.dimension.flare.ui.component.FlareDividerDefaults
import dev.dimension.flare.ui.component.platform.PlatformFilledTonalButton
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.KagariMadoBox
import dev.dimension.flare.ui.model.KagariMadoVoice
import dev.dimension.flare.ui.model.takeSuccess
import dev.dimension.flare.ui.presenter.KagariMadoVoicePresenter
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.render.UiDateTime
import dev.dimension.flare.ui.route.DeeplinkRoute
import dev.dimension.flare.ui.route.toUri
import dev.dimension.flare.ui.theme.PlatformTheme
import moe.tlaster.precompose.molecule.producePresenter
import org.jetbrains.compose.resources.stringResource

private val kagariMadoBlue = Color(0xFF4B8EF6)
private val kagariMadoBlueDark = Color(0xFF7DAEF9)

@Composable
private fun kagariMadoAccent(): Color =
    if (PlatformTheme.colorScheme.background.luminance() < 0.5f) {
        kagariMadoBlueDark
    } else {
        kagariMadoBlue
    }

@Composable
internal fun kagariRememberMadoVoice(
    accountType: AccountType,
    embed: KagariEmbed?,
): KagariMadoVoice? {
    val mado = embed as? KagariEmbed.MadoVoice ?: return null
    val state by producePresenter(key = "kagari_mado_${accountType}_${mado.url}") {
        remember(accountType, mado) { KagariMadoVoicePresenter(accountType, mado) }.invoke()
    }
    return state.takeSuccess()
}

@Composable
internal fun KagariMadoEmbedCard(
    voice: KagariMadoVoice,
    modifier: Modifier = Modifier,
) {
    val separator = stringResource(Res.string.kagari_embed_separator)
    KagariMadoVoiceCard(
        voice = voice,
        footer = stringResource(Res.string.kagari_mado_name) + separator + voice.boxTitle,
        date = null,
        modifier = modifier,
    )
}

@Composable
public fun KagariMadoVoiceCard(
    voice: KagariMadoVoice,
    footer: String,
    date: UiDateTime?,
    modifier: Modifier = Modifier,
) {
    val accent = kagariMadoAccent()
    val uriHandler = LocalUriHandler.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    FlareDividerDefaults.thickness,
                    color = FlareDividerDefaults.color,
                    shape = PlatformTheme.shapes.medium,
                ).clip(PlatformTheme.shapes.medium)
                .clickable { uriHandler.openUri(kagariMadoBrowserUri(voice.url)) }
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PlatformText(
            text = stringResource(Res.string.kagari_mado_voice),
            style = PlatformTheme.typography.caption,
            color = accent,
            fontWeight = FontWeight.Bold,
        )
        PlatformText(text = voice.body)
        voice.answers.forEach { answer ->
            PlatformText(
                text = stringResource(Res.string.kagari_mado_answer),
                style = PlatformTheme.typography.caption,
                color = accent,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
            PlatformText(text = answer.body)
        }
        Row(
            modifier = Modifier.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlatformText(
                text = footer,
                style = PlatformTheme.typography.caption,
                color = PlatformTheme.colorScheme.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (date != null) {
                PlatformText(
                    text = stringResource(Res.string.kagari_embed_separator),
                    style = PlatformTheme.typography.caption,
                    color = PlatformTheme.colorScheme.caption,
                )
                DateTimeText(
                    data = date,
                    style = PlatformTheme.typography.caption,
                    color = PlatformTheme.colorScheme.caption,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
public fun KagariMadoBoxCard(
    box: KagariMadoBox,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    FlareDividerDefaults.thickness,
                    color = FlareDividerDefaults.color,
                    shape = PlatformTheme.shapes.medium,
                ).clip(PlatformTheme.shapes.medium)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlatformText(
                text = box.title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            PlatformText(
                text = stringResource(if (box.isOpen) Res.string.kagari_mado_open else Res.string.kagari_mado_closed),
                style = PlatformTheme.typography.caption,
                color = PlatformTheme.colorScheme.caption,
                modifier =
                    Modifier
                        .clip(PlatformTheme.shapes.small)
                        .background(PlatformTheme.colorScheme.cardAlt)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        val description = box.description
        if (description != null) {
            PlatformText(
                text = description,
                style = PlatformTheme.typography.caption,
                color = PlatformTheme.colorScheme.caption,
            )
        }
        if (box.isOpen) {
            PlatformFilledTonalButton(
                onClick = { uriHandler.openUri(kagariMadoBrowserUri(box.url)) },
            ) {
                PlatformText(text = stringResource(Res.string.kagari_mado_send_voice))
            }
        }
    }
}

private fun kagariMadoBrowserUri(url: String): String = DeeplinkRoute.OpenLinkDirectly(url).toUri()
