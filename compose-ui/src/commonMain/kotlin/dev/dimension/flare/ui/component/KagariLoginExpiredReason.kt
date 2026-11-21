package dev.dimension.flare.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.dimension.flare.compose.ui.Res
import dev.dimension.flare.compose.ui.kagari_login_expired_reason
import dev.dimension.flare.data.repository.KagariLoginExpiredReasons
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.theme.PlatformTheme
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun KagariLoginExpiredReason(
    accountKey: MicroBlogKey,
    modifier: Modifier = Modifier,
) {
    val reason = remember(accountKey) { KagariLoginExpiredReasons.get(accountKey) } ?: return
    PlatformText(
        text = stringResource(Res.string.kagari_login_expired_reason, reason),
        modifier = modifier,
        textAlign = TextAlign.Center,
        style = PlatformTheme.typography.caption,
    )
}
