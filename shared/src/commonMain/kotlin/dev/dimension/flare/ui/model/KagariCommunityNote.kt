package dev.dimension.flare.ui.model

import androidx.compose.runtime.Immutable
import dev.dimension.flare.ui.render.UiRichText
import kotlinx.serialization.Serializable

@Serializable
@Immutable
public data class KagariCommunityNote public constructor(
    val title: String?,
    val text: UiRichText,
    val url: String?,
)
