package dev.dimension.flare.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant

@Immutable
public data class KagariTokimekiPollOption(
    val text: String,
    val count: Int,
    val isMine: Boolean,
)

@Immutable
public data class KagariTokimekiPoll(
    val uri: String,
    val url: String,
    val options: ImmutableList<KagariTokimekiPollOption>,
    val totalVotes: Int,
    val isEnded: Boolean,
    val endsAt: Instant?,
    val showsResults: Boolean,
)
