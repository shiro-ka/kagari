package dev.dimension.flare.ui.model.mapper

import app.bsky.embed.RecordViewRecordUnion
import app.bsky.embed.RecordWithMediaViewMediaUnion
import app.bsky.feed.PostView
import app.bsky.feed.PostViewEmbedUnion
import app.bsky.graph.Token
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.KagariEmbed
import dev.dimension.flare.ui.model.UiCard
import dev.dimension.flare.ui.model.UiMedia
import dev.dimension.flare.ui.route.DeeplinkRoute
import dev.dimension.flare.ui.route.KagariFeedRoute
import dev.dimension.flare.ui.route.KagariListRoute
import dev.dimension.flare.ui.route.toUri
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal fun kagariFindEmbed(
    accountKey: MicroBlogKey,
    postView: PostView,
): KagariEmbed? {
    val record =
        when (val embed = postView.embed) {
            is PostViewEmbedUnion.RecordView -> embed.value.record
            is PostViewEmbedUnion.RecordWithMediaView -> embed.value.record.record
            else -> return null
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
                avatar = feed.avatar?.uri,
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
                avatar = list.avatar?.uri,
                likeCount = null,
                url = KagariListRoute(accountKey = accountKey, uri = list.uri.atUri, title = list.name).toUri(),
            )
        }

        is RecordViewRecordUnion.GraphStarterPackViewBasic -> {
            val pack = record.value
            val handle = pack.creator.handle.handle
            val name =
                runCatching {
                    pack.record
                        .decodeAs<JsonObject>()["name"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                }.getOrNull()
            KagariEmbed.Card(
                kind = KagariEmbed.Kind.StarterPack,
                title = name ?: handle,
                creatorHandle = handle,
                avatar = null,
                likeCount = null,
                url =
                    DeeplinkRoute
                        .OpenLinkDirectly(
                            "https://bsky.app/starter-pack/$handle/${pack.uri.atUri.substringAfterLast("/")}",
                        ).toUri(),
            )
        }

        is RecordViewRecordUnion.LabelerLabelerView -> {
            val labeler = record.value
            val handle = labeler.creator.handle.handle
            KagariEmbed.Card(
                kind = KagariEmbed.Kind.Labeler,
                title = labeler.creator.displayName?.takeIf { it.isNotBlank() } ?: handle,
                creatorHandle = handle,
                avatar = labeler.creator.avatar?.uri,
                likeCount = labeler.likeCount,
                url = DeeplinkRoute.OpenLinkDirectly("https://bsky.app/profile/$handle").toUri(),
            )
        }

        else -> {
            null
        }
    }
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
