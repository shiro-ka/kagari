package dev.dimension.flare.data.network.bluesky

import dev.dimension.flare.data.datasource.microblog.KagariLinkCardPreview
import dev.dimension.flare.data.model.ProxyRef
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.toUiImage
import io.ktor.http.Url

internal suspend fun BlueskyLinkCardResolver.kagariPreview(
    url: String,
    accountKey: MicroBlogKey,
): KagariLinkCardPreview? {
    val card = resolve(url) ?: return null
    val domain = runCatching { Url(card.uri).host }.getOrNull().orEmpty()
    return KagariLinkCardPreview(
        uri = card.uri,
        title = card.title,
        domain = domain,
        image = card.imageUrl?.toUiImage(proxy = ProxyRef.forAccount(accountKey)),
    )
}
