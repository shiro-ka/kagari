package dev.dimension.flare.ui.screen.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.status.action.KagariInteractionGatePresenter
import dev.dimension.flare.ui.presenter.status.action.KagariInteractionGateState
import dev.dimension.flare.ui.screen.KagariLoadingDialog
import dev.dimension.flare.ui.screen.KagariMessageDialog
import dev.dimension.flare.ui.screen.compose.KagariGateDialog
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariInteractionGateDialog(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    onBack: () -> Unit,
) {
    val state by producePresenter(key = "KagariInteractionGatePresenter_${accountType}_$statusKey") {
        kagariInteractionGatePresenter(statusKey = statusKey, accountType = accountType)
    }
    var failed by remember { mutableStateOf(false) }
    when (val current = state.current) {
        is UiState.Success ->
            KagariGateDialog(
                initial = current.data,
                lists = state.lists,
                showReply = !state.isReply,
                showQuote = true,
                showSaveDefault = false,
                saving = state.saving,
                onDismiss = onBack,
                onConfirm = { gate, _ ->
                    state.save(gate) { ok ->
                        if (ok) onBack() else failed = true
                    }
                },
            )
        is UiState.Error ->
            KagariMessageDialog(
                title = stringResource(R.string.kagari_gate_edit_title),
                message = current.throwable.message.orEmpty(),
                onDismissRequest = onBack,
            )
        is UiState.Loading ->
            KagariLoadingDialog(
                title = stringResource(R.string.kagari_gate_edit_title),
                onDismissRequest = onBack,
            )
    }
    if (failed) {
        KagariMessageDialog(
            message = stringResource(R.string.kagari_gate_edit_failed),
            onDismissRequest = { failed = false },
        )
    }
}

@Composable
private fun kagariInteractionGatePresenter(
    statusKey: MicroBlogKey,
    accountType: AccountType,
) = run {
    val state =
        remember(statusKey, accountType) {
            KagariInteractionGatePresenter(accountType = accountType, statusKey = statusKey)
        }.invoke()
    object : KagariInteractionGateState by state {
    }
}
