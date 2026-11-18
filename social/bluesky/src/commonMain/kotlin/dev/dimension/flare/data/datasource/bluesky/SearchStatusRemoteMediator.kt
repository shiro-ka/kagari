package dev.dimension.flare.data.datasource.bluesky

import androidx.paging.ExperimentalPagingApi
import app.bsky.feed.SearchPostsQueryParams
import app.bsky.feed.SearchPostsSort
import dev.dimension.flare.data.datasource.microblog.paging.CacheableRemoteLoader
import dev.dimension.flare.data.datasource.microblog.paging.PagingRequest
import dev.dimension.flare.data.datasource.microblog.paging.PagingResult
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.data.network.bluesky.resolveBlueskyVideoDownloadUrls
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.model.mapper.render

@OptIn(ExperimentalPagingApi::class)
internal class SearchStatusRemoteMediator(
    private val getService: suspend () -> BlueskyService,
    private val accountKey: MicroBlogKey,
    private val query: String,
    private val kagariTop: Boolean = false,
) : CacheableRemoteLoader<UiTimelineV2> {
    override val pagingKey: String =
        buildString {
            append("search_")
            append(query)
            if (kagariTop) {
                append("_top")
            }
            append(accountKey.toString())
        }

    override suspend fun load(
        pageSize: Int,
        request: PagingRequest,
    ): PagingResult<UiTimelineV2> {
        val service = getService()
        val response =
            when (request) {
                is PagingRequest.Prepend -> {
                    return PagingResult(
                        endOfPaginationReached = true,
                    )
                }

                PagingRequest.Refresh -> {
                    service.searchPosts(
                        SearchPostsQueryParams(
                            q = query,
                            limit = pageSize.toLong(),
                            sort = if (kagariTop) SearchPostsSort.Top else null,
                        ),
                    )
                }

                is PagingRequest.Append -> {
                    service.searchPosts(
                        SearchPostsQueryParams(
                            q = query,
                            limit = pageSize.toLong(),
                            sort = if (kagariTop) SearchPostsSort.Top else null,
                            cursor = request.nextKey,
                        ),
                    )
                }
            }.requireResponse()

        val downloadUrls = resolveBlueskyVideoDownloadUrls(response.posts, accountKey)
        return PagingResult(
            endOfPaginationReached = response.cursor == null,
            data = response.posts.map { it.render(accountKey, downloadUrls) },
            nextKey = response.cursor,
        )
    }
}
