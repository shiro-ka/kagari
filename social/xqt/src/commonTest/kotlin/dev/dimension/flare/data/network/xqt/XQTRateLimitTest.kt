package dev.dimension.flare.data.network.xqt

import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import dev.dimension.flare.data.network.xqt.api.createCardsApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class XQTRateLimitTest {
    @Test
    fun readsTheRemainingRequestsFromTheResponseHeaders() =
        runTest {
            assertEquals(42, remainingFor("42"))
            assertEquals(null, remainingFor(null))
            assertEquals(null, remainingFor(""))
        }

    private suspend fun remainingFor(remaining: String?): Int? {
        val engine =
            MockEngine {
                respond(
                    """{"card_uri":"card://1"}""",
                    HttpStatusCode.OK,
                    if (remaining == null) {
                        headersOf(HttpHeaders.ContentType, "application/json")
                    } else {
                        headersOf(
                            HttpHeaders.ContentType to listOf("application/json"),
                            "x-rate-limit-remaining" to listOf(remaining),
                        )
                    },
                )
            }
        return HttpClient(engine) { install(ContentNegotiation) { json(XQT_JSON) } }.use { client ->
            ktorfit {
                baseUrl("https://caps.example.com/")
                httpClient(client)
                converterFactories(ResponseConverterFactory())
            }.createCardsApi()
                .createCard("{}")
                .rateLimitRemaining
        }
    }
}
