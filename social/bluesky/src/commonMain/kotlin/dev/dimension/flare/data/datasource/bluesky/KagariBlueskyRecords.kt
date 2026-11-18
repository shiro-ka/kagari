package dev.dimension.flare.data.datasource.bluesky

import app.bsky.actor.PreferencesUnion
import app.bsky.actor.PutPreferencesRequest
import com.atproto.repo.GetRecordQueryParams
import com.atproto.repo.PutRecordRequest
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.data.platform.BLUESKY_PLATFORM_ID
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import sh.christian.ozone.api.model.JsonContent

public fun kagariIsBluesky(platformId: String): Boolean = platformId == BLUESKY_PLATFORM_ID

internal class KagariAccountCache<T : Any> {
    private val values = MutableStateFlow<Map<MicroBlogKey, T>>(emptyMap())

    operator fun get(accountKey: MicroBlogKey): T? = values.value[accountKey]

    fun put(
        accountKey: MicroBlogKey,
        value: T,
    ) {
        values.update { it + (accountKey to value) }
    }
}

internal suspend inline fun <reified T : PreferencesUnion> BlueskyService.kagariPreferences(): List<T> =
    getPreferences()
        .requireResponse()
        .preferences
        .filterIsInstance<T>()

internal suspend inline fun <reified T : PreferencesUnion> BlueskyService.kagariReplacePreference(pref: T) {
    val others =
        getPreferences()
            .requireResponse()
            .preferences
            .filterNot { it is T }
    putPreferences(PutPreferencesRequest(preferences = (others + pref).toImmutableList()))
}

internal suspend fun BlueskyService.kagariGetRecord(
    repo: Did,
    collection: String,
    rkey: RKey,
): JsonContent? =
    getRecord(GetRecordQueryParams(repo = repo, collection = Nsid(collection), rkey = rkey))
        .maybeResponse()
        ?.value

internal suspend fun BlueskyService.kagariPutRecord(
    repo: Did,
    collection: String,
    rkey: RKey,
    record: JsonContent,
) {
    putRecord(
        PutRecordRequest(
            repo = repo,
            collection = Nsid(collection),
            rkey = rkey,
            record = record,
        ),
    ).requireResponse()
}

private const val KAGARI_PROFILE_COLLECTION = "app.bsky.actor.profile"

internal suspend fun BlueskyService.kagariProfileRecord(repo: Did): JsonObject =
    kagariGetRecord(repo, KAGARI_PROFILE_COLLECTION, RKey("self"))
        ?.decodeAs<JsonObject>()
        ?: buildJsonObject { put("\$type", KAGARI_PROFILE_COLLECTION) }

internal suspend fun BlueskyService.kagariPutProfileRecord(
    repo: Did,
    record: JsonObject,
) {
    kagariPutRecord(repo, KAGARI_PROFILE_COLLECTION, RKey("self"), record.bskyJson())
}

internal fun JsonObject.kagariString(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

internal fun JsonContent.kagariIsReply(): Boolean = runCatching { decodeAs<JsonObject>()["reply"] != null }.getOrDefault(false)

internal fun JsonContent.kagariReplyRootUri(): String? =
    decodeAs<JsonObject>()["reply"]
        ?.jsonObject
        ?.get("root")
        ?.jsonObject
        ?.kagariString("uri")

internal fun JsonContent.kagariStarterPackName(): String? = runCatching { decodeAs<JsonObject>().kagariString("name") }.getOrNull()

internal fun JsonContent.kagariStarterPackDescription(): String? =
    runCatching { decodeAs<JsonObject>().kagariString("description") }.getOrNull()
