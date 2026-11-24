package dev.dimension.flare.data.datastore.model

import kotlinx.serialization.Serializable

@Serializable
internal data class KagariTimelineSwitchPreferences(
    val values: Map<String, String> = emptyMap(),
)
