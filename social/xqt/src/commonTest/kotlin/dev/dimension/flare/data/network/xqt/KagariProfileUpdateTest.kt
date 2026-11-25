package dev.dimension.flare.data.network.xqt

import de.jensklingenberg.ktorfit.converter.ResponseConverterFactory
import de.jensklingenberg.ktorfit.ktorfit
import dev.dimension.flare.data.network.xqt.api.V11PostApi
import dev.dimension.flare.data.network.xqt.api.createV11PostApi
import dev.dimension.flare.data.network.xqt.api.kagariRemoveProfileBanner
import dev.dimension.flare.data.network.xqt.api.kagariUpdateProfile
import dev.dimension.flare.data.network.xqt.api.kagariUpdateProfileBanner
import dev.dimension.flare.data.network.xqt.api.kagariUpdateProfileImage
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
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

class KagariProfileUpdateTest {
    @Test
    fun updatingTheProfileSendsEveryTextFieldAsAForm() =
        runTest {
            withV11PostApi(
                MockEngine { request ->
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("/1.1/account/update_profile.json", request.url.encodedPath)
                    val form = assertIs<FormDataContent>(request.body).formData
                    assertEquals("しろか", form["name"])
                    assertEquals("a & b https://example.com", form["description"])
                    assertEquals("", form["location"])
                    assertEquals("https://shiro-ka.github.io", form["url"])
                    ok()
                },
            ) {
                it.kagariUpdateProfile(
                    name = "しろか",
                    description = "a & b https://example.com",
                    location = "",
                    url = "https://shiro-ka.github.io",
                )
            }
        }

    @Test
    fun imageAndBannerSendTheMediaIdAndAskForTheUser() =
        runTest {
            listOf(
                true to "/1.1/account/update_profile_image.json",
                false to "/1.1/account/update_profile_banner.json",
            ).forEach { (avatar, path) ->
                withV11PostApi(
                    MockEngine { request ->
                        assertEquals(HttpMethod.Post, request.method)
                        assertEquals(path, request.url.encodedPath)
                        val form = assertIs<FormDataContent>(request.body).formData
                        assertEquals("123", form["media_id"])
                        assertEquals("true", form["return_user"])
                        assertEquals("1", form["skip_status"])
                        ok()
                    },
                ) { if (avatar) it.kagariUpdateProfileImage("123") else it.kagariUpdateProfileBanner("123") }
            }
        }

    @Test
    fun removingTheBannerPostsToItsOwnEndpoint() =
        runTest {
            withV11PostApi(
                MockEngine { request ->
                    assertEquals(HttpMethod.Post, request.method)
                    assertEquals("/1.1/account/remove_profile_banner.json", request.url.encodedPath)
                    ok()
                },
            ) { it.kagariRemoveProfileBanner() }
        }

    @Test
    fun httpRejectionFails() =
        runTest {
            withV11PostApi(MockEngine { ok(HttpStatusCode.Forbidden) }) { api ->
                assertFailsWith<IllegalStateException> { api.kagariUpdateProfile("a", "", "", "") }
                assertFailsWith<IllegalStateException> { api.kagariUpdateProfileImage("1") }
                assertFailsWith<IllegalStateException> { api.kagariUpdateProfileBanner("1") }
                assertFailsWith<IllegalStateException> { api.kagariRemoveProfileBanner() }
            }
        }

    private fun MockRequestHandleScope.ok(status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
        respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))

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
