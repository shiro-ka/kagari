package dev.dimension.flare.data.datasource.microblog.datasource

import androidx.compose.runtime.Immutable
import dev.dimension.flare.model.MicroBlogKey

public interface KagariPixivBookmarkDataSource {
    public suspend fun kagariPixivBookmarkDetail(statusKey: MicroBlogKey): KagariPixivBookmarkDetail

    public suspend fun kagariPixivSaveBookmark(
        statusKey: MicroBlogKey,
        isPrivate: Boolean,
        tags: List<String>,
    )

    public suspend fun kagariPixivRemoveBookmark(statusKey: MicroBlogKey)
}

@Immutable
public data class KagariPixivBookmarkDetail(
    val isBookmarked: Boolean,
    val isPrivate: Boolean,
    val tags: List<KagariPixivBookmarkTag>,
)

@Immutable
public data class KagariPixivBookmarkTag(
    val name: String,
    val isRegistered: Boolean,
)
