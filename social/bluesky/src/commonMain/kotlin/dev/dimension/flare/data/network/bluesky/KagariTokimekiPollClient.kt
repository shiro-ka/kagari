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

    data class LocalVote(
        val index: Int?,
        val uri: String?,
    )

    val localVotes = MutableStateFlow<Map<String, LocalVote>>(emptyMap())

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
        val cid = poll.kagariTokimekiString("cid") ?: return null
        val myVote =
            root.kagariTokimekiInt("myVote")?.let { vote ->
                options.indexOfFirst { it.index == vote }.takeIf { it >= 0 }
            }
        val totalVotes = root.kagariTokimekiInt("totalVotes") ?: options.sumOf { it.count }
        val endsAt = poll.kagariTokimekiString("endsAt")?.let { runCatching { Instant.parse(it) }.getOrNull() }
        val isEnded =
            (root["isEnded"] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull == true ||
                (endsAt != null && endsAt <= now)
        val result =
            KagariTokimekiPoll(
                uri = uri,
                cid = cid,
                url = url,
                options =
                    options
                        .map { option -> KagariTokimekiPollOption(index = option.index, text = option.text, count = option.count) }
                        .toImmutableList(),
                totalVotes = totalVotes,
                isEnded = isEnded,
                endsAt = endsAt,
                isAuthor = poll.kagariTokimekiString("authorDid") == viewer,
                myVote = myVote,
                myVoteUri = root.kagariTokimekiString("myVoteUri")?.takeIf { myVote != null },
            )
        pollCache.update { it + (cacheKey to (result to now)) }
        return result
    }

    fun localVoteKey(
        viewer: String,
        pollUri: String,
    ): String = "$viewer $pollUri"

    fun setLocalVote(
        key: String,
        vote: LocalVote?,
    ) {
        localVotes.update { if (vote == null) it - key else it + (key to vote) }
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

internal fun KagariTokimekiPoll.kagariWithVote(vote: KagariTokimekiPollClient.LocalVote): KagariTokimekiPoll {
    val index = vote.index?.takeIf { it in options.indices }
    val serverVote = myVote
    return copy(
        options =
            options
                .mapIndexed { i, option ->
                    val count = option.count - (if (i == serverVote) 1 else 0) + (if (i == index) 1 else 0)
                    option.copy(count = count.coerceAtLeast(0))
                }.toImmutableList(),
        totalVotes = (totalVotes - (if (serverVote != null) 1 else 0) + (if (index != null) 1 else 0)).coerceAtLeast(0),
        myVote = index,
        myVoteUri = vote.uri.takeIf { index != null },
    )
}
