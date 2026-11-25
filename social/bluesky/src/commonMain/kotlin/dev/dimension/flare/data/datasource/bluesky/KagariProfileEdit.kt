package dev.dimension.flare.data.datasource.bluesky

import app.bsky.actor.GetProfileQueryParams
import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.DeleteRecordRequest
import dev.dimension.flare.data.datasource.microblog.KagariProfileDraftImages
import dev.dimension.flare.data.datasource.microblog.KagariProfileEditor
import dev.dimension.flare.data.datasource.microblog.KagariProfileImageChanges
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import kotlin.time.Clock

public data class KagariProfileLink(
    val url: String,
    val title: String,
    val uri: String? = null,
    val cid: String? = null,
)

public data class KagariProfileDraft(
    val displayName: String,
    val description: String,
    val pronouns: String,
    val links: List<KagariProfileLink>,
    override val avatarUrl: String?,
    override val bannerUrl: String?,
) : KagariProfileDraftImages

internal class KagariBlueskyProfileEditor(
    private val dataSource: BlueskyDataSource,
) : KagariProfileEditor<KagariProfileDraft> {
    override suspend fun load(): KagariProfileDraft {
        val service = dataSource.kagariService()
        val did = Did(did = dataSource.accountKey.id)
        val record = service.kagariProfileRecord(did)
        val view = service.getProfile(GetProfileQueryParams(actor = did)).requireResponse()
        return KagariProfileDraft(
            displayName = record.string("displayName"),
            description = record.string("description"),
            pronouns = record.string("pronouns"),
            links =
                view.links.orEmpty().map {
                    KagariProfileLink(
                        url = it.url.uri,
                        title = it.title.orEmpty(),
                        uri = it.uri.atUri,
                        cid = it.cid.cid,
                    )
                },
            avatarUrl = view.avatar?.uri,
            bannerUrl = view.banner?.uri,
        )
    }

    override suspend fun save(
        before: KagariProfileDraft,
        after: KagariProfileDraft,
        images: KagariProfileImageChanges,
    ) {
        val service = dataSource.kagariService()
        val repo = Did(did = dataSource.accountKey.id)
        val current = service.kagariProfileRecord(repo)
        val updated = current.toMutableMap()
        updated["\$type"] = JsonPrimitive("app.bsky.actor.profile")
        updated.setOrRemove("displayName", after.displayName)
        updated.setOrRemove("description", after.description)
        updated.setOrRemove("pronouns", after.pronouns)
        images.avatar?.let {
            updated["avatar"] = service.uploadBlob(it.bytes).requireResponse().blob.bskyJson().decodeAs<JsonObject>()
        }
        val banner = images.banner
        if (banner != null) {
            updated["banner"] = service.uploadBlob(banner.bytes).requireResponse().blob.bskyJson().decodeAs<JsonObject>()
        } else if (images.removeBanner) {
            updated.remove("banner")
        }
        val refs =
            after.links.map { link ->
                val same = before.links.firstOrNull { it.uri != null && it.uri == link.uri }
                if (same != null && same.url == link.url && same.title == link.title && same.cid != null) {
                    same.uri!! to same.cid
                } else {
                    val created =
                        service
                            .createRecord(
                                CreateRecordRequest(
                                    repo = repo,
                                    collection = Nsid("app.bsky.actor.link"),
                                    record =
                                        buildJsonObject {
                                            put("\$type", "app.bsky.actor.link")
                                            put("url", link.url)
                                            if (link.title.isNotBlank()) put("title", link.title)
                                            put("createdAt", Clock.System.now().toString())
                                        }.bskyJson(),
                                ),
                            ).requireResponse()
                    created.uri.atUri to created.cid.cid
                }
            }
        if (refs.isEmpty()) {
            updated.remove("links")
        } else {
            updated["links"] =
                JsonArray(
                    refs.map { (uri, cid) ->
                        buildJsonObject {
                            put("uri", uri)
                            put("cid", cid)
                        }
                    },
                )
        }
        service.kagariPutProfileRecord(repo, JsonObject(updated))
        val keptUris = refs.map { it.first }.toSet()
        before.links
            .mapNotNull { it.uri }
            .filterNot { it in keptUris }
            .forEach { uri ->
                runCatching {
                    service.deleteRecord(
                        DeleteRecordRequest(
                            repo = repo,
                            collection = Nsid("app.bsky.actor.link"),
                            rkey = RKey(uri.substringAfterLast('/')),
                        ),
                    )
                }
            }
    }
}

private fun JsonObject.string(key: String): String = (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty()

private fun MutableMap<String, kotlinx.serialization.json.JsonElement>.setOrRemove(
    key: String,
    value: String,
) {
    if (value.isBlank()) remove(key) else this[key] = JsonPrimitive(value)
}
