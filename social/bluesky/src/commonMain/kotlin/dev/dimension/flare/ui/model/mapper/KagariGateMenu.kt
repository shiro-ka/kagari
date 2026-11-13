package dev.dimension.flare.ui.model.mapper

import app.bsky.feed.PostView
import dev.dimension.flare.data.datasource.microblog.ActionMenu
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.ClickEvent
import dev.dimension.flare.ui.model.UiIcon
import dev.dimension.flare.ui.route.KagariInteractionGateRoute
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
    val isReply = runCatching { record.decodeAs<JsonObject>()["reply"] != null }.getOrDefault(false)
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
            runCatching { it.jsonObject["\$type"]?.jsonPrimitive?.contentOrNull }.getOrNull()
        }
    return buildList {
        if (types.any { it.endsWith("#followerRule") }) add("followers")
        if (types.any { it.endsWith("#followingRule") }) add("following")
        if (types.any { it.endsWith("#mentionRule") }) add("mentioned")
        val lists = types.count { it.endsWith("#listRule") }
        if (lists > 0) add("lists:$lists")
    }.ifEmpty { listOf("anyone") }
}
