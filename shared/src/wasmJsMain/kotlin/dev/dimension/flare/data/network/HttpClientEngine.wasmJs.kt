package dev.dimension.flare.data.network

import dev.dimension.flare.data.model.ProxyConnection
import dev.dimension.flare.data.model.ProxyMode
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineCapability
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.sse.SSECapability
import io.ktor.client.plugins.websocket.WebSocketCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.Headers
import io.ktor.utils.io.InternalAPI

internal actual val platformEngineCapabilities: Set<HttpClientEngineCapability<*>> =
    setOf(HttpTimeoutCapability, WebSocketCapability, SSECapability)

internal actual fun createPlatformHttpClientEngine(
    connection: ProxyConnection,
    plugin: Boolean,
): HttpClientEngine {
    check(connection.mode == ProxyMode.SYSTEM) { "App proxies are not supported in a browser" }
    val engine = Js.create()
    return if (isBrowserExtension()) ExtensionHeaderEngine(engine) else engine
}

private fun isBrowserExtension(): Boolean = js("globalThis.location?.protocol === 'moz-extension:'")

private const val EXTENSION_HEADER_PREFIX = "x-kagari-h-"

private val carriedHeaders = setOf("cookie", "dnt", "origin", "referer")

private fun carriedHeaderName(name: String): String? {
    val lower = name.lowercase()
    return if (lower in carriedHeaders || lower.startsWith("sec-")) EXTENSION_HEADER_PREFIX + lower else null
}

@OptIn(InternalAPI::class)
private class ExtensionHeaderEngine(
    private val delegate: HttpClientEngine,
) : HttpClientEngineBase("kagari-extension") {
    override val config: HttpClientEngineConfig get() = delegate.config
    override val supportedCapabilities: Set<HttpClientEngineCapability<*>> get() = delegate.supportedCapabilities

    override fun close() {
        super.close()
        delegate.close()
    }

    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        if (data.headers.names().none { carriedHeaderName(it) != null }) return delegate.execute(data)
        val headers =
            Headers.build {
                data.headers.forEach { name, values ->
                    appendAll(carriedHeaderName(name) ?: name, values)
                }
            }
        return delegate.execute(
            HttpRequestData(
                data.url,
                data.method,
                headers,
                data.body,
                data.executionContext,
                data.attributes,
            ),
        )
    }
}
