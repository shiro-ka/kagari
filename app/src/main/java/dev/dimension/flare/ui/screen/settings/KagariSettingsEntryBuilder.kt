package dev.dimension.flare.ui.screen.settings

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.dimension.flare.ui.route.KagariModerationListRoute
import dev.dimension.flare.ui.route.KagariMuteWordsRoute

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.kagariSettingsEntryBuilder(onBack: () -> Unit) {
    entry<KagariMuteWordsRoute>(
        metadata =
            ListDetailSceneStrategy.detailPane(
                sceneKey = "Settings",
            ),
    ) { args ->
        KagariMuteWordsScreen(
            accountKey = args.accountKey,
            onBack = onBack,
        )
    }

    entry<KagariModerationListRoute>(
        metadata =
            ListDetailSceneStrategy.detailPane(
                sceneKey = "Settings",
            ),
    ) { args ->
        KagariModerationListScreen(
            accountKey = args.accountKey,
            kind = args.kind,
            onBack = onBack,
        )
    }
}
