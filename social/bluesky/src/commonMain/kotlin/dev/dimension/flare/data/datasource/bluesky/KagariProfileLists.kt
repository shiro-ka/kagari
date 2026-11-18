package dev.dimension.flare.data.datasource.bluesky

import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.PagingState
import app.bsky.actor.GetProfileQueryParams
import app.bsky.feed.GetActorFeedsQueryParams
import app.bsky.graph.GetActorStarterPacksQueryParams
import app.bsky.graph.GetListsQueryParams
import app.bsky.graph.StarterPackViewBasic
import app.bsky.graph.Token
import dev.dimension.flare.common.BasePagingSource
import dev.dimension.flare.data.datasource.microblog.KagariProfileListKind
import dev.dimension.flare.data.datasource.microblog.pagingConfig
import dev.dimension.flare.data.model.ProxyRef
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiList
import dev.dimension.flare.ui.model.mapper.render
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import sh.christian.ozone.api.Did

internal suspend fun BlueskyDataSource.kagariLoadProfileListKinds(userKey: MicroBlogKey): List<KagariProfileListKind> {
    val isMe = userKey == accountKey
    val associated =
        kagariService()
            .getProfile(GetProfileQueryParams(actor = Did(did = userKey.id)))
            .requireResponse()
            .associated
    val starterPacks = associated?.starterPacks ?: 0L
    val lists = (associated?.lists ?: 0L) - starterPacks
    return listOfNotNull(
        KagariProfileListKind.Feeds.takeIf { isMe || (associated?.feedgens ?: 0L) > 0L },
        KagariProfileListKind.StarterPacks.takeIf { isMe || starterPacks > 0L },
        KagariProfileListKind.Lists.takeIf { isMe || lists > 0L },
    )
}

internal fun BlueskyDataSource.kagariProfileListPager(
    userKey: MicroBlogKey,
    kind: KagariProfileListKind,
): Flow<PagingData<UiList>> =
    Pager(config = pagingConfig) {
        object : BasePagingSource<String, UiList>() {
            override fun getRefreshKey(state: PagingState<String, UiList>): String? = null

            override suspend fun doLoad(params: LoadParams<String>): LoadResult<String, UiList> {
                val service = kagariService()
                val actor = Did(did = userKey.id)
                val limit = params.loadSize.toLong().coerceIn(1, 100)
                val (items, cursor) =
                    when (kind) {
                        KagariProfileListKind.Feeds -> {
                            val result =
                                service
                                    .getActorFeeds(GetActorFeedsQueryParams(actor = actor, limit = limit, cursor = params.key))
                                    .requireResponse()
                            result.feeds.map { it.render(accountKey) } to result.cursor
                        }

                        KagariProfileListKind.Lists -> {
                            val result =
                                service
                                    .getLists(GetListsQueryParams(actor = actor, limit = limit, cursor = params.key))
                                    .requireResponse()
                            result.lists
                                .filter { it.purpose == Token.Curatelist || it.purpose == Token.Modlist }
                                .map { it.render(accountKey) } to result.cursor
                        }

                        KagariProfileListKind.StarterPacks -> {
                            val result =
                                service
                                    .getActorStarterPacks(
                                        GetActorStarterPacksQueryParams(actor = actor, limit = limit, cursor = params.key),
                                    ).requireResponse()
                            result.starterPacks.map { it.kagariRender(accountKey) } to result.cursor
                        }
                    }
                return LoadResult.Page(
                    data = items,
                    prevKey = null,
                    nextKey = cursor?.takeIf { items.isNotEmpty() },
                )
            }
        }
    }.flow

internal fun StarterPackViewBasic.kagariStarterPackName(): String? =
    runCatching {
        record
            .decodeAs<JsonObject>()["name"]
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull()

internal fun StarterPackViewBasic.kagariStarterPackDescription(): String? =
    runCatching {
        record
            .decodeAs<JsonObject>()["description"]
            ?.jsonPrimitive
            ?.contentOrNull
    }.getOrNull()

private fun StarterPackViewBasic.kagariRender(accountKey: MicroBlogKey): UiList.List =
    UiList.List(
        proxy = ProxyRef.forAccount(accountKey),
        id = uri.atUri,
        title = kagariStarterPackName() ?: creator.handle.handle,
        description = kagariStarterPackDescription(),
        creator = creator.render(accountKey),
    )
