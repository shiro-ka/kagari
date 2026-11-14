package dev.dimension.flare.ui.route

import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.serialization.Serializable

@Serializable
public data class KagariInteractionGateRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
) : DeeplinkRoute.Status()

@Serializable
public data class KagariHideReplyRoute(
    val statusKey: MicroBlogKey,
    val accountType: AccountType,
) : DeeplinkRoute.Status()
