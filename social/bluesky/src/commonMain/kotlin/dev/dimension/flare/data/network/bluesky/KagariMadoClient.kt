package dev.dimension.flare.data.network.bluesky

import dev.dimension.flare.common.JSON
import dev.dimension.flare.data.network.ktorClient
import dev.dimension.flare.ui.model.KagariMadoAnswer
import dev.dimension.flare.ui.model.KagariMadoBox
import dev.dimension.flare.ui.model.KagariMadoProfile
import dev.dimension.flare.ui.model.KagariMadoVoice
import dev.dimension.flare.ui.render.toUi
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.appendPathSegments
import io.ktor.http.encodeURLPathPart
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal object KagariMadoClient {
    private const val BOX = "blue.mado.box"
    private const val KOE = "blue.mado.koe"
    private const val ANSWER = "blue.mado.answer"
    private const val KOE_MAX_BYTES = 3000
    private const val ANSWER_MAX_BYTES = 6000
    private const val MAX_PAGES = 50
    private const val MADO_ORIGIN = "https://mado.blue"
    private val cacheTtl = 10.minutes

    private val client by lazy { ktorClient() }
    private val pdsCache = MutableStateFlow<Map<String, Pair<String, Instant>>>(emptyMap())
    private val voiceCache = MutableStateFlow<Map<String, Pair<KagariMadoVoice, Instant>>>(emptyMap())

    private data class Record(
        val uri: String,
        val rkey: String,
        val value: JsonObject,
    )

    private data class Box(
        val uri: String,
        val slug: String,
        val title: String,
        val description: String?,
        val isOpen: Boolean,
        val createdAt: Instant,
    )

    private data class Koe(
        val uri: String,
        val rkey: String,
        val body: String,
        val box: String,
    )

    private data class Answer(
        val koe: String,
        val body: String,
        val createdAt: Instant,
    )

    suspend fun hasBoxes(did: String): Boolean = boxes(did).isNotEmpty()

    suspend fun profile(
        did: String,
        handle: String,
    ): KagariMadoProfile {
        val boxes = boxes(did)
        val boxByUri = boxes.associateBy { it.uri }
        val answersByKoe = answers(did).groupBy { it.koe }
        val voices =
            listRecords(did, KOE)
                .mapNotNull { it.toKoe() }
                .mapNotNull { koe ->
                    val box = boxByUri[koe.box] ?: return@mapNotNull null
                    voice(koe, box, answersByKoe[koe.uri].orEmpty(), handle)
                }.sortedByDescending { it.answeredAt.value }
        return KagariMadoProfile(
            boxes =
                boxes
                    .sortedBy { it.createdAt }
                    .map { box ->
                        KagariMadoBox(
                            uri = box.uri,
                            slug = box.slug,
                            title = box.title,
                            description = box.description,
                            isOpen = box.isOpen,
                            createdAt = box.createdAt.toUi(),
                            url = boxUrl(handle, box.slug),
                        )
                    }.toImmutableList(),
            voices = voices.toImmutableList(),
        )
    }

    suspend fun voice(
        did: String,
        handle: String,
        rkey: String,
    ): KagariMadoVoice? {
        val cacheKey = "$did/$rkey"
        val now = Clock.System.now()
        voiceCache.value[cacheKey]
            ?.takeIf { now - it.second < cacheTtl }
            ?.let { return it.first }
        val koe = getRecord(did, KOE, rkey)?.toKoe() ?: return null
        val koeAnswers = answers(did).filter { it.koe == koe.uri }
        if (koeAnswers.isEmpty()) {
            return null
        }
        val boxRef = parseAtUri(koe.box) ?: return null
        if (boxRef.second != BOX) {
            return null
        }
        val box = getRecord(boxRef.first, BOX, boxRef.third)?.toBox() ?: return null
        val result = voice(koe, box, koeAnswers, handle) ?: return null
        voiceCache.update { it + (cacheKey to (result to now)) }
        return result
    }

    private fun voice(
        koe: Koe,
        box: Box,
        answers: List<Answer>,
        handle: String,
    ): KagariMadoVoice? {
        val sorted = answers.sortedBy { it.createdAt }
        val latest = sorted.lastOrNull() ?: return null
        return KagariMadoVoice(
            uri = koe.uri,
            body = koe.body,
            boxTitle = box.title,
            answers =
                sorted
                    .map { KagariMadoAnswer(body = it.body, createdAt = it.createdAt.toUi()) }
                    .toImmutableList(),
            answeredAt = latest.createdAt.toUi(),
            url = "${boxUrl(handle, box.slug)}/${koe.rkey.encodeURLPathPart()}",
        )
    }

    private fun boxUrl(
        handle: String,
        slug: String,
    ): String = "$MADO_ORIGIN/@${handle.encodeURLPathPart()}/${slug.encodeURLPathPart()}"

    private suspend fun boxes(did: String): List<Box> = listRecords(did, BOX).mapNotNull { it.toBox() }

    private suspend fun answers(did: String): List<Answer> = listRecords(did, ANSWER).mapNotNull { it.toAnswer() }

    private fun Record.toBox(): Box? {
        val slug = value.kagariMadoString("slug")?.takeIf { it.isNotBlank() } ?: return null
        val title = value.kagariMadoString("title") ?: return null
        val isOpen = (value["isOpen"] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull ?: return null
        val createdAt = value.kagariMadoDate("createdAt") ?: return null
        val description =
            value["description"]?.takeIf { it !is JsonNull }?.let { element ->
                (element as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
            }
        return Box(
            uri = uri,
            slug = slug,
            title = title,
            description = description?.takeIf { it.isNotBlank() },
            isOpen = isOpen,
            createdAt = createdAt,
        )
    }

    private fun Record.toKoe(): Koe? {
        val body = value.kagariMadoString("body")?.takeIf { it.encodeToByteArray().size <= KOE_MAX_BYTES } ?: return null
        val box = value.kagariMadoString("box") ?: return null
        if (value.kagariMadoDate("createdAt") == null) {
            return null
        }
        return Koe(uri = uri, rkey = rkey, body = body, box = box)
    }

    private fun Record.toAnswer(): Answer? {
        val koe = value.kagariMadoString("koe") ?: return null
        val body = value.kagariMadoString("body")?.takeIf { it.encodeToByteArray().size <= ANSWER_MAX_BYTES } ?: return null
        val createdAt = value.kagariMadoDate("createdAt") ?: return null
        return Answer(koe = koe, body = body, createdAt = createdAt)
    }

    private fun JsonObject.kagariMadoString(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.kagariMadoDate(key: String): Instant? = kagariMadoString(key)?.let { runCatching { Instant.parse(it) }.getOrNull() }

    private fun parseAtUri(uri: String): Triple<String, String, String>? {
        val parts = uri.removePrefix("at://").takeIf { it != uri }?.split('/') ?: return null
        if (parts.size != 3 || parts.any { it.isEmpty() }) {
            return null
        }
        return Triple(parts[0], parts[1], parts[2])
    }

    private suspend fun pds(did: String): String? {
        val now = Clock.System.now()
        pdsCache.value[did]
            ?.takeIf { now - it.second < cacheTtl }
            ?.let { return it.first }
        val document = runCatching { getJson(didDocumentUrl(did)) }.getOrNull() as? JsonObject ?: return null
        val endpoint =
            (document["service"] as? JsonArray)
                ?.mapNotNull { it as? JsonObject }
                ?.firstOrNull { service ->
                    val id = service.kagariMadoString("id")
                    id == "#atproto_pds" || id == "$did#atproto_pds"
                }?.kagariMadoString("serviceEndpoint")
                ?.trimEnd('/')
                ?: return null
        val url = runCatching { Url(endpoint) }.getOrNull() ?: return null
        if (url.protocol != URLProtocol.HTTPS) {
            return null
        }
        pdsCache.update { it + (did to (endpoint to now)) }
        return endpoint
    }

    private suspend fun listRecords(
        did: String,
        collection: String,
    ): List<Record> {
        val pds = pds(did) ?: return emptyList()
        val records = mutableListOf<Record>()
        var cursor: String? = null
        repeat(MAX_PAGES) {
            val url =
                URLBuilder(pds)
                    .apply {
                        appendPathSegments("xrpc", "com.atproto.repo.listRecords")
                        parameters.append("repo", did)
                        parameters.append("collection", collection)
                        parameters.append("limit", "100")
                        cursor?.let { parameters.append("cursor", it) }
                    }.build()
            val response = runCatching { getJson(url) }.getOrNull() as? JsonObject ?: return records
            val page =
                (response["records"] as? JsonArray)
                    ?.mapNotNull { (it as? JsonObject)?.toRecord(collection) }
                    .orEmpty()
            records += page
            val next = response.kagariMadoString("cursor")
            if (next == null || next == cursor || (response["records"] as? JsonArray).isNullOrEmpty()) {
                return records
            }
            cursor = next
        }
        return records
    }

    private suspend fun getRecord(
        did: String,
        collection: String,
        rkey: String,
    ): Record? {
        val pds = pds(did) ?: return null
        val url =
            URLBuilder(pds)
                .apply {
                    appendPathSegments("xrpc", "com.atproto.repo.getRecord")
                    parameters.append("repo", did)
                    parameters.append("collection", collection)
                    parameters.append("rkey", rkey)
                }.build()
        return (runCatching { getJson(url) }.getOrNull() as? JsonObject)?.toRecord(collection)
    }

    private fun JsonObject.toRecord(collection: String): Record? {
        val uri = kagariMadoString("uri") ?: return null
        val parts = parseAtUri(uri) ?: return null
        if (parts.second != collection) {
            return null
        }
        val value = this["value"] as? JsonObject ?: return null
        return Record(uri = uri, rkey = parts.third, value = value)
    }

    private suspend fun getJson(url: Url): JsonElement? {
        val response = client.get(url)
        if (response.status != HttpStatusCode.OK) {
            return null
        }
        return JSON.parseToJsonElement(response.bodyAsText())
    }
}
