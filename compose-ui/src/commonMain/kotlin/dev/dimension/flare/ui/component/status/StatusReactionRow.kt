package dev.dimension.flare.ui.component.status

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ContextualFlowRowOverflow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_reaction_others
import dev.dimension.flare.compose.ui.mastodon_item_show_more
import dev.dimension.flare.data.model.KagariReactionLayout
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.theme.PlatformTheme
import org.jetbrains.compose.resources.stringResource

private const val KAGARI_REACTION_LIMIT = 16

@OptIn(ExperimentalLayoutApi::class)
@Suppress("DEPRECATION")
@Composable
internal fun StatusReactionRow(
    itemCount: Int,
    isDetail: Boolean,
    modifier: Modifier = Modifier,
    layout: KagariReactionLayout = KagariReactionLayout.TwoLines,
    mineIndex: Int = -1,
    content: @Composable (Int) -> Unit,
) {
    if (!isDetail && layout == KagariReactionLayout.OneLineScroll) {
        Row(
            modifier = modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(itemCount) { index ->
                content(index)
            }
        }
    } else {
        var expanded by rememberSaveable { mutableStateOf(false) }
        val collapsed = !isDetail && !expanded
        val visible =
            if (collapsed && layout == KagariReactionLayout.FirstSixteen && itemCount > KAGARI_REACTION_LIMIT) {
                val mine = mineIndex.takeIf { it in KAGARI_REACTION_LIMIT until itemCount }
                List(KAGARI_REACTION_LIMIT) { it } + listOfNotNull(mine)
            } else {
                null
            }
        ContextualFlowRow(
            itemCount = if (visible != null) visible.size + 1 else itemCount,
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
            maxLines = if (collapsed && layout == KagariReactionLayout.TwoLines) 2 else Int.MAX_VALUE,
            overflow =
                ContextualFlowRowOverflow.expandIndicator {
                    PlatformText(
                        text = stringResource(Res.string.mastodon_item_show_more),
                        style = PlatformTheme.typography.postBody,
                        color = StatusComponentDefaults.secondaryColor,
                        modifier = Modifier.clickable { expanded = true },
                    )
                },
        ) { index ->
            when {
                visible == null -> {
                    if (index < itemCount) {
                        content(index)
                    }
                }
                index < visible.size -> content(visible[index])
                index == visible.size -> {
                    PlatformText(
                        text = stringResource(Res.string.kagari_reaction_others, itemCount - visible.size),
                        style = PlatformTheme.typography.postBody,
                        color = StatusComponentDefaults.secondaryColor,
                        modifier = Modifier.clickable { expanded = true },
                    )
                }
                else -> Unit
            }
        }
    }
}
