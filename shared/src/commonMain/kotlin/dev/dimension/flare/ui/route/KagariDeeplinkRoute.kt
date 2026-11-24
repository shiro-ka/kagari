package dev.dimension.flare.ui.route

import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.serialization.Serializable

public sealed interface KagariDeeplink

@Serializable
public data class KagariInteractionGateRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariHideReplyRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariPinPostRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
    val pin: Boolean,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariMuteThreadRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
    val rootUri: String,
    val mute: Boolean,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariVerificationRoute(
    val accountKey: MicroBlogKey,
    val userKey: MicroBlogKey,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariDetachQuoteRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
    val quotedUri: String,
    val detach: Boolean,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariProfileEditRoute(
    val accountKey: MicroBlogKey,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariFeedRoute(
    val accountKey: MicroBlogKey,
    val uri: String,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariListRoute(
    val accountKey: MicroBlogKey,
    val uri: String,
    val title: String,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariStarterPackRoute(
    val accountKey: MicroBlogKey,
    val uri: String,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariSendViaChatRoute(
    val accountKey: MicroBlogKey,
    val uri: String,
    val cid: String,
) : DeeplinkRoute.Status(),
    KagariDeeplink

@Serializable
public data class KagariPixivBookmarkRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
) : DeeplinkRoute.Status(),
    KagariDeeplink
