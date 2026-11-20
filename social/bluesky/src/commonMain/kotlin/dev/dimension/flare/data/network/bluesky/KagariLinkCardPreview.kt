package dev.dimension.flare.data.network.bluesky

import dev.dimension.flare.data.datasource.microblog.KagariLinkCardPreview
import io.ktor.http.Url

internal suspend fun BlueskyLinkCardResolver.kagariPreview(url: String): KagariLinkCardPreview? {
    val card = resolve(url) ?: return null
    val domain = runCatching { Url(card.uri).host }.getOrNull().orEmpty()
    return KagariLinkCardPreview(
        uri = card.uri,
        title = card.title,
        domain = domain,
        imageUrl = card.imageUrl,
    )
}
