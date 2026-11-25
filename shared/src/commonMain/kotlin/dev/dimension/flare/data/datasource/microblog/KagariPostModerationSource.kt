package dev.dimension.flare.data.datasource.microblog

import dev.dimension.flare.model.MicroBlogKey

public interface KagariPostModerationSource {
    public suspend fun kagariSetReplyHidden(
        statusKey: MicroBlogKey,
        hide: Boolean,
    )
}
