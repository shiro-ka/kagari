package dev.dimension.flare.data.datasource.bluesky

import app.bsky.graph.GetBlocksQueryParams
import app.bsky.graph.GetMutesQueryParams
import dev.dimension.flare.data.datasource.microblog.MicroblogDataSource
import dev.dimension.flare.data.datasource.microblog.paging.PagingRequest
import dev.dimension.flare.data.datasource.microblog.paging.PagingResult
import dev.dimension.flare.data.datasource.microblog.paging.RemoteLoader
import dev.dimension.flare.data.datasource.microblog.paging.notSupported
import dev.dimension.flare.ui.model.UiProfile
import dev.dimension.flare.ui.model.mapper.render

public fun MicroblogDataSource.kagariMutedAccounts(): RemoteLoader<UiProfile> =
    (this as? BlueskyDataSource)?.let { KagariModerationListPagingSource(it, blocked = false) } ?: notSupported()

public fun MicroblogDataSource.kagariBlockedAccounts(): RemoteLoader<UiProfile> =
    (this as? BlueskyDataSource)?.let { KagariModerationListPagingSource(it, blocked = true) } ?: notSupported()

private class KagariModerationListPagingSource(
    private val dataSource: BlueskyDataSource,
    private val blocked: Boolean,
) : RemoteLoader<UiProfile> {
    override suspend fun load(
        pageSize: Int,
        request: PagingRequest,
    ): PagingResult<UiProfile> {
        val cursor =
            when (request) {
                is PagingRequest.Prepend -> return PagingResult(endOfPaginationReached = true)
                PagingRequest.Refresh -> null
                is PagingRequest.Append -> request.nextKey
            }
        val service = dataSource.kagariService()
        return if (blocked) {
            val response =
                service
                    .getBlocks(params = GetBlocksQueryParams(limit = pageSize.toLong(), cursor = cursor))
                    .requireResponse()
            PagingResult(
                endOfPaginationReached = response.cursor == null,
                data = response.blocks.map { it.render(accountKey = dataSource.accountKey) },
                nextKey = response.cursor,
            )
        } else {
            val response =
                service
                    .getMutes(params = GetMutesQueryParams(limit = pageSize.toLong(), cursor = cursor))
                    .requireResponse()
            PagingResult(
                endOfPaginationReached = response.cursor == null,
                data = response.mutes.map { it.render(accountKey = dataSource.accountKey) },
                nextKey = response.cursor,
            )
        }
    }
}
