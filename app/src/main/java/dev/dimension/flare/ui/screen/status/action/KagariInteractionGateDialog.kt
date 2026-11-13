package dev.dimension.flare.ui.screen.status.action

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.status.action.KagariInteractionGatePresenter
import dev.dimension.flare.ui.presenter.status.action.KagariInteractionGateState
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
            AlertDialog(
                onDismissRequest = onBack,
                title = { Text(stringResource(R.string.kagari_gate_edit_title)) },
                text = { Text(current.throwable.message.orEmpty()) },
                confirmButton = {
                    TextButton(onClick = onBack) { Text(stringResource(android.R.string.ok)) }
                },
            )
        is UiState.Loading ->
            AlertDialog(
                onDismissRequest = onBack,
                title = { Text(stringResource(R.string.kagari_gate_edit_title)) },
                text = {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {},
            )
    }
    if (failed) {
        AlertDialog(
            onDismissRequest = { failed = false },
            text = { Text(stringResource(R.string.kagari_gate_edit_failed)) },
            confirmButton = {
                TextButton(onClick = { failed = false }) { Text(stringResource(android.R.string.ok)) }
            },
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
