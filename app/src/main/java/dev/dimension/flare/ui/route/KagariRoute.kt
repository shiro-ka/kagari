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
    val hide: Boolean? = null,
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
internal data class KagariFeedFeedbackDialogRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
    val feedUri: String,
    val feedServiceDid: String,
    val feedContext: String?,
    val reqId: String?,
    val more: Boolean,
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

@Serializable
internal data class KagariDMRequestsRoute(
    val accountType: AccountType,
) : Route.DM

@Serializable
internal data class KagariSendViaChatScreenRoute(
    val accountKey: MicroBlogKey,
    val uri: String,
    val cid: String,
) : Route.DM

@Serializable
internal data class KagariPixivBookmarkSheetRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
) : Route.Status

internal fun KagariDeeplink.toKagariRoute(): Route =
    when (this) {
        is KagariHideReplyRoute -> {
            KagariHideReplyDialogRoute(
                statusKey = statusKey,
                accountType = accountType,
                hide = hide,
            )
        }

        is KagariPinPostRoute -> {
            KagariPinPostDialogRoute(
                statusKey = statusKey,
                accountType = accountType,
                pin = pin,
            )
        }

        is KagariMuteThreadRoute -> {
            KagariMuteThreadDialogRoute(
                statusKey = statusKey,
                accountType = accountType,
                rootUri = rootUri,
                mute = mute,
            )
        }

        is KagariProfileEditRoute -> {
            KagariProfileEditScreenRoute(accountKey = accountKey)
        }

        is KagariSendViaChatRoute -> {
            KagariSendViaChatScreenRoute(
                accountKey = accountKey,
                uri = uri,
                cid = cid,
            )
        }

        is KagariStarterPackRoute -> {
            KagariStarterPackScreenRoute(
                accountKey = accountKey,
                uri = uri,
            )
        }

        is KagariFeedRoute -> {
            Route.Bluesky.FeedDetail(
                accountType = AccountType.Specific(accountKey),
                feedId = uri,
            )
        }

        is KagariListRoute -> {
            Route.Lists.Detail(
                accountType = AccountType.Specific(accountKey),
                listId = uri,
                title = title,
            )
        }

        is KagariDetachQuoteRoute -> {
            KagariDetachQuoteDialogRoute(
                statusKey = statusKey,
                accountType = accountType,
                quotedUri = quotedUri,
                detach = detach,
            )
        }

        is KagariVerificationRoute -> {
            KagariVerificationDialogRoute(
                accountType = AccountType.Specific(accountKey),
                userKey = userKey,
            )
        }

        is KagariInteractionGateRoute -> {
            KagariInteractionGateDialogRoute(
                statusKey = statusKey,
                accountType = accountType,
            )
        }

        is KagariPixivBookmarkRoute -> {
            KagariPixivBookmarkSheetRoute(
                statusKey = statusKey,
                accountType = accountType,
            )
        }

        is KagariFeedFeedbackRoute -> {
            KagariFeedFeedbackDialogRoute(
                statusKey = statusKey,
                accountType = accountType,
                feedUri = feedUri,
                feedServiceDid = feedServiceDid,
                feedContext = feedContext,
                reqId = reqId,
                more = more,
            )
        }
    }
