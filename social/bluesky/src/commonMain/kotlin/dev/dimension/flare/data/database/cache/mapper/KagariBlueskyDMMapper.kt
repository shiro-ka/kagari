package dev.dimension.flare.data.database.cache.mapper

import chat.bsky.convo.MessageView
import chat.bsky.convo.MessageViewEmbedUnion
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.KagariDMReaction
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.model.mapper.kagariRenderRecord

internal fun MessageView.kagariQuote(accountKey: MicroBlogKey): UiTimelineV2.Post? =
    when (val value = embed) {
        is MessageViewEmbedUnion.AppBskyEmbedRecordView -> kagariRenderRecord(accountKey, value.value.record)
        else -> null
    }

internal fun MessageView.kagariReactions(accountKey: MicroBlogKey): List<KagariDMReaction> =
    reactions
        .orEmpty()
        .groupBy { it.value }
        .map { (value, list) ->
            KagariDMReaction(
                value = value,
                count = list.size,
                mine = list.any { it.sender.did.did == accountKey.id },
            )
        }
