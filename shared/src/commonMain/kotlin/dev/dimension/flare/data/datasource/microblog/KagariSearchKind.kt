package dev.dimension.flare.data.datasource.microblog

import dev.dimension.flare.data.database.cache.CacheDatabase
import dev.dimension.flare.data.datasource.microblog.paging.CacheableRemoteLoader
import dev.dimension.flare.data.datasource.microblog.paging.PagingRequest
import dev.dimension.flare.data.datasource.microblog.paging.PagingResult
import dev.dimension.flare.data.datasource.microblog.paging.RemoteLoader
import dev.dimension.flare.data.datasource.microblog.paging.notSupported
import dev.dimension.flare.data.model.tab.TimelineMergePolicy
import dev.dimension.flare.ui.model.UiTimelineV2
import kotlinx.serialization.Serializable

@Serializable
public enum class KagariSearchKind {
    Posts,
    Top,
    Latest,
    Users,
    Media,
    Oldest,
    Popular,
}

public interface KagariSearchKinds {
    public val kagariSearchKinds: List<KagariSearchKind>

    public fun kagariSearchStatus(
        query: String,
        kind: KagariSearchKind,
    ): RemoteLoader<UiTimelineV2>
}

public fun MicroblogDataSource.kagariSearchKindList(): List<KagariSearchKind> {
    val search = capabilities.search ?: return emptyList()
    val posts = search.supports(SearchOperation.Posts)
    val users = if (search.supports(SearchOperation.Profiles)) listOf(KagariSearchKind.Users) else emptyList()
    val source = this
    return when {
        source is KagariSearchKinds -> source.kagariSearchKinds
        source is KagariSortableSearch && posts -> listOf(KagariSearchKind.Top, KagariSearchKind.Latest) + users
        posts -> listOf(KagariSearchKind.Posts) + users
        else -> users
    }
}

public fun MicroblogDataSource.kagariSearchPosts(
    query: String,
    kind: KagariSearchKind,
): RemoteLoader<UiTimelineV2> =
    when (val source = this) {
        is KagariSearchKinds -> source.kagariSearchStatus(query, kind)
        is KagariSortableSearch -> source.kagariSearchStatus(query, kind == KagariSearchKind.Top)
        else -> source.searchStatus(query)
    }

public interface KagariNewestFirstPostsSearch

public fun MicroblogDataSource.kagariSearchNewestFirst(kind: KagariSearchKind): Boolean =
    when (kind) {
        KagariSearchKind.Latest -> true
        KagariSearchKind.Posts -> this is KagariNewestFirstPostsSearch
        else -> false
    }

public fun KagariSearchKind.kagariCanMix(): Boolean = this != KagariSearchKind.Users

internal class KagariSearchMixMember(
    private val loader: RemoteLoader<UiTimelineV2>,
    override val pagingKey: String,
    override val canJoinTimeline: Boolean,
) : CacheableRemoteLoader<UiTimelineV2> {
    override val remotePageSize: Int?
        get() = (loader as? CacheableRemoteLoader<*>)?.remotePageSize

    override val collapseReplyChains: Boolean
        get() = (loader as? CacheableRemoteLoader<*>)?.collapseReplyChains ?: true

    override suspend fun load(
        pageSize: Int,
        request: PagingRequest,
    ): PagingResult<UiTimelineV2> = loader.load(pageSize, request)
}

internal fun kagariMixedSearchLoader(
    database: CacheDatabase,
    query: String,
    members: List<Pair<MicroblogDataSource, KagariSearchKind>>,
): RemoteLoader<UiTimelineV2> {
    val mixable = members.filter { (_, kind) -> kind.kagariCanMix() }
    if (mixable.isEmpty()) return notSupported()
    return MixedRemoteMediator(
        database = database,
        mediators =
            mixable.map { (source, kind) ->
                KagariSearchMixMember(
                    loader = source.kagariSearchPosts(query, kind),
                    pagingKey = "kagari_search_mix/${kind.name}/${source.accountKeyOrNull}/$query",
                    canJoinTimeline = source.kagariSearchNewestFirst(kind),
                )
            },
        mergePolicy = TimelineMergePolicy.Time,
    )
}
