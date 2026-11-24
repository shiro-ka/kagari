package dev.dimension.flare.data.network.bluesky

import com.sun.net.httpserver.HttpServer
import dev.dimension.flare.data.datasource.bluesky.kagariSendFeedFeedback
import dev.dimension.flare.di.startProxyTestStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.net.InetSocketAddress
import java.util.Collections
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KagariFeedFeedbackProxyTest {
    private lateinit var proxyStore: AutoCloseable

    @BeforeTest fun setUpProxy() {
        proxyStore = startProxyTestStore()
    }

    @AfterTest fun tearDownProxy() {
        proxyStore.close()
    }

    @Test
    fun feedbackGoesToTheFeedGeneratorWithTheFeedContext() =
        runBlocking {
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            val proxies = Collections.synchronizedList(mutableListOf<String?>())
            val bodies = Collections.synchronizedList(mutableListOf<String>())
            server.createContext("/xrpc/app.bsky.feed.sendInteractions") { exchange ->
                proxies += exchange.requestHeaders.getFirst("Atproto-Proxy")
                bodies += exchange.requestBody.readBytes().decodeToString()
                val body = "{}".toByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
            }
            server.start()
            try {
                withTimeout(10_000) {
                    BlueskyService("http://127.0.0.1:${server.address.port}").kagariSendFeedFeedback(
                        feedUri = "at://did:plc:owner/app.bsky.feed.generator/whats-hot",
                        feedServiceDid = "did:web:discover.bsky.app",
                        postUri = "at://did:plc:author/app.bsky.feed.post/1",
                        feedContext = "context",
                        reqId = "request",
                        more = false,
                    )
                }
                assertEquals(listOf<String?>("did:web:discover.bsky.app#bsky_fg"), proxies.toList())
                val body = bodies.single()
                assertTrue(body.contains("\"app.bsky.feed.defs#requestLess\""))
                assertTrue(body.contains("\"feedContext\":\"context\""))
                assertTrue(body.contains("\"reqId\":\"request\""))
                assertTrue(body.contains("\"feed\":\"at://did:plc:owner/app.bsky.feed.generator/whats-hot\""))
            } finally {
                server.stop(0)
            }
        }
}
