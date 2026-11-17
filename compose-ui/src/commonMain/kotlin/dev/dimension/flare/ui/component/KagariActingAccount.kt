package dev.dimension.flare.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_acting_as
import dev.dimension.flare.compose.ui.kagari_viewing_as
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.model.UiProfile
import dev.dimension.flare.ui.theme.PlatformTheme
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.compose.resources.stringResource

public val LocalKagariAccounts: ProvidableCompositionLocal<ImmutableMap<MicroBlogKey, UiProfile>> =
    compositionLocalOf { persistentMapOf() }

public val LocalKagariMixedTimeline: ProvidableCompositionLocal<Boolean> = compositionLocalOf { false }

@Composable
public fun kagariActingAccount(accountType: AccountType?): UiProfile? {
    val accounts = LocalKagariAccounts.current
    if (accounts.size < 2) return null
    val key = (accountType as? AccountType.Specific)?.accountKey ?: return null
    return accounts[key]
}

@Composable
public fun KagariActingAccountLine(
    accountType: AccountType?,
    modifier: Modifier = Modifier,
    viewing: Boolean = false,
) {
    val profile = kagariActingAccount(accountType) ?: return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AvatarComponent(data = profile.avatar, size = 18.dp)
        PlatformText(
            text =
                stringResource(
                    if (viewing) Res.string.kagari_viewing_as else Res.string.kagari_acting_as,
                    profile.handle.display,
                ),
            style = PlatformTheme.typography.caption,
            color = PlatformTheme.colorScheme.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun KagariActingAccountAvatar(
    accountType: AccountType?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    if (!LocalKagariMixedTimeline.current) return
    val profile = kagariActingAccount(accountType) ?: return
    AvatarComponent(
        data = profile.avatar,
        size = size,
        contentDescription = profile.handle.display,
        modifier = modifier,
    )
}
