package dev.dimension.flare.data.network.xqt

import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import dev.dimension.flare.data.datasource.microblog.ComposeData
import dev.dimension.flare.data.network.xqt.api.CardsApi
import dev.dimension.flare.data.network.xqt.api.XqtPollResult
import dev.dimension.flare.data.network.xqt.api.createCardsApi
import dev.dimension.flare.data.network.xqt.api.createPoll
import dev.dimension.flare.data.network.xqt.api.votePoll
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
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class PollCardTest {
    @Test
    fun createsCardWithTrimmedChoicesAndWholeMinutes() =
        runTest {
            withCardsApi(
                MockEngine { request ->
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("/v2/cards/create.json", request.url.encodedPath)
                    val form = assertIs<FormDataContent>(request.body)
                    val data = XQT_JSON.parseToJsonElement(assertNotNull(form.formData["card_data"])).jsonObject
                    assertEquals(
                        XQT_JSON.parseToJsonElement(
                            """{"twitter:card":"poll2choice_text_only","twitter:api:api:endpoint":"1","twitter:long:duration_minutes":60,"twitter:string:choice1_label":"A & B","twitter:string:choice2_label":"C"}""",
                        ),
                        data,
                    )
                    respond(
                        """{"card_uri":"card://1234"}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) { api ->
                assertEquals("card://1234", api.createPoll(ComposeData.Poll(listOf(" A & B ", "C", " "), 3_600_000L, false)))
            }
        }

    @Test
    fun invalidPollsAreRejectedBeforeNetworkRequests() =
        runTest {
            withCardsApi(MockEngine { error("Invalid polls must not reach X") }) { api ->
                val poll = ComposeData.Poll(listOf("A", "B"), 300_000L, false)
                listOf(
                    poll.copy(multiple = true),
                    poll.copy(options = listOf("A", " ")),
                    poll.copy(options = listOf("A", "B", "C", "D", "E")),
                    poll.copy(options = listOf("A", "界".repeat(26))),
                    poll.copy(expiredAfter = 240_000L),
                    poll.copy(expiredAfter = 300_001L),
                    poll.copy(expiredAfter = 604_860_000L),
                ).forEach { invalid ->
                    assertFailsWith<IllegalArgumentException> { api.createPoll(invalid) }
                }
            }
        }

    @Test
    fun rejectsUnconfirmedCardCreation() =
        runTest {
            withCardsApi(MockEngine { respond("{}", headers = headersOf(HttpHeaders.ContentType, "application/json")) }) { api ->
                assertFailsWith<IllegalStateException> { api.createPoll(ComposeData.Poll(listOf("A", "B"), 300_000L, false)) }
            }
        }

    @Test
    fun votingPostsTheChoiceToPassthroughAndReadsTheReturnedCounts() =
        runTest {
            withCardsApi(
                MockEngine { request ->
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("/v2/capi/passthrough/1", request.url.encodedPath)
                    val form = assertIs<FormDataContent>(request.body).formData
                    assertEquals("card://9", form["twitter:string:card_uri"])
                    assertEquals("42", form["twitter:long:original_tweet_id"])
                    assertEquals("poll2choice_text_only", form["twitter:string:response_card_name"])
                    assertEquals("Web-12", form["twitter:string:cards_platform"])
                    assertEquals("2", form["twitter:string:selected_choice"])
                    respond(
                        """{"card":{"name":"poll2choice_text_only","binding_values":{"choice1_label":{"type":"STRING","string_value":"A"},"choice1_count":{"type":"STRING","string_value":"3"},"choice2_label":{"type":"STRING","string_value":"B"},"choice2_count":{"type":"STRING","string_value":"2"},"selected_choice":{"type":"STRING","string_value":"2"}}}}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) { api ->
                assertEquals(
                    XqtPollResult(counts = listOf(3L, 2L), selected = 1),
                    api.votePoll(cardUri = "card://9", tweetId = "42", cardName = "poll2choice_text_only", choice = 1),
                )
            }
        }

    @Test
    fun rejectedVoteFails() =
        runTest {
            withCardsApi(MockEngine { respond("{}", HttpStatusCode.BadRequest, headersOf(HttpHeaders.ContentType, "application/json")) }) { api ->
                assertFailsWith<IllegalStateException> {
                    api.votePoll(cardUri = "card://9", tweetId = "42", cardName = "poll2choice_text_only", choice = 0)
                }
            }
        }

    private suspend fun withCardsApi(
        engine: MockEngine,
        block: suspend (CardsApi) -> Unit,
    ) {
        HttpClient(engine) { install(ContentNegotiation) { json(XQT_JSON) } }.use { client ->
            block(
                ktorfit {
                    baseUrl("https://caps.example.com/")
                    httpClient(client)
                    converterFactories(ResponseConverterFactory())
                }.createCardsApi(),
            )
        }
    }
}
