package dev.dimension.flare.data.network.bluesky

import dev.dimension.flare.common.JSON
import dev.dimension.flare.data.network.ktorClient
import dev.dimension.flare.ui.model.KagariTokimekiPoll
import dev.dimension.flare.ui.model.KagariTokimekiPollOption
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal object KagariTokimekiPollClient {
    private const val ORIGIN = "https://poll.tokimeki.tech"
    private const val COLLECTION = "tech.tokimeki.poll.poll"
    private val cacheTtl = 1.minutes

    private val client by lazy { ktorClient() }
    private val pollCache = MutableStateFlow<Map<String, Pair<KagariTokimekiPoll, Instant>>>(emptyMap())

    private data class Option(
        val index: Int,
        val text: String,
        val count: Int,
    )

    suspend fun poll(
        did: String,
        rkey: String,
        url: String,
        viewer: String,
    ): KagariTokimekiPoll? {
        val uri = "at://$did/$COLLECTION/$rkey"
        val cacheKey = "$viewer $uri"
        val now = Clock.System.now()
        pollCache.value[cacheKey]
            ?.takeIf { now - it.second < cacheTtl }
            ?.let { return it.first }
        val requestUrl =
            URLBuilder(ORIGIN)
                .apply {
                    appendPathSegments("xrpc", "tech.tokimeki.poll.getPoll")
                    parameters.append("uri", uri)
                    parameters.append("viewer", viewer)
                }.build()
        val response = client.get(requestUrl)
        if (response.status != HttpStatusCode.OK) {
            return null
        }
        val root = JSON.parseToJsonElement(response.bodyAsText()) as? JsonObject ?: return null
        val poll = root["poll"] as? JsonObject ?: return null
        val options =
            (root["options"] as? JsonArray)
                ?.mapNotNull { (it as? JsonObject)?.toOption() }
                ?.sortedBy { it.index }
                .orEmpty()
        if (options.isEmpty()) {
            return null
        }
        val myVote = root.kagariTokimekiInt("myVote")
        val totalVotes = root.kagariTokimekiInt("totalVotes") ?: options.sumOf { it.count }
        val endsAt = poll.kagariTokimekiString("endsAt")?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val isEnded =
            (root["isEnded"] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull == true ||
                (endsAt != null && endsAt <= now)
        val authorDid = poll.kagariTokimekiString("authorDid")
        val result =
            KagariTokimekiPoll(
                uri = uri,
                url = url,
                options =
                    options
                        .map { option ->
                            KagariTokimekiPollOption(
                                text = option.text,
                                count = option.count,
                                isMine = option.index == myVote,
                            )
                        }.toImmutableList(),
                totalVotes = totalVotes,
                isEnded = isEnded,
                endsAt = endsAt,
                showsResults = isEnded || myVote != null || authorDid == viewer,
            )
        pollCache.update { it + (cacheKey to (result to now)) }
        return result
    }

    private fun JsonObject.toOption(): Option? {
        val index = kagariTokimekiInt("index") ?: return null
        val text = kagariTokimekiString("text") ?: return null
        val count = kagariTokimekiInt("count") ?: return null
        return Option(index = index, text = text, count = count)
    }

    private fun JsonObject.kagariTokimekiString(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.kagariTokimekiInt(key: String): Int? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
}
