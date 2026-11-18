package dev.dimension.flare.ui.route

import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.screen.settings.KagariModerationListKind
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

@Serializable
internal data class KagariVerificationDialogRoute(
    val accountType: AccountType,
    val userKey: MicroBlogKey,
) : Route.Status

@Serializable
internal data class KagariModerationListRoute(
    val accountKey: MicroBlogKey,
    val kind: KagariModerationListKind,
) : Route.Settings

@Serializable
internal data class KagariDetachQuoteDialogRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
    val quotedUri: String,
    val detach: Boolean,
) : Route.Status

@Serializable
internal data class KagariProfileEditScreenRoute(
    val accountKey: MicroBlogKey,
) : Route.Profile

@Serializable
internal data class KagariMuteWordsRoute(
    val accountKey: MicroBlogKey,
) : Route.Settings

@Serializable
internal data class KagariStarterPackScreenRoute(
    val accountKey: MicroBlogKey,
    val uri: String,
) : Route.Profile
