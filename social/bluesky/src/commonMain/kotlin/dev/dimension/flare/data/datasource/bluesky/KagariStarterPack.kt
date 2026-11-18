package dev.dimension.flare.data.datasource.bluesky

import app.bsky.graph.Follow
import app.bsky.graph.GetListQueryParams
import app.bsky.graph.GetStarterPackQueryParams
import com.atproto.repo.CreateRecordRequest
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiList
import dev.dimension.flare.ui.model.UiProfile
import dev.dimension.flare.ui.model.mapper.render
import kotlinx.serialization.json.JsonObject
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import kotlin.time.Clock

public data class KagariStarterPackMember(
    val profile: UiProfile,
    val following: Boolean,
)

public data class KagariStarterPackDetail(
    val name: String,
    val description: String?,
    val creator: UiProfile,
    val feeds: List<UiList.Feed>,
    val members: List<KagariStarterPackMember>,
)

internal suspend fun BlueskyService.kagariLoadStarterPack(
    accountKey: MicroBlogKey,
    uri: String,
): KagariStarterPackDetail {
    val pack =
        getStarterPack(GetStarterPackQueryParams(starterPack = AtUri(uri)))
            .requireResponse()
            .starterPack
    val record = runCatching { pack.record.decodeAs<JsonObject>() }.getOrNull()
    val members = mutableListOf<KagariStarterPackMember>()
    val listUri = pack.list?.uri
    if (listUri != null) {
        var cursor: String? = null
        do {
            val page =
                getList(GetListQueryParams(list = listUri, limit = 100, cursor = cursor))
                    .requireResponse()
            page.items.forEach { item ->
                members +=
                    KagariStarterPackMember(
                        profile = item.subject.render(accountKey),
                        following = item.subject.viewer?.following != null,
                    )
            }
            cursor = page.cursor?.takeIf { page.items.isNotEmpty() }
        } while (cursor != null && members.size < 1000)
    }
    return KagariStarterPackDetail(
        name = record?.kagariString("name") ?: pack.creator.handle.handle,
        description = record?.kagariString("description"),
        creator = pack.creator.render(accountKey),
        feeds = pack.feeds.orEmpty().map { it.render(accountKey) },
        members = members.filter { it.profile.key != accountKey },
    )
}

internal suspend fun BlueskyService.kagariFollow(
    accountKey: MicroBlogKey,
    userKey: MicroBlogKey,
) {
    createRecord(
        CreateRecordRequest(
            repo = Did(did = accountKey.id),
            collection = Nsid("app.bsky.graph.follow"),
            record =
                Follow(
                    subject = Did(userKey.id),
                    createdAt = Clock.System.now(),
                ).bskyJson(),
        ),
    ).requireResponse()
}
