package dev.dimension.flare.ui.screen.status

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import dev.dimension.flare.ui.route.KagariDetachQuoteDialogRoute
import dev.dimension.flare.ui.route.KagariHideReplyDialogRoute
import dev.dimension.flare.ui.route.KagariProfileEditScreenRoute
import dev.dimension.flare.ui.route.KagariStarterPackScreenRoute
import dev.dimension.flare.ui.route.KagariMuteThreadDialogRoute
import dev.dimension.flare.ui.route.KagariPinPostDialogRoute
import dev.dimension.flare.ui.route.KagariInteractionGateDialogRoute
import dev.dimension.flare.ui.route.KagariVerificationDialogRoute
import dev.dimension.flare.ui.screen.profile.KagariProfileEditScreen
import dev.dimension.flare.ui.screen.profile.KagariStarterPackScreen
import dev.dimension.flare.ui.screen.profile.KagariVerificationDialog
import dev.dimension.flare.ui.screen.status.action.KagariDetachQuoteDialog
import dev.dimension.flare.ui.screen.status.action.KagariHideReplyDialog
import dev.dimension.flare.ui.screen.status.action.KagariMuteThreadAction
import dev.dimension.flare.ui.screen.status.action.KagariPinPostAction
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
    entry<KagariHideReplyDialogRoute>(
        metadata = DialogSceneStrategy.dialog(),
    ) { args ->
        KagariHideReplyDialog(
            statusKey = args.statusKey,
            accountType = args.accountType,
            onBack = onBack,
        )
    }
    entry<KagariPinPostDialogRoute>(
        metadata = DialogSceneStrategy.dialog(),
    ) { args ->
        KagariPinPostAction(
            statusKey = args.statusKey,
            accountType = args.accountType,
            pin = args.pin,
            onBack = onBack,
        )
    }
    entry<KagariMuteThreadDialogRoute>(
        metadata = DialogSceneStrategy.dialog(),
    ) { args ->
        KagariMuteThreadAction(
            statusKey = args.statusKey,
            accountType = args.accountType,
            rootUri = args.rootUri,
            mute = args.mute,
            onBack = onBack,
        )
    }
    entry<KagariVerificationDialogRoute>(
        metadata = DialogSceneStrategy.dialog(),
    ) { args ->
        KagariVerificationDialog(
            accountType = args.accountType,
            userKey = args.userKey,
            onBack = onBack,
        )
    }
    entry<KagariDetachQuoteDialogRoute>(
        metadata = DialogSceneStrategy.dialog(),
    ) { args ->
        KagariDetachQuoteDialog(
            statusKey = args.statusKey,
            accountType = args.accountType,
            quotedUri = args.quotedUri,
            detach = args.detach,
            onBack = onBack,
        )
    }
    entry<KagariProfileEditScreenRoute> { args ->
        KagariProfileEditScreen(
            accountKey = args.accountKey,
            onBack = onBack,
        )
    }
    entry<KagariStarterPackScreenRoute> { args ->
        KagariStarterPackScreen(
            accountKey = args.accountKey,
            uri = args.uri,
            onBack = onBack,
        )
    }
}
