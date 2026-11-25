package dev.dimension.flare.data.network.xqt

import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import dev.dimension.flare.data.network.xqt.api.V11PostApi
import dev.dimension.flare.data.network.xqt.api.createV11PostApi
import dev.dimension.flare.data.network.xqt.api.kagariSetConversationMuted
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class KagariConversationMuteTest {
    @Test
    fun mutingAndUnmutingPostTheTweetIdAsAForm() =
        runTest {
            listOf(
                true to "/1.1/mutes/conversations/create.json",
                false to "/1.1/mutes/conversations/destroy.json",
            ).forEach { (mute, path) ->
                withV11PostApi(
                    MockEngine { request ->
                        assertEquals(HttpMethod.Post, request.method)
                        assertEquals(path, request.url.encodedPath)
                        val form = assertIs<FormDataContent>(request.body).formData
                        assertEquals("42", form["tweet_id"])
                        assertEquals("extended", form["tweet_mode"])
                        respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                    },
                ) { it.kagariSetConversationMuted("42", mute) }
            }
        }

    @Test
    fun httpRejectionFails() =
        runTest {
            withV11PostApi(
                MockEngine { respond("{}", HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/json")) },
            ) { api ->
                assertFailsWith<IllegalStateException> { api.kagariSetConversationMuted("42", mute = true) }
            }
        }

    private suspend fun withV11PostApi(
        engine: MockEngine,
        block: suspend (V11PostApi) -> Unit,
    ) {
        HttpClient(engine) { install(ContentNegotiation) { json(XQT_JSON) } }.use { client ->
            block(
                ktorfit {
                    baseUrl("https://api.example.com/")
                    httpClient(client)
                    converterFactories(ResponseConverterFactory())
                }.createV11PostApi(),
            )
        }
    }
}
