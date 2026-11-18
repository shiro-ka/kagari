package dev.dimension.flare.data.datasource.microblog

import dev.dimension.flare.data.datasource.microblog.paging.RemoteLoader
import dev.dimension.flare.ui.model.UiTimelineV2

public interface KagariSortableSearch {
    public fun kagariSearchStatus(
        query: String,
        top: Boolean,
    ): RemoteLoader<UiTimelineV2>
}
