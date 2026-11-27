package dev.dimension.flare.data.model

import kotlinx.serialization.Serializable

@Serializable
public enum class KagariRemoteReactionMode {
    AsIs,
    MergeIntoHeart,
    Hidden,
}
