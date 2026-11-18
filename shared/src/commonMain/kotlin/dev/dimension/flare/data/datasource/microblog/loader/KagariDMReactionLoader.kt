package dev.dimension.flare.data.datasource.microblog.loader

import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiDMItem

public interface KagariDMReactionLoader {
    public suspend fun kagariReact(
        roomKey: MicroBlogKey,
        messageKey: MicroBlogKey,
        value: String,
        add: Boolean,
    ): UiDMItem
}
