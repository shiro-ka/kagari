package dev.dimension.flare.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant

@Immutable
public data class KagariTokimekiPollOption(
    val index: Int,
    val text: String,
    val count: Int,
)

@Immutable
public data class KagariTokimekiPoll(
    val uri: String,
    val cid: String,
    val url: String,
    val options: ImmutableList<KagariTokimekiPollOption>,
    val totalVotes: Int,
    val isEnded: Boolean,
    val endsAt: Instant?,
    val isAuthor: Boolean,
    val myVote: Int?,
    val myVoteUri: String?,
) {
    val showsResults: Boolean
        get() = isEnded || isAuthor || myVote != null
}
