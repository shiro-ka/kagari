package dev.dimension.flare.ui.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public data class KagariFile public constructor(
    val name: String,
    val url: String,
    val type: String,
    val size: Long,
    val sensitive: Boolean,
)
