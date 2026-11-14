package dev.dimension.flare.ui.screen.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.brands.Twitter
import dev.dimension.flare.model.xqtHost
import dev.dimension.flare.model.xqtOldHost
import dev.dimension.flare.ui.component.FAIcon
import dev.dimension.flare.ui.component.TabIcon
import dev.dimension.flare.ui.component.platform.PlatformText
import dev.dimension.flare.ui.component.status.AdaptiveCard
import dev.dimension.flare.ui.icons.KagariFanbox
import dev.dimension.flare.ui.icons.KagariMisskeyIo
import dev.dimension.flare.ui.model.UiInstance
import dev.dimension.flare.ui.theme.PlatformTheme

private fun UiInstance.kagariIcon(): ImageVector? =
    when (domain) {
        xqtHost -> FontAwesomeIcons.Brands.Twitter
        "misskey.io" -> FontAwesomeIcons.Brands.KagariMisskeyIo
        "fanbox.cc" -> FontAwesomeIcons.Brands.KagariFanbox
        else -> null
    }

private fun UiInstance.kagariDisplayDomain(): String = if (domain == xqtHost) xqtOldHost else domain

@Composable
internal fun KagariServiceSelectItem(
    instance: UiInstance?,
    onClick: () -> Unit,
    index: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    AdaptiveCard(
        index = index,
        totalCount = totalCount,
        modifier = modifier,
    ) {
        Row(
            modifier =
                Modifier
                    .clickable { onClick.invoke() }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (instance != null) {
                val icon = instance.kagariIcon()
                if (icon != null) {
                    FAIcon(
                        imageVector = icon,
                        contentDescription = null,
                    )
                } else {
                    TabIcon(
                        icon = instance.platformIcon,
                        contentDescription = null,
                        size = 20.dp,
                    )
                }
                PlatformText(
                    text = instance.name,
                    style = PlatformTheme.typography.title,
                )
                Spacer(modifier = Modifier.weight(1f))
                PlatformText(
                    text = instance.kagariDisplayDomain(),
                    style = PlatformTheme.typography.caption,
                )
            }
        }
    }
}
