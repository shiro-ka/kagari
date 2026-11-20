package dev.dimension.flare.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public sealed interface KagariEmbed {
    @Serializable
    @Immutable
    public data class Card public constructor(
        val kind: Kind,
        val title: String,
        val creatorHandle: String,
        val avatar: UiMedia.Image?,
        val likeCount: Long?,
        val url: String,
    ) : KagariEmbed

    @Serializable
    @Immutable
    public data class Unavailable public constructor(
        val reason: Reason,
    ) : KagariEmbed

    @Serializable
    @Immutable
    public data class MadoVoice public constructor(
        val handle: String,
        val rkey: String,
        val url: String,
        val ownerDid: String?,
    ) : KagariEmbed

    @Serializable
    public enum class Kind {
        Feed,
        UserList,
        ModerationList,
        StarterPack,
        Labeler,
    }

    @Serializable
    public enum class Reason {
        Deleted,
        Blocked,
        RemovedByAuthor,
        RemovedByYou,
    }
}
