package dev.dimension.flare.ui.component.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_embed_blocked
import dev.dimension.flare.compose.ui.kagari_embed_deleted
import dev.dimension.flare.compose.ui.kagari_embed_feed
import dev.dimension.flare.compose.ui.kagari_embed_labeler
import dev.dimension.flare.compose.ui.kagari_embed_likes
import dev.dimension.flare.compose.ui.kagari_embed_moderation_list
import dev.dimension.flare.compose.ui.kagari_embed_removed_by_author
import dev.dimension.flare.compose.ui.kagari_embed_removed_by_you
import dev.dimension.flare.compose.ui.kagari_embed_separator
import dev.dimension.flare.compose.ui.kagari_embed_starter_pack
import dev.dimension.flare.compose.ui.kagari_embed_user_list
import dev.dimension.flare.ui.component.FlareDividerDefaults
import dev.dimension.flare.ui.component.NetworkImage
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.KagariMadoVoice
import dev.dimension.flare.ui.model.KagariTokimekiPoll
import dev.dimension.flare.ui.theme.PlatformTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun KagariEmbedComponent(
    embed: KagariEmbed,
    modifier: Modifier = Modifier,
    madoVoice: KagariMadoVoice? = null,
    tokimekiPoll: KagariTokimekiPoll? = null,
) {
    when (embed) {
        is KagariEmbed.Card -> KagariEmbedCard(embed, modifier)
        is KagariEmbed.Unavailable -> KagariEmbedUnavailable(embed, modifier)
        is KagariEmbed.MadoVoice -> madoVoice?.let { KagariMadoEmbedCard(it, modifier) }
        is KagariEmbed.TokimekiPoll -> tokimekiPoll?.let { KagariTokimekiPollCard(it, modifier) }
    }
}

@Composable
private fun KagariEmbedCard(
    card: KagariEmbed.Card,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val separator = stringResource(Res.string.kagari_embed_separator)
    val kind =
        stringResource(
            when (card.kind) {
                KagariEmbed.Kind.Feed -> Res.string.kagari_embed_feed
                KagariEmbed.Kind.UserList -> Res.string.kagari_embed_user_list
                KagariEmbed.Kind.ModerationList -> Res.string.kagari_embed_moderation_list
                KagariEmbed.Kind.StarterPack -> Res.string.kagari_embed_starter_pack
                KagariEmbed.Kind.Labeler -> Res.string.kagari_embed_labeler
            },
        )
    val likes = card.likeCount?.let { stringResource(Res.string.kagari_embed_likes, it.toString()) }
    val subtitle = listOfNotNull(kind, "@${card.creatorHandle}", likes).joinToString(separator)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    FlareDividerDefaults.thickness,
                    color = FlareDividerDefaults.color,
                    shape = PlatformTheme.shapes.medium,
                ).clip(PlatformTheme.shapes.medium)
                .clickable { uriHandler.openUri(card.url) }
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val avatar = card.avatar
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(PlatformTheme.shapes.small)
                    .background(PlatformTheme.colorScheme.outline),
        ) {
            if (avatar != null) {
                NetworkImage(
                    model = avatar,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PlatformText(
                text = card.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PlatformText(
                text = subtitle,
                style = PlatformTheme.typography.caption,
                color = PlatformTheme.colorScheme.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun KagariEmbedUnavailable(
    embed: KagariEmbed.Unavailable,
    modifier: Modifier = Modifier,
) {
    PlatformText(
        text =
            stringResource(
                when (embed.reason) {
                    KagariEmbed.Reason.Deleted -> Res.string.kagari_embed_deleted
                    KagariEmbed.Reason.Blocked -> Res.string.kagari_embed_blocked
                    KagariEmbed.Reason.RemovedByAuthor -> Res.string.kagari_embed_removed_by_author
                    KagariEmbed.Reason.RemovedByYou -> Res.string.kagari_embed_removed_by_you
                },
            ),
        style = PlatformTheme.typography.caption,
        color = PlatformTheme.colorScheme.caption,
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    FlareDividerDefaults.thickness,
                    color = FlareDividerDefaults.color,
                    shape = PlatformTheme.shapes.medium,
                ).padding(12.dp),
    )
}
