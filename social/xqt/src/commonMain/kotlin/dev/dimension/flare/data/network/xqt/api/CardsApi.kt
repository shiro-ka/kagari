package dev.dimension.flare.data.network.xqt.api

import de.jensklingenberg.ktorfit.Response
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.POST
import dev.dimension.flare.data.datasource.microblog.ComposeConfig
import dev.dimension.flare.data.datasource.microblog.ComposeData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.milliseconds

internal val XQT_POLL_CONFIG = ComposeConfig.Poll(maxOptions = 4, allowsMultiple = false, maxOptionLength = 25, allowsMedia = false)

internal interface CardsApi {
    @FormUrlEncoded
    @POST("v2/cards/create.json")
    suspend fun createCard(@Field("card_data") cardData: String): Response<CreatedCard>

    @FormUrlEncoded
    @POST("v2/capi/passthrough/1")
    suspend fun postPollVote(
        @Field("twitter:string:card_uri") cardUri: String,
        @Field("twitter:long:original_tweet_id") originalTweetId: String,
        @Field("twitter:string:response_card_name") responseCardName: String,
        @Field("twitter:string:cards_platform") cardsPlatform: String = "Web-12",
        @Field("twitter:string:selected_choice") selectedChoice: String,
    ): Response<JsonObject>
}

internal data class XqtPollResult(
    val counts: List<Long>,
    val selected: Int?,
)

internal suspend fun CardsApi.votePoll(
    cardUri: String,
    tweetId: String,
    cardName: String,
    choice: Int,
): XqtPollResult? {
    val response =
        postPollVote(
            cardUri = cardUri,
            originalTweetId = tweetId,
            responseCardName = cardName,
            selectedChoice = (choice + 1).toString(),
        )
    check(response.isSuccessful) { "X could not record the vote (HTTP ${response.code})." }
    return response.body()?.toXqtPollResult()
}

internal fun JsonObject.toXqtPollResult(): XqtPollResult? {
    val values = (this["card"] as? JsonObject)?.get("binding_values") as? JsonObject ?: return null

    fun string(key: String): String? = ((values[key] as? JsonObject)?.get("string_value") as? JsonPrimitive)?.contentOrNull

    val counts =
        (1..4).mapNotNull { index ->
            string("choice${index}_label")?.let { string("choice${index}_count")?.toLongOrNull() ?: 0L }
        }
    if (counts.isEmpty()) return null
    return XqtPollResult(
        counts = counts,
        selected = string("selected_choice")?.toIntOrNull()?.minus(1),
    )
}

@Serializable
internal data class CreatedCard(
    @SerialName("card_uri")
    val cardUri: String? = null,
)

internal suspend fun CardsApi.createPoll(poll: ComposeData.Poll): String {
    require(!poll.multiple) { "X polls only allow one choice per vote." }
    val choices =
        requireNotNull(XQT_POLL_CONFIG.validOptions(poll.options)) { "X polls need 2 to 4 choices of up to 25 characters each." }
    val durationMinutes = poll.expiredAfter.milliseconds.inWholeMinutes
    require(durationMinutes in 5..10_080 && poll.expiredAfter % 60_000L == 0L) {
        "X polls must last between 5 minutes and 7 days, in whole minutes."
    }
    val cardData =
        buildJsonObject {
            put("twitter:card", "poll${choices.size}choice_text_only")
            put("twitter:api:api:endpoint", "1")
            put("twitter:long:duration_minutes", durationMinutes)
            choices.forEachIndexed { index, choice ->
                put("twitter:string:choice${index + 1}_label", choice)
            }
        }
    val response = createCard(cardData.toString())
    check(response.isSuccessful) { "X could not create the poll (HTTP ${response.code})." }
    return checkNotNull(response.body()?.cardUri?.takeIf(String::isNotBlank)) { "X did not return a poll card." }
}
