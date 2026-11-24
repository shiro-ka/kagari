package dev.dimension.flare.data.network.misskey

import dev.dimension.flare.common.JSON
import dev.dimension.flare.common.Locale
import dev.dimension.flare.data.model.ProxyRef
import dev.dimension.flare.data.network.ktorClient
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiCard
import dev.dimension.flare.ui.model.toUiImage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.content.TextContent
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

internal object KagariMisskeyUrlPreviewClient {
    private const val MAX_PREVIEWS = 500
    private val previewTtl = 30.minutes
    private val metaTtl = 6.hours
    private val youtubeMusicPath = Regex("^/(?:watch|channel)")

    private val clients = MutableStateFlow<Map<MicroBlogKey, HttpClient>>(emptyMap())
    private val enabled = MutableStateFlow<Map<String, Pair<Boolean, Instant>>>(emptyMap())
    private val previews = MutableStateFlow<Map<String, Pair<UiCard?, Instant>>>(emptyMap())

    suspend fun preview(
        accountKey: MicroBlogKey,
        url: String,
    ): UiCard? {
        val host = accountKey.host
        val lang = Locale.language
        val cacheKey = "$host\n$lang\n$url"
        val now = Clock.System.now()
        previews.value[cacheKey]
            ?.takeIf { now - it.second < previewTtl }
            ?.let { return it.first }
        val client = client(accountKey)
        if (!urlPreviewEnabled(client, host)) {
            return null
        }
        val card = fetch(client, accountKey, host, url, lang)
        previews.update { current ->
            val next = current + (cacheKey to (card to now))
            if (next.size > MAX_PREVIEWS) {
                next.entries
                    .sortedByDescending { it.value.second }
                    .take(MAX_PREVIEWS)
                    .associate { it.key to it.value }
            } else {
                next
            }
        }
        return card
    }

    private fun client(accountKey: MicroBlogKey): HttpClient {
        clients.value[accountKey]?.let { return it }
        val created = ktorClient(ProxyRef.forAccount(accountKey))
        var result = created
        clients.update { current ->
            val existing = current[accountKey]
            if (existing != null) {
                result = existing
                current
            } else {
                result = created
                current + (accountKey to created)
            }
        }
        if (result !== created) {
            created.close()
        }
        return result
    }

    private suspend fun urlPreviewEnabled(
        client: HttpClient,
        host: String,
    ): Boolean {
        val now = Clock.System.now()
        enabled.value[host]
            ?.takeIf { now - it.second < metaTtl }
            ?.let { return it.first }
        val value =
            try {
                val response =
                    client.post("https://$host/api/meta") {
                        setBody(TextContent("{\"detail\":false}", ContentType.Application.Json))
                    }
                if (response.status == HttpStatusCode.OK) {
                    val meta = JSON.parseToJsonElement(response.bodyAsText()) as? JsonObject
                    (meta?.get("enableUrlPreview") as? JsonPrimitive)?.booleanOrNull != false
                } else {
                    true
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                true
            }
        enabled.update { it + (host to (value to now)) }
        return value
    }

    private suspend fun fetch(
        client: HttpClient,
        accountKey: MicroBlogKey,
        host: String,
        url: String,
        lang: String,
    ): UiCard? =
        try {
            val target =
                URLBuilder(url)
                    .apply {
                        if (this.host == "music.youtube.com" && youtubeMusicPath.containsMatchIn(encodedPath)) {
                            this.host = "www.youtube.com"
                        }
                        fragment = ""
                    }.buildString()
            val request =
                URLBuilder(
                    protocol = URLProtocol.HTTPS,
                    host = host,
                ).apply {
                    encodedPath = "/url"
                    parameters.append("url", target)
                    parameters.append("lang", lang)
                }.build()
            val response = client.get(request)
            if (response.status == HttpStatusCode.OK) {
                (JSON.parseToJsonElement(response.bodyAsText()) as? JsonObject)?.toCard(accountKey, url)
            } else {
                null
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }

    private fun JsonObject.toCard(
        accountKey: MicroBlogKey,
        url: String,
    ): UiCard? {
        if (string("url") == null) {
            return null
        }
        val title = string("title") ?: string("sitename") ?: return null
        val sensitive = (get("sensitive") as? JsonPrimitive)?.booleanOrNull == true
        return UiCard(
            title = title,
            description = string("description"),
            media =
                string("thumbnail")
                    ?.takeUnless { sensitive }
                    ?.toUiImage(proxy = ProxyRef.forAccount(accountKey)),
            url = url,
        )
    }

    private fun JsonObject.string(key: String): String? =
        (get(key) as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
}
