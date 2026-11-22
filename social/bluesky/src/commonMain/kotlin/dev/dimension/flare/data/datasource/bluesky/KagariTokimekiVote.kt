package dev.dimension.flare.data.datasource.bluesky

import com.atproto.repo.CreateRecordRequest
import com.atproto.repo.DeleteRecordRequest
import dev.dimension.flare.data.network.bluesky.BlueskyService
import dev.dimension.flare.model.MicroBlogKey
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import sh.christian.ozone.api.Did
import sh.christian.ozone.api.Nsid
import sh.christian.ozone.api.RKey
import kotlin.time.Clock

private const val KAGARI_TOKIMEKI_VOTE_COLLECTION = "tech.tokimeki.poll.vote"

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
