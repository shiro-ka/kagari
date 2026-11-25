package dev.dimension.flare.ui.model.mapper

import dev.dimension.flare.data.datasource.microblog.ActionMenu
import dev.dimension.flare.data.network.xqt.model.Tweet
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.ClickEvent
import dev.dimension.flare.ui.model.UiIcon
import dev.dimension.flare.ui.model.UiTimelineV2
import dev.dimension.flare.ui.route.KagariHideReplyRoute

internal fun Tweet.kagariIsReplyInOwnConversation(
    accountKey: MicroBlogKey,
    parents: List<UiTimelineV2.Post>,
): Boolean {
    val tweet = legacy ?: return false
    val replyTo = tweet.in_reply_to_status_id_str ?: return false
    val rootId = tweet.conversationIdStr ?: return false
    if (replyTo == rootId && tweet.in_reply_to_user_id_str == accountKey.id) return true
    return parents.any { it.statusKey.id == rootId && it.user?.key == accountKey }
}

internal fun kagariXqtHideReplyMenu(
    accountKey: MicroBlogKey,
    statusKey: MicroBlogKey,
): List<ActionMenu> =
    listOf(true, false).map { hide ->
        ActionMenu.Item(
            icon = if (hide) UiIcon.KagariHideReply else UiIcon.Eye,
            text =
                ActionMenu.Item.Text.Localized(
                    type =
                        if (hide) {
                            ActionMenu.Item.Text.Localized.Type.KagariHideReply
                        } else {
                            ActionMenu.Item.Text.Localized.Type.KagariUnhideReply
                        },
                ),
            clickEvent =
                ClickEvent.Deeplink(
                    KagariHideReplyRoute(
                        statusKey = statusKey,
                        accountType = AccountType.Specific(accountKey),
                        hide = hide,
                    ),
                ),
        )
    }
