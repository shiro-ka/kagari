package dev.dimension.flare.data.platform

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import sh.christian.ozone.BlueskyJson
import sh.christian.ozone.api.Did
import sh.christian.ozone.oauth.DpopKeyPair
import sh.christian.ozone.oauth.OAuthScope
import sh.christian.ozone.oauth.OAuthToken
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class StoredOAuthTokenCompatibilityTest {
    @Test
    fun readsTokenSavedByOAuthTokenSerializer() =
        runTest {
            val keyPair = DpopKeyPair.generateKeyPair()
            val token = oauthToken(keyPair)
            val saved = BlueskyJson.encodeToJsonElement(OAuthToken.serializer(), token)

            val stored = BlueskyJson.decodeFromJsonElement(StoredOAuthToken.serializer(), saved)

            assertEquals(token.accessToken, stored.accessToken)
            assertEquals(token.refreshToken, stored.refreshToken)
            assertEquals(token.expiresIn, stored.expiresIn)
            assertEquals(token.scopes, stored.scopes)
            assertEquals(token.subject, stored.subject)
            assertEquals(token.nonce, stored.nonce)
            assertEquals(token.clientId, stored.clientId)
            assertEquals(token.pdsUrl, stored.pdsUrl)
            assertSameKeys(keyPair, stored.keyPair.toDpopKeyPair())
            assertEquals(saved, BlueskyJson.encodeToJsonElement(StoredOAuthToken.serializer(), stored))
        }

    @Test
    fun oauthTokenSerializerReadsStoredToken() =
        runTest {
            val keyPair = DpopKeyPair.generateKeyPair()
            val stored = oauthToken(keyPair).toStored()
            val saved = BlueskyJson.encodeToJsonElement(StoredOAuthToken.serializer(), stored)

            val token = BlueskyJson.decodeFromJsonElement(OAuthToken.serializer(), saved)

            assertEquals(stored.accessToken, token.accessToken)
            assertEquals(stored.pdsUrl, token.pdsUrl)
            assertSameKeys(keyPair, token.keyPair)
        }

    @Test
    fun readsCredentialSavedWithOAuthToken() =
        runTest {
            val keyPair = DpopKeyPair.generateKeyPair()
            val token = oauthToken(keyPair)
            val savedToken = BlueskyJson.encodeToJsonElement(OAuthToken.serializer(), token)
            val current =
                BlueskyJson
                    .encodeToJsonElement(
                        BlueskyCredential.serializer(),
                        BlueskyCredential.OAuthCredential(
                            baseUrl = "https://auth.example",
                            oAuthToken = token.toStored(),
                            pdsUrlVerified = true,
                            pdsNonce = "pds-nonce",
                        ),
                    ).jsonObject
            val saved = JsonObject(current + ("oAuthToken" to savedToken))

            val decoded =
                BlueskyJson.decodeFromJsonElement(
                    BlueskyCredential.serializer(),
                    saved,
                ) as BlueskyCredential.OAuthCredential

            assertEquals("https://auth.example", decoded.baseUrl)
            assertEquals(token.accessToken, decoded.accessToken)
            assertEquals(token.refreshToken, decoded.refreshToken)
            assertEquals(true, decoded.pdsUrlVerified)
            assertEquals("pds-nonce", decoded.pdsNonce)
            assertSameKeys(keyPair, decoded.oAuthToken.keyPair.toDpopKeyPair())
        }

    @Test
    fun legacyTokenWithoutClientIdAndPdsUrlMatchesOAuthToken() =
        runTest {
            val payload = """{"client_id":"https://client.example/metadata.json","aud":"did:web:pds.example"}"""
            val token =
                oauthToken(DpopKeyPair.generateKeyPair())
                    .copy(accessToken = "header.${Base64.Default.encode(payload.encodeToByteArray())}.signature")
            val saved =
                BlueskyJson
                    .encodeToJsonElement(OAuthToken.serializer(), token)
                    .jsonObject
                    .let { JsonObject(it - "clientId" - "pdsUrl") }

            val legacy = BlueskyJson.decodeFromJsonElement(OAuthToken.serializer(), saved)
            val stored = BlueskyJson.decodeFromJsonElement(StoredOAuthToken.serializer(), saved)

            assertEquals(legacy.clientId, stored.clientId)
            assertEquals(legacy.pdsUrl, stored.pdsUrl)
            assertEquals(legacy.pds, stored.pds)
        }

    private fun oauthToken(keyPair: DpopKeyPair) =
        OAuthToken(
            accessToken = "access",
            refreshToken = "refresh",
            keyPair = keyPair,
            expiresIn = 10.minutes,
            scopes = listOf(OAuthScope.AtProto, OAuthScope.Generic),
            subject = Did("did:plc:alice"),
            nonce = "nonce",
            clientId = "https://client.example/metadata.json",
            pdsUrl = "https://pds.example",
        )

    private suspend fun assertSameKeys(
        expected: DpopKeyPair,
        actual: DpopKeyPair,
    ) {
        assertContentEquals(
            expected.publicKey(DpopKeyPair.PublicKeyFormat.DER),
            actual.publicKey(DpopKeyPair.PublicKeyFormat.DER),
        )
        assertContentEquals(
            expected.privateKey(DpopKeyPair.PrivateKeyFormat.DER),
            actual.privateKey(DpopKeyPair.PrivateKeyFormat.DER),
        )
    }
}
