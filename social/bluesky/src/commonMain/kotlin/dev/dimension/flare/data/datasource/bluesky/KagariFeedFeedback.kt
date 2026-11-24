package dev.dimension.flare.data.datasource.bluesky

import app.bsky.feed.GetFeedGeneratorQueryParams
import app.bsky.feed.Interaction
import app.bsky.feed.SendInteractionsRequest
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.ui.model.mapper.KagariFeedSource
import sh.christian.ozone.api.AtUri

internal suspend fun BlueskyService.kagariFeedSource(uri: String): KagariFeedSource? =
    runCatching {
        getFeedGenerator(GetFeedGeneratorQueryParams(feed = AtUri(uri)))
            .maybeResponse()
            ?.view
            ?.takeIf { it.acceptsInteractions == true }
            ?.let { KagariFeedSource(uri = uri, serviceDid = it.did.did) }
    }.getOrNull()

internal suspend fun BlueskyService.kagariSendFeedFeedback(
    feedUri: String,
    feedServiceDid: String,
    postUri: String,
    feedContext: String?,
    reqId: String?,
    more: Boolean,
) {
    kagariForFeedGenerator(feedServiceDid)
        .sendInteractions(
            SendInteractionsRequest(
                feed = AtUri(feedUri),
                interactions =
                    listOf(
                        Interaction(
                            item = AtUri(postUri),
                            event = if (more) "app.bsky.feed.defs#requestMore" else "app.bsky.feed.defs#requestLess",
                            feedContext = feedContext,
                            reqId = reqId,
                        ),
                    ),
            ),
        ).requireResponse()
}
