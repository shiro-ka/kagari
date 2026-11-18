package dev.dimension.flare.data.network.bluesky

import io.ktor.client.plugins.api.createClientPlugin

internal class KagariLabelerProxyConfig {
    var labelerProxy: String? = null
}

internal val KagariLabelerProxyPlugin =
    createClientPlugin("KagariLabelerProxyPlugin", ::KagariLabelerProxyConfig) {
        val labelerProxy = pluginConfig.labelerProxy
        onRequest { request, _ ->
            if (labelerProxy != null && request.url.pathSegments.lastOrNull() == "com.atproto.moderation.createReport") {
                request.headers["Atproto-Proxy"] = labelerProxy
            }
        }
    }
