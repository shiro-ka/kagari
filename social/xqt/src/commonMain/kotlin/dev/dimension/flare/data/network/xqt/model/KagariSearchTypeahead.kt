package dev.dimension.flare.data.network.xqt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class KagariSearchTypeahead(
    @SerialName(value = "users")
    val users: List<KagariTypeaheadUser> = emptyList(),
)

@Serializable
internal data class KagariTypeaheadUser(
    @SerialName(value = "id_str")
    val idStr: String? = null,
    @SerialName(value = "screen_name")
    val screenName: String? = null,
    @SerialName(value = "name")
    val name: String? = null,
    @SerialName(value = "profile_image_url_https")
    val profileImageUrlHttps: String? = null,
    @SerialName(value = "verified")
    val verified: Boolean = false,
    @SerialName(value = "ext_is_blue_verified")
    val extIsBlueVerified: Boolean = false,
    @SerialName(value = "is_protected")
    val isProtected: Boolean = false,
)
