package dev.dimension.flare.ui.model

import kotlinx.serialization.Serializable

@Serializable
public enum class KagariProfileEditPlatform {
    Bluesky,
    Twitter,
}

public val UiProfile.kagariProfileEditPlatform: KagariProfileEditPlatform?
    get() =
        when (platformIcon) {
            UiIcon.Bluesky.asType() -> KagariProfileEditPlatform.Bluesky
            UiIcon.X.asType() -> KagariProfileEditPlatform.Twitter
            else -> null
        }
