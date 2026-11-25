package dev.dimension.flare.data.datasource.microblog

import dev.dimension.flare.model.MicroBlogKey

public interface KagariPostModerationSource {
    public suspend fun kagariSetReplyHidden(
        statusKey: MicroBlogKey,
        hide: Boolean,
    )

    public suspend fun kagariSetPinned(
        statusKey: MicroBlogKey,
        pin: Boolean,
    )

    public suspend fun kagariSetThreadMuted(
        rootId: String,
        mute: Boolean,
    )
}
