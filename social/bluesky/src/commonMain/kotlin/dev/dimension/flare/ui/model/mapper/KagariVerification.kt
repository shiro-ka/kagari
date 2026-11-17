package dev.dimension.flare.ui.model.mapper

import dev.dimension.flare.ui.model.UiProfile
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

internal fun kagariBlueskyVerifiedMarks(
    verifiedStatus: String?,
    trustedVerifierStatus: String?,
): PersistentList<UiProfile.Mark> =
    when {
        trustedVerifierStatus == "valid" -> persistentListOf(UiProfile.Mark.Verified, UiProfile.Mark.KagariTrustedVerifier)
        verifiedStatus == "valid" -> persistentListOf(UiProfile.Mark.Verified)
        else -> persistentListOf()
    }
