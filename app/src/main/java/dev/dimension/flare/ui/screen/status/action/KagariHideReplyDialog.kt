package dev.dimension.flare.ui.screen.status.action

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.status.action.KagariHideReplyPresenter
import dev.dimension.flare.ui.presenter.status.action.KagariHideReplyState
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariHideReplyDialog(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by producePresenter(key = "KagariHideReplyPresenter_${accountType}_$statusKey") {
        kagariHideReplyPresenter(statusKey = statusKey, accountType = accountType)
    }
    when (val current = state.hidden) {
        is UiState.Success -> {
            val hidden = current.data
            AlertDialog(
                onDismissRequest = onBack,
                title = {
                    Text(stringResource(if (hidden) R.string.kagari_hide_reply_show_title else R.string.kagari_hide_reply_title))
                },
                text = {
                    Text(stringResource(if (hidden) R.string.kagari_hide_reply_show_message else R.string.kagari_hide_reply_message))
                },
                confirmButton = {
                    TextButton(
                        enabled = !state.saving,
                        onClick = {
                            state.setHidden(!hidden) { result ->
                                val message =
                                    when (result) {
                                        KagariHideReplyState.Result.Done ->
                                            if (hidden) R.string.kagari_hide_reply_shown else R.string.kagari_hide_reply_hidden
                                        KagariHideReplyState.Result.TooMany -> R.string.kagari_hide_reply_too_many
                                        KagariHideReplyState.Result.Failed -> R.string.kagari_hide_reply_failed
                                    }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                    ) {
                        Text(stringResource(if (hidden) R.string.kagari_hide_reply_show_confirm else R.string.kagari_hide_reply_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onBack) { Text(stringResource(android.R.string.cancel)) }
                },
            )
        }
        is UiState.Error ->
            AlertDialog(
                onDismissRequest = onBack,
                text = { Text(stringResource(R.string.kagari_hide_reply_failed)) },
                confirmButton = {
                    TextButton(onClick = onBack) { Text(stringResource(android.R.string.ok)) }
                },
            )
        is UiState.Loading ->
            AlertDialog(
                onDismissRequest = onBack,
                text = {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {},
            )
    }
}

@Composable
private fun kagariHideReplyPresenter(
    statusKey: MicroBlogKey,
    accountType: AccountType,
) = run {
    val state =
        remember(statusKey, accountType) {
            KagariHideReplyPresenter(accountType = accountType, statusKey = statusKey)
        }.invoke()
    object : KagariHideReplyState by state {
    }
}
