package dev.dimension.flare.ui.component.dm

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.dimension.flare.ui.component.FlareDividerDefaults
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.component.status.CommonStatusComponent
import dev.dimension.flare.ui.model.KagariDMReaction
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.theme.PlatformTheme

@Composable
internal fun KagariDMQuote(
    post: UiTimelineV2.Post,
    modifier: Modifier = Modifier,
) {
    CommonStatusComponent(
        post,
        isQuote = true,
        modifier =
            modifier
                .fillMaxWidth(0.75f)
                .clip(PlatformTheme.shapes.large)
                .border(
                    FlareDividerDefaults.thickness,
                    color = FlareDividerDefaults.color,
                    shape = PlatformTheme.shapes.large,
                ).padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KagariDMReactions(
    reactions: List<KagariDMReaction>,
    onClick: ((KagariDMReaction) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { reaction ->
            PlatformText(
                text = if (reaction.count > 1) "${reaction.value} ${reaction.count}" else reaction.value,
                style = PlatformTheme.typography.caption,
                color = if (reaction.mine) PlatformTheme.colorScheme.primary else PlatformTheme.colorScheme.onCard,
                modifier =
                    Modifier
                        .clip(PlatformTheme.shapes.large)
                        .border(
                            FlareDividerDefaults.thickness,
                            color = if (reaction.mine) PlatformTheme.colorScheme.primary else FlareDividerDefaults.color,
                            shape = PlatformTheme.shapes.large,
                        ).clickable(enabled = onClick != null && reaction.mine) { onClick?.invoke(reaction) }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
