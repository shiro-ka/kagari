package dev.dimension.flare.ui.screen.status.action

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.status.action.KagariDetachQuotePresenter
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariDetachQuoteDialog(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    quotedUri: String,
    detach: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by producePresenter(key = "KagariDetachQuotePresenter_${accountType}_$statusKey") {
        remember(statusKey, accountType, quotedUri) {
            KagariDetachQuotePresenter(accountType = accountType, statusKey = statusKey, quotedUri = quotedUri)
        }.invoke()
    }
    AlertDialog(
        onDismissRequest = onBack,
        title = {
            Text(stringResource(if (detach) R.string.kagari_detach_quote_title else R.string.kagari_reattach_quote_title))
        },
        text = {
            Text(stringResource(if (detach) R.string.kagari_detach_quote_message else R.string.kagari_reattach_quote_message))
        },
        confirmButton = {
            TextButton(
                enabled = state.ready && !state.saving,
                onClick = {
                    state.setDetached(detach) { ok ->
                        val message =
                            when {
                                !ok -> R.string.kagari_detach_quote_failed
                                detach -> R.string.kagari_detach_quote_done
                                else -> R.string.kagari_reattach_quote_done
                            }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
            ) {
                Text(stringResource(if (detach) R.string.kagari_detach_quote_confirm else R.string.kagari_reattach_quote_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onBack) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
