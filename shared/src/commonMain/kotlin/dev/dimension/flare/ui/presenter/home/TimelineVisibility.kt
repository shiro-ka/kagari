package dev.dimension.flare.ui.presenter.home

import dev.dimension.flare.data.model.tab.TimelineFilterConfig
import dev.dimension.flare.data.repository.KeywordFilterPattern
import dev.dimension.flare.data.repository.MxgaSnapshot
import dev.dimension.flare.data.repository.isMxgaMatch
import dev.dimension.flare.model.PlatformRegistry
import dev.dimension.flare.ui.model.UiTimelineV2

internal data class TimelineVisibility(
    private val filters: List<KeywordFilterPattern>,
    private val config: TimelineFilterConfig,
    private val mxgaEnabled: Boolean,
    private val mxgaSnapshot: MxgaSnapshot,
    private val platformRegistry: PlatformRegistry,
    private val kagariHideMutedWords: Boolean = true,
) {
    fun isVisible(item: UiTimelineV2): Boolean =
        item is UiTimelineV2.Gap ||
            (
                item.matchesKeywordFilters(filters) && item.matchesTimelineFilter(config) &&
                    (!mxgaEnabled || !item.isMxgaMatch(mxgaSnapshot, platformRegistry)) &&
                    !(kagariHideMutedWords && item.kagariMutedByWord())
            )
}

internal fun UiTimelineV2.kagariMutedByWord(): Boolean =
    when (this) {
        is UiTimelineV2.Post -> kagariMutedWord != null
        is UiTimelineV2.TimelinePostItem -> post.kagariMutedWord != null
        else -> false
    }
