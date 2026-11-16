package dev.dimension.flare.ui.route

import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.serialization.Serializable

@Serializable
internal data class KagariInteractionGateDialogRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
) : Route.Status

@Serializable
internal data class KagariHideReplyDialogRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
) : Route.Status

@Serializable
internal data class KagariPinPostDialogRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
    val pin: Boolean,
) : Route.Status

@Serializable
internal data class KagariMuteThreadDialogRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
    val rootUri: String,
    val mute: Boolean,
) : Route.Status
