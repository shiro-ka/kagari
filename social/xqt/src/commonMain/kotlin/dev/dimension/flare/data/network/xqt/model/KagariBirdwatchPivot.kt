package dev.dimension.flare.data.network.xqt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class KagariBirdwatchPivot(
    @SerialName(value = "destinationUrl")
    val destinationUrl: String? = null,
    @SerialName(value = "title")
    val title: String? = null,
    @SerialName(value = "subtitle")
    val subtitle: KagariBirdwatchText? = null,
)

@Serializable
internal data class KagariBirdwatchText(
    @SerialName(value = "text")
    val text: String? = null,
    @SerialName(value = "entities")
    val entities: List<KagariBirdwatchEntity>? = null,
)

@Serializable
internal data class KagariBirdwatchEntity(
    @SerialName(value = "fromIndex")
    val fromIndex: Int? = null,
    @SerialName(value = "toIndex")
    val toIndex: Int? = null,
    @SerialName(value = "ref")
    val ref: KagariBirdwatchEntityRef? = null,
)

@Serializable
internal data class KagariBirdwatchEntityRef(
    @SerialName(value = "url")
    val url: String? = null,
)
