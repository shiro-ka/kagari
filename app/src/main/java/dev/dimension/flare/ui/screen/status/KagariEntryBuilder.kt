package dev.dimension.flare.ui.screen.status

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import dev.dimension.flare.ui.route.KagariInteractionGateDialogRoute
import dev.dimension.flare.ui.screen.status.action.KagariInteractionGateDialog

internal fun EntryProviderScope<NavKey>.kagariEntryBuilder(onBack: () -> Unit) {
    entry<KagariInteractionGateDialogRoute>(
        metadata = DialogSceneStrategy.dialog(),
    ) { args ->
        KagariInteractionGateDialog(
            statusKey = args.statusKey,
            accountType = args.accountType,
            onBack = onBack,
        )
    }
}
