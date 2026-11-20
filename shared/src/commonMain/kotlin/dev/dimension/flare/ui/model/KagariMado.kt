package dev.dimension.flare.ui.model

import androidx.compose.runtime.Immutable
import dev.dimension.flare.ui.render.UiDateTime
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class KagariMadoBox(
    val uri: String,
    val slug: String,
    val title: String,
    val description: String?,
    val isOpen: Boolean,
    val createdAt: UiDateTime,
    val url: String,
)

@Immutable
public data class KagariMadoAnswer(
    val body: String,
    val createdAt: UiDateTime,
)

@Immutable
public data class KagariMadoVoice(
    val uri: String,
    val body: String,
    val boxTitle: String,
    val answers: ImmutableList<KagariMadoAnswer>,
    val answeredAt: UiDateTime,
    val url: String,
)

@Immutable
public data class KagariMadoProfile(
    val boxes: ImmutableList<KagariMadoBox>,
    val voices: ImmutableList<KagariMadoVoice>,
)
