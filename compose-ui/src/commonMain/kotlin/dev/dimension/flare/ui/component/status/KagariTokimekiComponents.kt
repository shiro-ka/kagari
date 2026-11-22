package dev.dimension.flare.ui.component.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_embed_separator
import dev.dimension.flare.compose.ui.kagari_tokimeki_cancel
import dev.dimension.flare.compose.ui.kagari_tokimeki_cancel_failed
import dev.dimension.flare.compose.ui.kagari_tokimeki_days_left
import dev.dimension.flare.compose.ui.kagari_tokimeki_ended
import dev.dimension.flare.compose.ui.kagari_tokimeki_hours_left
import dev.dimension.flare.compose.ui.kagari_tokimeki_minutes_left
import dev.dimension.flare.compose.ui.kagari_tokimeki_vote
import dev.dimension.flare.compose.ui.kagari_tokimeki_vote_failed
import dev.dimension.flare.compose.ui.kagari_tokimeki_voted
import dev.dimension.flare.compose.ui.kagari_tokimeki_votes
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.ui.component.FlareDividerDefaults
import dev.dimension.flare.ui.component.platform.PlatformFilledTonalButton
import dev.dimension.flare.ui.component.platform.PlatformRadioButton
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.component.platform.PlatformTextButton
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.KagariTokimekiPoll
import dev.dimension.flare.ui.model.KagariTokimekiPollOption
import dev.dimension.flare.ui.presenter.KagariTokimekiPollFailure
import dev.dimension.flare.ui.presenter.KagariTokimekiPollPresenter
import dev.dimension.flare.ui.presenter.KagariTokimekiPollState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.route.DeeplinkRoute
import dev.dimension.flare.ui.route.toUri
import dev.dimension.flare.ui.theme.PlatformTheme
import moe.tlaster.precompose.molecule.producePresenter
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Clock

@Composable
internal fun kagariRememberTokimekiPoll(
    accountType: AccountType,
    embed: KagariEmbed?,
): KagariTokimekiPollState? {
    val tokimeki = embed as? KagariEmbed.TokimekiPoll ?: return null
    val state by producePresenter(key = "kagari_tokimeki_${accountType}_${tokimeki.url}") {
        remember(accountType, tokimeki) { KagariTokimekiPollPresenter(accountType, tokimeki) }.invoke()
    }
    return state.takeIf { it.poll != null }
}

@Composable
internal fun KagariTokimekiPollCard(
    state: KagariTokimekiPollState,
    modifier: Modifier = Modifier,
) {
    val poll = state.poll ?: return
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
                .clickable { uriHandler.openUri(DeeplinkRoute.OpenLinkDirectly(poll.url).toUri()) }
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        poll.options.forEachIndexed { index, option ->
            KagariTokimekiPollOptionRow(
                option = option,
                totalVotes = poll.totalVotes,
                showsResults = poll.showsResults,
                isMine = poll.myVote == index,
                canVote = state.canVote,
                selected = state.selected == index,
                onSelect = { state.select(index) },
            )
        }
        Row(
            modifier = Modifier.padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlatformText(
                text =
                    listOfNotNull(
                        stringResource(Res.string.kagari_tokimeki_votes, poll.totalVotes),
                        kagariTokimekiRemaining(poll),
                    ).joinToString(stringResource(Res.string.kagari_embed_separator)),
                style = PlatformTheme.typography.caption,
                color = PlatformTheme.colorScheme.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (state.canVote) {
                PlatformFilledTonalButton(
                    onClick = { state.vote() },
                    enabled = state.selected != null && !state.busy,
                ) {
                    PlatformText(text = stringResource(Res.string.kagari_tokimeki_vote))
                }
            } else if (!poll.isEnded && poll.myVote != null) {
                PlatformTextButton(
                    onClick = { state.cancel() },
                    enabled = state.canCancel,
                ) {
                    PlatformText(text = stringResource(Res.string.kagari_tokimeki_cancel))
                }
            }
        }
        val failure = state.failure
        if (failure != null) {
            PlatformText(
                text =
                    stringResource(
                        when (failure) {
                            KagariTokimekiPollFailure.Vote -> Res.string.kagari_tokimeki_vote_failed
                            KagariTokimekiPollFailure.Cancel -> Res.string.kagari_tokimeki_cancel_failed
                        },
                    ),
                style = PlatformTheme.typography.caption,
                color = PlatformTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun KagariTokimekiPollOptionRow(
    option: KagariTokimekiPollOption,
    totalVotes: Int,
    showsResults: Boolean,
    isMine: Boolean,
    canVote: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val percent = if (totalVotes > 0) (option.count * 100.0 / totalVotes).roundToInt() else 0
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .border(
                    width = if (selected) 2.dp else FlareDividerDefaults.thickness,
                    color = if (selected) PlatformTheme.colorScheme.primary else FlareDividerDefaults.color,
                    shape = PlatformTheme.shapes.small,
                ).clip(PlatformTheme.shapes.small)
                .clickable(enabled = canVote, onClick = onSelect),
    ) {
        if (showsResults) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                        .background(
                            PlatformTheme.colorScheme.primary.copy(alpha = if (isMine) 0.28f else 0.12f),
                        ),
            )
        }
        Row(
            modifier =
                Modifier
                    .zIndex(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canVote) {
                PlatformRadioButton(
                    selected = selected,
                    onClick = null,
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlatformText(
                    text = option.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isMine) {
                    PlatformText(
                        text = stringResource(Res.string.kagari_tokimeki_voted),
                        style = PlatformTheme.typography.caption,
                        color = PlatformTheme.colorScheme.caption,
                        maxLines = 1,
                        modifier =
                            Modifier
                                .clip(PlatformTheme.shapes.small)
                                .background(PlatformTheme.colorScheme.cardAlt)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (showsResults) {
                PlatformText(
                    text = "$percent%",
                    style = PlatformTheme.typography.caption,
                    color = PlatformTheme.colorScheme.text,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun kagariTokimekiRemaining(poll: KagariTokimekiPoll): String? {
    val remaining = poll.endsAt?.let { it - Clock.System.now() }
    if (poll.isEnded || (remaining != null && !remaining.isPositive())) {
        return stringResource(Res.string.kagari_tokimeki_ended)
    }
    if (remaining == null) {
        return null
    }
    val days = remaining.inWholeDays
    val hours = remaining.inWholeHours
    return when {
        days >= 1 -> stringResource(Res.string.kagari_tokimeki_days_left, days.toInt())
        hours >= 1 -> stringResource(Res.string.kagari_tokimeki_hours_left, hours.toInt())
        else -> stringResource(Res.string.kagari_tokimeki_minutes_left, remaining.inWholeMinutes.toInt().coerceAtLeast(1))
    }
}
