package dev.dimension.flare.ui.model.mapper

import dev.dimension.flare.ui.model.toUiImage
import app.bsky.embed.RecordViewRecordUnion
import app.bsky.embed.RecordWithMediaViewMediaUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import app.bsky.graph.Token
import dev.dimension.flare.data.datasource.bluesky.kagariStarterPackName
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.UiCard
import dev.dimension.flare.ui.model.UiMedia
import dev.dimension.flare.ui.route.DeeplinkRoute
import dev.dimension.flare.ui.route.KagariFeedRoute
import dev.dimension.flare.ui.route.KagariListRoute
import dev.dimension.flare.ui.route.KagariStarterPackRoute
import dev.dimension.flare.ui.route.toUri
import io.ktor.http.decodeURLPart

internal fun kagariFindEmbed(
    accountKey: MicroBlogKey,
    postView: PostView,
): KagariEmbed? {
    val record =
        when (val embed = postView.embed) {
            is PostViewEmbedUnion.RecordView -> embed.value.record
            is PostViewEmbedUnion.RecordWithMediaView -> embed.value.record.record
            else -> return kagariFindMadoVoice(postView)
        }
    return when (record) {
        is RecordViewRecordUnion.ViewNotFound -> {
            KagariEmbed.Unavailable(KagariEmbed.Reason.Deleted)
        }

        is RecordViewRecordUnion.ViewBlocked -> {
            KagariEmbed.Unavailable(KagariEmbed.Reason.Blocked)
        }

        is RecordViewRecordUnion.ViewDetached -> {
            KagariEmbed.Unavailable(
                if (record.value.uri.atUri
                        .removePrefix("at://")
                        .substringBefore("/") == accountKey.id
                ) {
                    KagariEmbed.Reason.RemovedByYou
                } else {
                    KagariEmbed.Reason.RemovedByAuthor
                },
            )
        }

        is RecordViewRecordUnion.FeedGeneratorView -> {
            val feed = record.value
            KagariEmbed.Card(
                kind = KagariEmbed.Kind.Feed,
                title = feed.displayName,
                creatorHandle = feed.creator.handle.handle,
                avatar = feed.avatar?.uri.toUiImage(),
                likeCount = feed.likeCount,
                url = KagariFeedRoute(accountKey = accountKey, uri = feed.uri.atUri).toUri(),
            )
        }

        is RecordViewRecordUnion.GraphListView -> {
            val list = record.value
            KagariEmbed.Card(
                kind =
                    if (list.purpose == Token.Modlist) {
                        KagariEmbed.Kind.ModerationList
                    } else {
                        KagariEmbed.Kind.UserList
                    },
                title = list.name,
                creatorHandle = list.creator.handle.handle,
                avatar = list.avatar?.uri.toUiImage(),
                likeCount = null,
                url = KagariListRoute(accountKey = accountKey, uri = list.uri.atUri, title = list.name).toUri(),
            )
        }

        is RecordViewRecordUnion.GraphStarterPackViewBasic -> {
            val pack = record.value
            val handle = pack.creator.handle.handle
            KagariEmbed.Card(
                kind = KagariEmbed.Kind.StarterPack,
                title = pack.record.kagariStarterPackName() ?: handle,
                creatorHandle = handle,
                avatar = "https://ogcard.cdn.bsky.app/start/${pack.creator.did.did}/${pack.uri.atUri.substringAfterLast("/")}".toUiImage(),
                likeCount = null,
                url = KagariStarterPackRoute(accountKey = accountKey, uri = pack.uri.atUri).toUri(),
            )
        }

        is RecordViewRecordUnion.LabelerLabelerView -> {
            val labeler = record.value
            val handle = labeler.creator.handle.handle
            KagariEmbed.Card(
                kind = KagariEmbed.Kind.Labeler,
                title = labeler.creator.displayName?.takeIf { it.isNotBlank() } ?: handle,
                creatorHandle = handle,
                avatar = labeler.creator.avatar?.uri.toUiImage(),
                likeCount = labeler.likeCount,
                url = DeeplinkRoute.OpenLinkDirectly("https://bsky.app/profile/$handle").toUri(),
            )
        }

        else -> {
            kagariFindMadoVoice(postView)
        }
    }
}

private fun kagariFindMadoVoice(postView: PostView): KagariEmbed.MadoVoice? {
    val url =
        when (val embed = postView.embed) {
            is PostViewEmbedUnion.ExternalView -> embed.value.external.uri.uri
            is PostViewEmbedUnion.RecordWithMediaView ->
                (embed.value.media as? RecordWithMediaViewMediaUnion.ExternalView)
                    ?.value
                    ?.external
                    ?.uri
                    ?.uri
            else -> null
        } ?: return null
    val rest = url.substringAfter("://", "").takeIf { url.startsWith("https://", ignoreCase = true) } ?: return null
    if (!rest.substringBefore('/').equals("mado.blue", ignoreCase = true)) {
        return null
    }
    val segments =
        rest
            .substringAfter('/', "")
            .substringBefore('#')
            .substringBefore('?')
            .split('/')
            .filter { it.isNotEmpty() }
            .map { runCatching { it.decodeURLPart() }.getOrNull() ?: return null }
    if (segments.size != 3) {
        return null
    }
    val handle = segments[0].takeIf { it.startsWith("@") }?.removePrefix("@")?.takeIf { it.isNotBlank() } ?: return null
    val rkey = segments[2].takeIf { key -> key.all { it.isLetterOrDigit() || it in "._:~-" } } ?: return null
    val author = postView.author
    val ownerDid =
        author.did.did.takeIf {
            handle.equals(author.handle.handle, ignoreCase = true) || handle == it
        }
    return KagariEmbed.MadoVoice(
        handle = handle,
        rkey = rkey,
        url = url,
        ownerDid = ownerDid,
    )
}

internal fun kagariFindCardBesideQuote(postView: PostView): UiCard? {
    val embed = postView.embed as? PostViewEmbedUnion.RecordWithMediaView ?: return null
    val media = embed.value.media as? RecordWithMediaViewMediaUnion.ExternalView ?: return null
    val external = media.value.external
    val url = external.uri.uri
    return UiCard(
        url = url,
        title = external.title.takeIf { it.isNotEmpty() } ?: url,
        description = external.description.takeIf { it.isNotEmpty() } ?: url.takeIf { external.title.isNotEmpty() },
        media =
            external.thumb?.let {
                UiMedia.Image(
                    url = it.uri,
                    previewUrl = it.uri,
                    description = null,
                    width = 0f,
                    height = 0f,
                    sensitive = false,
                )
            },
    )
}
