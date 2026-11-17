package dev.dimension.flare.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.CircleCheck
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.profile_mark_verified
import dev.dimension.flare.data.model.KagariVerifiedBadge
import dev.dimension.flare.ui.model.UiProfile
import dev.dimension.flare.ui.theme.PlatformTheme
import org.jetbrains.compose.resources.stringResource

private val kagariVerifiedBlue = Color(0xFF1083FE)

@Composable
public fun KagariVerifiedCheck(
    user: UiProfile,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    if (UiProfile.Mark.Verified !in user.mark) return
    val tint =
        when (LocalGlobalAppearance.current.kagariVerifiedBadge) {
            KagariVerifiedBadge.Off -> return
            KagariVerifiedBadge.Simple -> PlatformTheme.colorScheme.caption
            KagariVerifiedBadge.On -> kagariVerifiedBlue
        }
    FAIcon(
        imageVector =
            if (UiProfile.Mark.KagariTrustedVerifier in user.mark) {
                KagariTrustedVerifierIcon
            } else {
                FontAwesomeIcons.Solid.CircleCheck
            },
        contentDescription = stringResource(Res.string.profile_mark_verified),
        modifier =
            modifier
                .size(size)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        tint = tint,
    )
}
