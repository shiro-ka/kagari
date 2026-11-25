package dev.dimension.flare.ui.screen.profile

import androidx.compose.runtime.Composable
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.KagariProfileEditPlatform

@Composable
internal fun KagariProfileEditScreen(
    accountKey: MicroBlogKey,
    platform: KagariProfileEditPlatform,
    onBack: () -> Unit,
) {
    when (platform) {
        KagariProfileEditPlatform.Bluesky -> {
            KagariBlueskyProfileEditScreen(accountKey = accountKey, onBack = onBack)
        }

        KagariProfileEditPlatform.Twitter -> {
            KagariTwitterProfileEditScreen(accountKey = accountKey, onBack = onBack)
        }
    }
}
