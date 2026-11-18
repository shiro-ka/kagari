package dev.dimension.flare.ui.screen.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.status.action.KagariMuteThreadPresenter
import dev.dimension.flare.ui.screen.KagariToggleAction
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariMuteThreadAction(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    rootUri: String,
    mute: Boolean,
    onBack: () -> Unit,
) {
    val state by producePresenter(key = "KagariMuteThreadPresenter_${accountType}_$statusKey") {
        remember(accountType, rootUri) { KagariMuteThreadPresenter(accountType, rootUri) }.invoke()
    }
    KagariToggleAction(
        ready = state.ready,
        on = mute,
        onDone = R.string.kagari_thread_muted,
        offDone = R.string.kagari_thread_unmuted,
        failed = R.string.kagari_thread_mute_failed,
        apply = { value, onResult -> state.setMuted(value, onResult) },
        onBack = onBack,
    )
}
