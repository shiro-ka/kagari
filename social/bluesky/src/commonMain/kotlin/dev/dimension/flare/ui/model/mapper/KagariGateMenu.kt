package dev.dimension.flare.ui.model.mapper

import app.bsky.embed.RecordViewRecordUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import dev.dimension.flare.data.datasource.bluesky.kagariIsReply
import dev.dimension.flare.data.datasource.bluesky.kagariReplyRootUri
import dev.dimension.flare.data.datasource.bluesky.kagariString
import dev.dimension.flare.data.datasource.microblog.ActionMenu
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.ClickEvent
import dev.dimension.flare.ui.model.UiIcon
import dev.dimension.flare.ui.route.KagariDetachQuoteRoute
import dev.dimension.flare.ui.route.KagariHideReplyRoute
import dev.dimension.flare.ui.route.KagariInteractionGateRoute
import dev.dimension.flare.ui.route.KagariMuteThreadRoute
import dev.dimension.flare.ui.route.KagariPinPostRoute
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal fun PostView.kagariInteractionGateMenu(
    accountKey: MicroBlogKey,
    statusKey: MicroBlogKey,
): List<ActionMenu> {
    val clickEvent =
        ClickEvent.Deeplink(
            KagariInteractionGateRoute(
                statusKey = statusKey,
                accountType = AccountType.Specific(accountKey),
            ),
        )
    val isReply = record.kagariIsReply()
    val tokens = if (isReply) listOf("reply-post") else threadgateTokens()
    return listOf(
        ActionMenu.Item(
            icon = UiIcon.KagariGateSettings,
            text =
                ActionMenu.Item.Text.Localized(
                    type = ActionMenu.Item.Text.Localized.Type.KagariReplyGate,
                    parameters = tokens.toImmutableList(),
                ),
            clickEvent = clickEvent,
        ),
        ActionMenu.Divider,
    )
}

private fun PostView.threadgateTokens(): List<String> {
    val gate = threadgate?.record ?: return listOf("anyone")
    val allow =
        runCatching { gate.decodeAs<JsonObject>()["allow"]?.jsonArray }.getOrNull()
            ?: return listOf("anyone")
    if (allow.isEmpty()) return listOf("nobody")
    val types =
        allow.mapNotNull {
            runCatching { it.jsonObject.kagariString("\$type") }.getOrNull()
        }
    return buildList {
        if (types.any { it.endsWith("#followerRule") }) add("followers")
        if (types.any { it.endsWith("#followingRule") }) add("following")
        if (types.any { it.endsWith("#mentionRule") }) add("mentioned")
        val lists = types.count { it.endsWith("#listRule") }
        if (lists > 0) add("lists:$lists")
    }.ifEmpty { listOf("anyone") }
}

internal fun PostView.kagariHideReplyMenu(
    accountKey: MicroBlogKey,
    statusKey: MicroBlogKey,
): List<ActionMenu> {
    val rootUri = runCatching { record.kagariReplyRootUri() }.getOrNull() ?: return emptyList()
    val rootAuthor = rootUri.removePrefix("at://").substringBefore('/')
    if (rootAuthor != accountKey.id) return emptyList()
    return listOf(
        ActionMenu.Item(
            icon = UiIcon.KagariHideReply,
            text = ActionMenu.Item.Text.Localized(type = ActionMenu.Item.Text.Localized.Type.KagariHideReply),
            clickEvent =
                ClickEvent.Deeplink(
                    KagariHideReplyRoute(
                        statusKey = statusKey,
                        accountType = AccountType.Specific(accountKey),
                    ),
                ),
        ),
    )
}

internal fun PostView.kagariPinMenu(
    accountKey: MicroBlogKey,
    statusKey: MicroBlogKey,
): List<ActionMenu> {
    val pinned = viewer?.pinned == true
    return listOf(
        ActionMenu.Item(
            icon = UiIcon.KagariPin,
            text =
                ActionMenu.Item.Text.Localized(
                    type =
                        if (pinned) {
                            ActionMenu.Item.Text.Localized.Type.KagariUnpinPost
                        } else {
                            ActionMenu.Item.Text.Localized.Type.KagariPinPost
                        },
                ),
            clickEvent =
                ClickEvent.Deeplink(
                    KagariPinPostRoute(
                        statusKey = statusKey,
                        accountType = AccountType.Specific(accountKey),
                        pin = !pinned,
                    ),
                ),
        ),
    )
}

internal fun PostView.kagariMuteThreadMenu(
    accountKey: MicroBlogKey,
    statusKey: MicroBlogKey,
): List<ActionMenu> {
    val rootUri = runCatching { record.kagariReplyRootUri() }.getOrNull() ?: uri.atUri
    val muted = viewer?.threadMuted == true
    return listOf(
        ActionMenu.Item(
            icon = if (muted) UiIcon.KagariUnmuteThread else UiIcon.KagariMuteThread,
            text =
                ActionMenu.Item.Text.Localized(
                    type =
                        if (muted) {
                            ActionMenu.Item.Text.Localized.Type.KagariUnmuteThread
                        } else {
                            ActionMenu.Item.Text.Localized.Type.KagariMuteThread
                        },
                ),
            clickEvent =
                ClickEvent.Deeplink(
                    KagariMuteThreadRoute(
                        statusKey = statusKey,
                        accountType = AccountType.Specific(accountKey),
                        rootUri = rootUri,
                        mute = !muted,
                    ),
                ),
        ),
    )
}

internal fun PostView.kagariDetachQuoteMenu(
    accountKey: MicroBlogKey,
    statusKey: MicroBlogKey,
): List<ActionMenu> {
    val embed = runCatching { record.decodeAs<JsonObject>()["embed"]?.jsonObject }.getOrNull() ?: return emptyList()
    val inner = embed["record"]?.jsonObject ?: return emptyList()
    val quotedUri =
        inner.kagariString("uri")
            ?: inner["record"]?.jsonObject?.kagariString("uri")
            ?: return emptyList()
    if (quotedUri.removePrefix("at://").substringBefore('/') != accountKey.id) return emptyList()
    val quoted =
        when (val view = this.embed) {
            is PostViewEmbedUnion.RecordView -> view.value.record
            is PostViewEmbedUnion.RecordWithMediaView -> view.value.record.record
            else -> null
        }
    val detached = quoted is RecordViewRecordUnion.ViewDetached
    return listOf(
        ActionMenu.Item(
            icon = UiIcon.KagariHideReply,
            text =
                ActionMenu.Item.Text.Localized(
                    type =
                        if (detached) {
                            ActionMenu.Item.Text.Localized.Type.KagariReattachQuote
                        } else {
                            ActionMenu.Item.Text.Localized.Type.KagariDetachQuote
                        },
                ),
            clickEvent =
                ClickEvent.Deeplink(
                    KagariDetachQuoteRoute(
                        statusKey = statusKey,
                        accountType = AccountType.Specific(accountKey),
                        quotedUri = quotedUri,
                        detach = !detached,
                    ),
                ),
        ),
    )
}
