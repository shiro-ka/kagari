package dev.dimension.flare.ui.model.mapper

import dev.dimension.flare.data.datasource.microblog.ActionMenu
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.ClickEvent
import dev.dimension.flare.ui.model.UiIcon
import dev.dimension.flare.ui.route.KagariFeedFeedbackRoute

internal data class KagariFeedSource(
    val uri: String,
    val serviceDid: String,
)

internal data class KagariFeedItem(
    val feed: KagariFeedSource,
    val feedContext: String?,
    val reqId: String?,
)

internal fun kagariFeedFeedbackMenu(
    accountKey: MicroBlogKey,
    statusKey: MicroBlogKey,
    item: KagariFeedItem?,
): List<ActionMenu> {
    if (item == null) return emptyList()
    fun entry(more: Boolean): ActionMenu.Item =
        ActionMenu.Item(
            icon = if (more) UiIcon.KagariFaceSmile else UiIcon.KagariFaceFrown,
            text =
                ActionMenu.Item.Text.Localized(
                    type =
                        if (more) {
                            ActionMenu.Item.Text.Localized.Type.KagariShowMoreLikeThis
                        } else {
                            ActionMenu.Item.Text.Localized.Type.KagariShowLessLikeThis
                        },
                ),
            clickEvent =
                ClickEvent.Deeplink(
                    KagariFeedFeedbackRoute(
                        statusKey = statusKey,
                        accountType = AccountType.Specific(accountKey),
                        feedUri = item.feed.uri,
                        feedServiceDid = item.feed.serviceDid,
                        feedContext = item.feedContext,
                        reqId = item.reqId,
                        more = more,
                    ),
                ),
        )
    return listOf(
        ActionMenu.Divider,
        entry(more = true),
        entry(more = false),
    )
}
