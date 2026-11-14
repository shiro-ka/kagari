package dev.dimension.flare.ui.presenter.login

import dev.dimension.flare.model.xqtHost
import dev.dimension.flare.ui.model.UiInstance

private data class KagariLoginEntry(
    val platformId: String,
    val name: String,
    val domain: String,
)

private val kagariLoginEntries =
    listOf(
        KagariLoginEntry("Bluesky", "Bluesky", "bsky.social"),
        KagariLoginEntry("xQt", "Twitter", xqtHost),
        KagariLoginEntry("Misskey", "Misskey", "misskey.io"),
        KagariLoginEntry("Mastodon", "Mastodon", "mastodon.social"),
        KagariLoginEntry("Pixiv", "pixiv", "pixiv.net"),
        KagariLoginEntry("Fanbox", "FANBOX", "fanbox.cc"),
    )

internal fun LoginPlatformRegistry.kagariLoginInstances(): List<UiInstance> =
    kagariLoginEntries.mapNotNull { entry ->
        val provider = get(entry.platformId) ?: return@mapNotNull null
        UiInstance(
            name = entry.name,
            description = null,
            iconUrl = null,
            domain = entry.domain,
            platformId = entry.platformId,
            bannerUrl = null,
            usersCount = 0,
            platformDisplayName = provider.metadata.displayName,
            platformIcon = provider.metadata.icon,
        )
    }
