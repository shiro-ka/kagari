package dev.dimension.flare.data.network.xqt

import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import dev.dimension.flare.data.network.xqt.api.PostApi
import dev.dimension.flare.data.network.xqt.api.createPostApi
import dev.dimension.flare.data.network.xqt.api.kagariSetPinned
import dev.dimension.flare.data.network.xqt.api.kagariSetReplyHidden
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class KagariMutationTest {
    @Test
    fun hidingAReplySendsModerateTweet() =
        runTest {
            withPostApi(
                MockEngine { request ->
                    request.assertGraphql(
                        path = "/graphql/${XQTMutationQueryIds.MODERATE_TWEET}/ModerateTweet",
                        body = """{"variables":{"tweetId":"42"},"queryId":"${XQTMutationQueryIds.MODERATE_TWEET}"}""",
                    )
                    ok()
                },
            ) { it.kagariSetReplyHidden("42", hide = true) }
        }

    @Test
    fun showingAReplySendsUnmoderateTweet() =
        runTest {
            withPostApi(
                MockEngine { request ->
                    request.assertGraphql(
                        path = "/graphql/${XQTMutationQueryIds.UNMODERATE_TWEET}/UnmoderateTweet",
                        body = """{"variables":{"tweetId":"42"},"queryId":"${XQTMutationQueryIds.UNMODERATE_TWEET}"}""",
                    )
                    ok()
                },
            ) { it.kagariSetReplyHidden("42", hide = false) }
        }

    @Test
    fun pinningSendsPinTweetAndUnpinningSendsUnpinTweet() =
        runTest {
            listOf(
                true to "/graphql/${XQTMutationQueryIds.PIN_TWEET}/PinTweet",
                false to "/graphql/${XQTMutationQueryIds.UNPIN_TWEET}/UnpinTweet",
            ).forEach { (pin, path) ->
                val queryId = if (pin) XQTMutationQueryIds.PIN_TWEET else XQTMutationQueryIds.UNPIN_TWEET
                withPostApi(
                    MockEngine { request ->
                        request.assertGraphql(
                            path = path,
                            body = """{"variables":{"tweet_id":"42"},"queryId":"$queryId"}""",
                        )
                        ok()
                    },
                ) { it.kagariSetPinned("42", pin) }
            }
        }

    @Test
    fun graphqlErrorsFailTheMutation() =
        runTest {
            withPostApi(MockEngine { ok("""{"errors":[{"message":"nope"}]}""") }) { api ->
                assertFailsWith<IllegalStateException> { api.kagariSetReplyHidden("42", hide = true) }
            }
        }

    @Test
    fun httpRejectionFailsTheMutation() =
        runTest {
            withPostApi(MockEngine { ok("{}", HttpStatusCode.BadRequest) }) { api ->
                assertFailsWith<IllegalStateException> { api.kagariSetReplyHidden("42", hide = true) }
            }
        }

    private fun HttpRequestData.assertGraphql(
        path: String,
        body: String,
    ) {
        assertEquals(HttpMethod.Post, method)
        assertEquals(path, url.encodedPath)
        assertEquals(
            XQT_JSON.parseToJsonElement(body),
            XQT_JSON.parseToJsonElement(assertIs<TextContent>(this.body).text),
        )
    }

    private fun MockRequestHandleScope.ok(
        body: String = """{"data":{}}""",
        status: HttpStatusCode = HttpStatusCode.OK,
    ): HttpResponseData = respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private suspend fun withPostApi(
        engine: MockEngine,
        block: suspend (PostApi) -> Unit,
    ) {
        HttpClient(engine) { install(ContentNegotiation) { json(XQT_JSON) } }.use { client ->
            block(
                ktorfit {
                    baseUrl("https://api.example.com/")
                    httpClient(client)
                    converterFactories(ResponseConverterFactory())
                }.createPostApi(),
            )
        }
    }
}
