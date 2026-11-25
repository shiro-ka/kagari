package dev.dimension.flare.ui.screen.status.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.model.UiState
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.status.action.KagariHideReplyPresenter
import dev.dimension.flare.ui.presenter.status.action.KagariHideReplyState
import dev.dimension.flare.ui.screen.KagariConfirmDialog
import dev.dimension.flare.ui.screen.KagariLoadingDialog
import dev.dimension.flare.ui.screen.KagariMessageDialog
import dev.dimension.flare.ui.screen.kagariToast
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariHideReplyDialog(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    hide: Boolean?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by producePresenter(key = "KagariHideReplyPresenter_${accountType}_$statusKey") {
        kagariHideReplyPresenter(statusKey = statusKey, accountType = accountType, hide = hide)
    }
    when (val current = state.hidden) {
        is UiState.Success -> {
            val hidden = current.data
            KagariConfirmDialog(
                title = stringResource(if (hidden) R.string.kagari_hide_reply_show_title else R.string.kagari_hide_reply_title),
                text = stringResource(if (hidden) R.string.kagari_hide_reply_show_message else R.string.kagari_hide_reply_message),
                confirmText = stringResource(if (hidden) R.string.kagari_hide_reply_show_confirm else R.string.kagari_hide_reply_confirm),
                confirmEnabled = !state.saving,
                onConfirm = {
                    state.setHidden(!hidden) { result ->
                        val message =
                            when (result) {
                                KagariHideReplyState.Result.Done ->
                                    if (hidden) R.string.kagari_hide_reply_shown else R.string.kagari_hide_reply_hidden
                                KagariHideReplyState.Result.TooMany -> R.string.kagari_hide_reply_too_many
                                KagariHideReplyState.Result.Failed -> R.string.kagari_hide_reply_failed
                            }
                        kagariToast(context, message)
                        onBack()
                    }
                },
                onDismissRequest = onBack,
            )
        }
        is UiState.Error ->
            KagariMessageDialog(
                message = stringResource(R.string.kagari_hide_reply_failed),
                onDismissRequest = onBack,
            )
        is UiState.Loading ->
            KagariLoadingDialog(onDismissRequest = onBack)
    }
}

@Composable
private fun kagariHideReplyPresenter(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    hide: Boolean?,
) = run {
    val state =
        remember(statusKey, accountType, hide) {
            KagariHideReplyPresenter(accountType = accountType, statusKey = statusKey, hide = hide)
        }.invoke()
    object : KagariHideReplyState by state {
    }
}
