package dev.dimension.flare.data.platform

import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.buildUrl
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import sh.christian.ozone.api.Did
import sh.christian.ozone.oauth.DpopKeyPair
import sh.christian.ozone.oauth.OAuthScope
import sh.christian.ozone.oauth.OAuthToken
import kotlin.io.encoding.Base64
import kotlin.time.Duration

@Serializable
internal data class StoredOAuthToken(
    val accessToken: String,
    val refreshToken: String,
    val keyPair: StoredDpopKeyPair,
    val expiresIn: Duration,
    val scopes: List<OAuthScope>,
    val subject: Did,
    val nonce: String,
    val clientId: String = accessToken.jwtClaim("client_id").orEmpty(),
    val pdsUrl: String = accessToken.jwtClaim("aud")?.toPdsUrl()?.toString().orEmpty(),
) {
    val pds: Url by lazy {
        pdsUrl.takeIf { it.isNotBlank() }?.let(::Url)
            ?: requireNotNull(accessToken.jwtClaim("aud")) {
                "JWT payload does not contain 'aud' claim"
            }.toPdsUrl()
    }
}

internal suspend fun OAuthToken.toStored(): StoredOAuthToken =
    StoredOAuthToken(
        accessToken = accessToken,
        refreshToken = refreshToken,
        keyPair = keyPair.toStored(),
        expiresIn = expiresIn,
        scopes = scopes,
        subject = subject,
        nonce = nonce,
        clientId = clientId,
        pdsUrl = pdsUrl,
    )

@Serializable(with = StoredDpopKeyPairSerializer::class)
internal class StoredDpopKeyPair(
    val publicKey: String,
    val privateKey: String,
) {
    internal var dpopKeyPair: DpopKeyPair? = null

    suspend fun toDpopKeyPair(): DpopKeyPair =
        dpopKeyPair
            ?: DpopKeyPair
                .fromKeyPair(
                    publicKey = Base64.Default.decode(publicKey),
                    publicKeyFormat = DpopKeyPair.PublicKeyFormat.DER,
                    privateKey = Base64.Default.decode(privateKey),
                    privateKeyFormat = DpopKeyPair.PrivateKeyFormat.DER,
                ).also { dpopKeyPair = it }

    override fun equals(other: Any?): Boolean =
        other is StoredDpopKeyPair &&
            publicKey == other.publicKey &&
            privateKey == other.privateKey

    override fun hashCode(): Int = 31 * publicKey.hashCode() + privateKey.hashCode()
}

internal suspend fun DpopKeyPair.toStored(): StoredDpopKeyPair =
    StoredDpopKeyPair(
        publicKey = Base64.Default.encode(publicKey(DpopKeyPair.PublicKeyFormat.DER)),
        privateKey = Base64.Default.encode(privateKey(DpopKeyPair.PrivateKeyFormat.DER)),
    ).also { it.dpopKeyPair = this }

internal object StoredDpopKeyPairSerializer : KSerializer<StoredDpopKeyPair> {
    private val keysSerializer = ListSerializer(String.serializer())

    override val descriptor: SerialDescriptor = keysSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: StoredDpopKeyPair,
    ) {
        encoder.encodeSerializableValue(keysSerializer, listOf(value.publicKey, value.privateKey))
    }

    override fun deserialize(decoder: Decoder): StoredDpopKeyPair {
        val keys = decoder.decodeSerializableValue(keysSerializer)
        require(keys.size == 2) { "Expected an array of two strings for serialized DpopKeyPair." }
        return StoredDpopKeyPair(publicKey = keys[0], privateKey = keys[1])
    }
}

private val jwtPayloadBase64 = Base64.Default.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

private fun String.jwtClaim(key: String): String? {
    val payload = split(".").getOrNull(1) ?: return null
    return runCatching {
        Json.decodeFromString(
            JsonObject.serializer(),
            jwtPayloadBase64.decode(payload.replace('-', '+').replace('_', '/')).decodeToString(),
        )
    }.getOrNull()
        ?.get(key)
        ?.let { (it as? JsonPrimitive)?.contentOrNull }
}

private fun String.toPdsUrl(): Url =
    runCatching { Url(this) }
        .getOrElse {
            val did = Did(this)
            buildUrl {
                protocol = URLProtocol.HTTPS
                host = did.toString().substringAfterLast(":")
            }
        }
