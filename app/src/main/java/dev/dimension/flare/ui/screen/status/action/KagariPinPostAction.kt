package dev.dimension.flare.ui.screen.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.status.action.KagariPinPostPresenter
import dev.dimension.flare.ui.screen.KagariToggleAction
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariPinPostAction(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    pin: Boolean,
    onBack: () -> Unit,
) {
    val state by producePresenter(key = "KagariPinPostPresenter_${accountType}_$statusKey") {
        remember(accountType, statusKey) { KagariPinPostPresenter(accountType, statusKey) }.invoke()
    }
    KagariToggleAction(
        ready = state.ready,
        on = pin,
        onDone = R.string.kagari_pinned,
        offDone = R.string.kagari_unpinned,
        failed = R.string.kagari_pin_failed,
        apply = { value, onResult -> state.setPinned(value, onResult) },
        onBack = onBack,
    )
}
