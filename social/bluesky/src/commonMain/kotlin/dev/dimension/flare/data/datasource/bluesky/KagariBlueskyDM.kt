package dev.dimension.flare.data.datasource.bluesky

import app.bsky.actor.GetProfileQueryParams
import app.bsky.richtext.Facet
import chat.bsky.convo.AddReactionRequest
import chat.bsky.convo.RemoveReactionRequest
import dev.dimension.flare.data.database.cache.mapper.Bluesky
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiDMItem
import dev.dimension.flare.ui.model.mapper.parseBskyFacets
import sh.christian.ozone.api.Handle

internal suspend fun BlueskyService.kagariMessageFacets(text: String): List<Facet> =
    runCatching {
        parseBskyFacets(
            text,
            resolveMentionDid = { handle ->
                getProfile(GetProfileQueryParams(actor = Handle(handle = handle)))
                    .requireResponse()
                    .did.did
            },
        )
    }.getOrDefault(emptyList())

internal suspend fun BlueskyService.kagariReactToMessage(
    accountKey: MicroBlogKey,
    roomKey: MicroBlogKey,
    messageKey: MicroBlogKey,
    value: String,
    add: Boolean,
): UiDMItem {
    val message =
        if (add) {
            addReaction(AddReactionRequest(convoId = roomKey.id, messageId = messageKey.id, value = value))
                .requireResponse()
                .message
        } else {
            removeReaction(RemoveReactionRequest(convoId = roomKey.id, messageId = messageKey.id, value = value))
                .requireResponse()
                .message
        }
    return Bluesky.messages(accountKey = accountKey, roomKey = roomKey, data = listOf(message)).single()
}
