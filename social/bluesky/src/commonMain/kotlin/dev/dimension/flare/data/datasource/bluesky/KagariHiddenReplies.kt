package dev.dimension.flare.data.datasource.bluesky

import app.bsky.feed.FeedViewPost
import app.bsky.unspecced.GetPostThreadOtherV2QueryParams
import app.bsky.unspecced.GetPostThreadOtherV2ThreadItemValueUnion
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.data.network.bluesky.resolveBlueskyVideoDownloadUrls
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.model.toAccountType
import dev.dimension.flare.ui.model.ClickEvent
import dev.dimension.flare.ui.model.UiIcon
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.model.mapper.render
import dev.dimension.flare.ui.route.DeeplinkRoute
import sh.christian.ozone.api.AtUri

internal const val KAGARI_HIDDEN_REPLIES_KEY: String = "kagari_hidden_replies"

internal suspend fun BlueskyService.kagariHiddenReplies(
    statusKey: MicroBlogKey,
    accountKey: MicroBlogKey,
): List<UiTimelineV2> {
    val response =
        getPostThreadOtherV2Unspecced(
            GetPostThreadOtherV2QueryParams(anchor = AtUri(statusKey.id)),
        ).requireResponse()
    val posts =
        response.thread.mapNotNull {
            (it.value as? GetPostThreadOtherV2ThreadItemValueUnion.ThreadItemPost)?.value?.post
        }
    val downloadUrls = resolveBlueskyVideoDownloadUrls(posts, accountKey)
    return posts.mapNotNull { post ->
        listOf(FeedViewPost(post))
            .render(accountKey, downloadUrls)
            .firstOrNull()
            ?.kagariWithHiddenLabel(accountKey)
    }
}

private fun UiTimelineV2.kagariWithHiddenLabel(accountKey: MicroBlogKey): UiTimelineV2 {
    val message =
        UiTimelineV2.Message(
            user = null,
            statusKey =
                MicroBlogKey(
                    id = statusKey.id + "_kagari_hidden",
                    host = accountKey.host,
                ),
            icon = UiIcon.KagariHideReply,
            type = UiTimelineV2.Message.Type.Localized(UiTimelineV2.Message.Type.Localized.MessageId.KagariHiddenReply),
            createdAt = createdAt,
            clickEvent =
                ClickEvent.Deeplink(
                    DeeplinkRoute.Status.Detail(
                        statusKey = statusKey,
                        accountType = accountKey.toAccountType(),
                    ),
                ),
            accountType = accountKey.toAccountType(),
        )
    return when (this) {
        is UiTimelineV2.TimelinePostItem -> copy(presentation = presentation.copy(message = message))
        is UiTimelineV2.Post -> UiTimelineV2.TimelinePostItem(post = this, presentation = UiTimelineV2.PostPresentation(message = message))
        else -> this
    }
}
