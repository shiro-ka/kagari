package dev.dimension.flare.ui.component.platform

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun KagariPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    content: @Composable RowScope.() -> Unit,
) {
    PlatformButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        content = content,
    )
}
