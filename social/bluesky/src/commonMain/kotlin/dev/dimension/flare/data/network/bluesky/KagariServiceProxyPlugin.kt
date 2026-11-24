package dev.dimension.flare.data.network.bluesky

import io.ktor.client.plugins.api.createClientPlugin

internal class KagariServiceProxyConfig {
    var labelerProxy: String? = null
    var feedGeneratorProxy: String? = null
}

internal val KagariServiceProxyPlugin =
    createClientPlugin("KagariServiceProxyPlugin", ::KagariServiceProxyConfig) {
        val labelerProxy = pluginConfig.labelerProxy
        val feedGeneratorProxy = pluginConfig.feedGeneratorProxy
        onRequest { request, _ ->
            when (request.url.pathSegments.lastOrNull()) {
                "com.atproto.moderation.createReport" -> {
                    if (labelerProxy != null) request.headers["Atproto-Proxy"] = labelerProxy
                }

                "app.bsky.feed.sendInteractions" -> {
                    if (feedGeneratorProxy != null) request.headers["Atproto-Proxy"] = feedGeneratorProxy
                }
            }
        }
    }
