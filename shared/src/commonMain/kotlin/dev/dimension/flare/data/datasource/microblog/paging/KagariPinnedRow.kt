package dev.dimension.flare.data.datasource.microblog.paging

import dev.dimension.flare.ui.model.UiIcon
import dev.dimension.flare.ui.model.UiTimelineV2

internal fun UiTimelineV2.Message?.kagariIsPinned(): Boolean = this?.icon == UiIcon.Pin

internal fun UiTimelineV2.Message?.kagariRowSuffix(): String = if (kagariIsPinned()) ":kagari-pinned" else ""
