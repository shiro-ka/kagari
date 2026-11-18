package dev.dimension.flare.data.datasource.microblog.loader

import dev.dimension.flare.data.datasource.microblog.paging.PagingResult
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiDMItem
import dev.dimension.flare.ui.model.UiDMRoom

public interface KagariDMRoomLoader {
    public suspend fun kagariLoadRequests(
        pageSize: Int,
        cursor: String?,
    ): PagingResult<UiDMRoom>

    public suspend fun kagariAccept(roomKey: MicroBlogKey): UiDMRoom?

    public suspend fun kagariSetMuted(
        roomKey: MicroBlogKey,
        muted: Boolean,
    ): UiDMRoom

    public suspend fun kagariSendPost(
        roomKey: MicroBlogKey,
        text: String,
        uri: String,
        cid: String,
    ): UiDMItem
}
