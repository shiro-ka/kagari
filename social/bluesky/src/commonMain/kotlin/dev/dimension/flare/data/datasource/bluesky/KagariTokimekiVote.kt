package dev.dimension.flare.data.datasource.bluesky

import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.DeleteRecordRequest
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private const val KAGARI_TOKIMEKI_VOTE_COLLECTION = "tech.tokimeki.poll.vote"
private const val KAGARI_TOKIMEKI_POLL_COLLECTION = "tech.tokimeki.poll.poll"

internal fun kagariTokimekiPollUrl(
    accountKey: MicroBlogKey,
    rkey: String,
    optionCount: Int,
): String = "https://poll.tokimeki.tech/p/${accountKey.id}/$rkey?options=$optionCount"

internal suspend fun BlueskyService.kagariTokimekiCreatePoll(
    accountKey: MicroBlogKey,
    rkey: String,
    postUri: String,
    postCid: String,
    options: List<String>,
    durationMillis: Long,
) {
    val now = Clock.System.now()
    createRecord(
        CreateRecordRequest(
            repo = Did(did = accountKey.id),
            collection = Nsid(KAGARI_TOKIMEKI_POLL_COLLECTION),
            rkey = RKey(rkey),
            record =
                buildJsonObject {
                    put("\$type", KAGARI_TOKIMEKI_POLL_COLLECTION)
                    putJsonObject("subject") {
                        put("uri", postUri)
                        put("cid", postCid)
                    }
                    put("options", JsonArray(options.map { JsonPrimitive(it) }))
                    put("createdAt", now.toString())
                    put("endsAt", (now + durationMillis.milliseconds).toString())
                }.bskyJson(),
        ),
    ).requireResponse()
}

internal suspend fun BlueskyService.kagariTokimekiVote(
    accountKey: MicroBlogKey,
    pollUri: String,
    pollCid: String,
    optionIndex: Int,
): String =
    createRecord(
        CreateRecordRequest(
            repo = Did(did = accountKey.id),
            collection = Nsid(KAGARI_TOKIMEKI_VOTE_COLLECTION),
            record =
                buildJsonObject {
                    put("\$type", KAGARI_TOKIMEKI_VOTE_COLLECTION)
                    putJsonObject("poll") {
                        put("uri", pollUri)
                        put("cid", pollCid)
                    }
                    put("optionIndex", optionIndex)
                    put("createdAt", Clock.System.now().toString())
                }.bskyJson(),
        ),
    ).requireResponse()
        .uri
        .atUri

internal suspend fun BlueskyService.kagariTokimekiUnvote(
    accountKey: MicroBlogKey,
    voteUri: String,
) {
    deleteRecord(
        DeleteRecordRequest(
            repo = Did(did = accountKey.id),
            collection = Nsid(KAGARI_TOKIMEKI_VOTE_COLLECTION),
            rkey = RKey(voteUri.substringAfterLast('/')),
        ),
    ).requireResponse()
}
