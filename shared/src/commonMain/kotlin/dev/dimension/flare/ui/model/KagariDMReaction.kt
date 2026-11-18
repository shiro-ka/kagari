package dev.dimension.flare.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public data class KagariDMReaction(
    val value: String,
    val count: Int,
    val mine: Boolean,
)
