package dev.dimension.flare.data.datasource.bluesky

import app.bsky.actor.MutedWord
import app.bsky.actor.MutedWordActorTarget
import app.bsky.actor.MutedWordTarget
import app.bsky.actor.MutedWordsPref
import app.bsky.actor.PreferencesUnion
import app.bsky.actor.PutPreferencesRequest
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import sh.christian.ozone.api.model.JsonContent
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

public data class KagariMuteWord(
    val id: String?,
    val value: String,
    val tagOnly: Boolean,
    val excludeFollowing: Boolean,
    val expiresAt: Instant?,
)

private val kagariMuteWords = MutableStateFlow<Map<MicroBlogKey, List<KagariMuteWord>>>(emptyMap())

internal fun kagariMuteWordsFor(accountKey: MicroBlogKey): List<KagariMuteWord> = kagariMuteWords.value[accountKey].orEmpty()

internal suspend fun BlueskyService.kagariLoadMuteWords(accountKey: MicroBlogKey): List<KagariMuteWord> {
    val words =
        getPreferences()
            .requireResponse()
            .preferences
            .filterIsInstance<PreferencesUnion.MutedWordsPref>()
            .flatMap { it.value.items }
            .map { word ->
                KagariMuteWord(
                    id = word.id,
                    value = word.value,
                    tagOnly = word.targets.none { it is MutedWordTarget.Content },
                    excludeFollowing = word.actorTarget is MutedWordActorTarget.ExcludeFollowing,
                    expiresAt = word.expiresAt,
                )
            }
    kagariMuteWords.update { it + (accountKey to words) }
    return words
}

internal suspend fun BlueskyService.kagariSaveMuteWords(
    accountKey: MicroBlogKey,
    words: List<KagariMuteWord>,
) {
    val others =
        getPreferences()
            .requireResponse()
            .preferences
            .filterNot { it is PreferencesUnion.MutedWordsPref }
    val pref =
        PreferencesUnion.MutedWordsPref(
            MutedWordsPref(
                items =
                    words
                        .map { word ->
                            MutedWord(
                                id = word.id ?: Uuid.random().toString(),
                                value = word.value,
                                targets =
                                    if (word.tagOnly) {
                                        listOf<MutedWordTarget>(MutedWordTarget.Tag).toImmutableList()
                                    } else {
                                        listOf(MutedWordTarget.Content, MutedWordTarget.Tag).toImmutableList()
                                    },
                                actorTarget = if (word.excludeFollowing) MutedWordActorTarget.ExcludeFollowing else MutedWordActorTarget.All,
                                expiresAt = word.expiresAt,
                            )
                        }.toImmutableList(),
            ),
        )
    putPreferences(PutPreferencesRequest(preferences = (others + pref).toImmutableList()))
    kagariMuteWords.update { it + (accountKey to words) }
}

private val kagariSpacelessLanguages = setOf("ja", "zh", "ko", "th", "vi")
private val kagariWordBoundary = Regex("[\\s]+")
private val kagariEdgePunctuation = Regex("(?:^\\p{P}+|\\p{P}+$)")
private val kagariPunctuation = Regex("\\p{P}+")
private val kagariSpaceOrPunctuation = Regex("(?:\\s|\\p{P})+?")

internal fun kagariMatchMuteWord(
    words: List<KagariMuteWord>,
    record: JsonContent,
    languages: List<String>,
    authorFollowed: Boolean,
): String? {
    if (words.isEmpty()) return null
    val json = runCatching { record.decodeAs<JsonObject>() }.getOrNull() ?: return null
    val text = (json["text"] as? JsonPrimitive)?.contentOrNull.orEmpty().lowercase()
    val tags =
        buildList {
            runCatching { json["tags"]?.jsonArray }.getOrNull()?.forEach { tag ->
                (tag as? JsonPrimitive)?.contentOrNull?.let { add(it.lowercase()) }
            }
            runCatching { json["facets"]?.jsonArray }.getOrNull()?.forEach { facet ->
                runCatching { facet.jsonObject["features"]?.jsonArray }.getOrNull()?.forEach { feature ->
                    val obj = runCatching { feature.jsonObject }.getOrNull() ?: return@forEach
                    if (obj["\$type"]?.jsonPrimitive?.contentOrNull == "app.bsky.richtext.facet#tag") {
                        obj["tag"]?.jsonPrimitive?.contentOrNull?.let { add(it.lowercase()) }
                    }
                }
            }
        }
    val exception = languages.firstOrNull()?.substringBefore('-')?.lowercase() in kagariSpacelessLanguages
    val now = Clock.System.now()
    for (word in words) {
        val muted = word.value.lowercase().removePrefix("#")
        if (muted.isEmpty()) continue
        if (word.expiresAt != null && word.expiresAt < now) continue
        if (word.excludeFollowing && authorFollowed) continue
        if (muted in tags) return word.value
        if (word.tagOnly) continue
        if ((muted.length == 1 || exception) && text.contains(muted)) return word.value
        if (muted.length > text.length) continue
        if (muted == text) return word.value
        if (kagariSpaceOrPunctuation.containsMatchIn(muted) && text.contains(muted)) return word.value
        for (candidate in text.split(kagariWordBoundary)) {
            if (candidate == muted) return word.value
            val trimmed = candidate.replace(kagariEdgePunctuation, "")
            if (trimmed == muted) return word.value
            if (muted.length > trimmed.length) continue
            if (kagariPunctuation.containsMatchIn(trimmed)) {
                if (trimmed.contains('/')) break
                val spaced = trimmed.replace(kagariPunctuation, " ")
                if (spaced == muted) return word.value
                if (spaced.replace(" ", "") == muted) return word.value
                if (trimmed.split(kagariPunctuation).any { it == muted }) return word.value
            }
        }
    }
    return null
}
