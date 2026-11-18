package dev.dimension.flare.data.datasource.bluesky

import app.bsky.feed.GetPostsQueryParams
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did

internal suspend fun setBlueskyPinnedPost(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    postUri: String,
    pin: Boolean,
) {
    val repo = Did(did = accountKey.id)
    val current = service.kagariProfileRecord(repo)
    val updated =
        if (pin) {
            val post =
                service
                    .getPosts(GetPostsQueryParams(persistentListOf(AtUri(postUri))))
                    .requireResponse()
                    .posts
                    .firstOrNull() ?: error("Post not found")
            JsonObject(
                current +
                    (
                        "pinnedPost" to
                            buildJsonObject {
                                put("uri", post.uri.atUri)
                                put("cid", post.cid.cid)
                            }
                    ),
            )
        } else {
            JsonObject(current - "pinnedPost")
        }
    service.kagariPutProfileRecord(repo, updated)
}
