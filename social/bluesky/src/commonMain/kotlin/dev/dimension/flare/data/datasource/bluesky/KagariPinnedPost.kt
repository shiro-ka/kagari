package dev.dimension.flare.data.datasource.bluesky

import app.bsky.feed.GetPostsQueryParams
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.PutRecordRequest
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey

internal suspend fun setBlueskyPinnedPost(
    service: BlueskyService,
    accountKey: MicroBlogKey,
    postUri: String,
    pin: Boolean,
) {
    val repo = Did(did = accountKey.id)
    val collection = Nsid("app.bsky.actor.profile")
    val rkey = RKey("self")
    val current =
        service
            .getRecord(GetRecordQueryParams(repo = repo, collection = collection, rkey = rkey))
            .maybeResponse()
            ?.value
            ?.decodeAs<JsonObject>()
            ?: buildJsonObject { put("\$type", "app.bsky.actor.profile") }
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
    service
        .putRecord(
            PutRecordRequest(
                repo = repo,
                collection = collection,
                rkey = rkey,
                record = updated.bskyJson(),
            ),
        ).requireResponse()
}
