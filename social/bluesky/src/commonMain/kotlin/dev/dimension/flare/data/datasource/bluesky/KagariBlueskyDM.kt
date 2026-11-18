package dev.dimension.flare.data.datasource.bluesky

import app.bsky.actor.GetProfileQueryParams
import app.bsky.embed.Record
import app.bsky.richtext.Facet
import chat.bsky.convo.AcceptConvoRequest
import chat.bsky.convo.AddReactionRequest
import chat.bsky.convo.GetConvoQueryParams
import chat.bsky.convo.ListConvosQueryParams
import chat.bsky.convo.ListConvosStatus
import chat.bsky.convo.MessageInput
import chat.bsky.convo.MessageInputEmbedUnion
import chat.bsky.convo.MuteConvoRequest
import chat.bsky.convo.RemoveReactionRequest
import chat.bsky.convo.SendMessageRequest
import chat.bsky.convo.UnmuteConvoRequest
import com.atproto.repo.StrongRef
import dev.dimension.flare.data.database.cache.mapper.Bluesky
import dev.dimension.flare.data.datasource.microblog.paging.PagingResult
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiDMItem
import dev.dimension.flare.ui.model.UiDMRoom
import dev.dimension.flare.ui.model.mapper.parseBskyFacets
import sh.christian.ozone.api.AtUri
import sh.christian.ozone.api.Cid
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

internal suspend fun BlueskyService.kagariLoadRequestRooms(
    accountKey: MicroBlogKey,
    pageSize: Int,
    cursor: String?,
): PagingResult<UiDMRoom> {
    val response =
        listConvos(
            ListConvosQueryParams(
                limit = pageSize.toLong().coerceIn(1, 100),
                cursor = cursor,
                status = ListConvosStatus.Request,
            ),
        ).requireResponse()
    return PagingResult(
        data = Bluesky.rooms(accountKey, response.convos),
        nextKey = response.cursor,
    )
}

internal suspend fun BlueskyService.kagariAcceptRoom(
    accountKey: MicroBlogKey,
    roomKey: MicroBlogKey,
): UiDMRoom? {
    acceptConvo(AcceptConvoRequest(convoId = roomKey.id)).requireResponse()
    return runCatching {
        Bluesky.rooms(accountKey, listOf(getConvo(GetConvoQueryParams(convoId = roomKey.id)).requireResponse().convo)).single()
    }.getOrNull()
}

internal suspend fun BlueskyService.kagariSetRoomMuted(
    accountKey: MicroBlogKey,
    roomKey: MicroBlogKey,
    muted: Boolean,
): UiDMRoom {
    val convo =
        if (muted) {
            muteConvo(MuteConvoRequest(convoId = roomKey.id)).requireResponse().convo
        } else {
            unmuteConvo(UnmuteConvoRequest(convoId = roomKey.id)).requireResponse().convo
        }
    return Bluesky.rooms(accountKey, listOf(convo)).single()
}

internal suspend fun BlueskyService.kagariSendPostMessage(
    accountKey: MicroBlogKey,
    roomKey: MicroBlogKey,
    text: String,
    uri: String,
    cid: String,
): UiDMItem {
    val message =
        sendMessage(
            SendMessageRequest(
                convoId = roomKey.id,
                message =
                    MessageInput(
                        text = text,
                        facets = kagariMessageFacets(text).ifEmpty { null },
                        embed =
                            MessageInputEmbedUnion.AppBskyEmbedRecord(
                                Record(record = StrongRef(uri = AtUri(uri), cid = Cid(cid))),
                            ),
                    ),
            ),
        ).requireResponse()
    return Bluesky.messages(accountKey = accountKey, roomKey = roomKey, data = listOf(message)).single()
}
