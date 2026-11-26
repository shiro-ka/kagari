package dev.dimension.flare.data.datasource.microblog

import dev.dimension.flare.data.datasource.microblog.paging.RemoteLoader
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
