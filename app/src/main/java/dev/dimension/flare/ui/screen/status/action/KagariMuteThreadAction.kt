package dev.dimension.flare.ui.screen.status.action

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.dimension.flare.R
import dev.dimension.flare.model.AccountType
import dev.dimension.flare.model.MicroBlogKey
import dev.dimension.flare.ui.presenter.invoke
import dev.dimension.flare.ui.presenter.status.action.KagariMuteThreadPresenter
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariMuteThreadAction(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    rootUri: String,
    mute: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by producePresenter(key = "KagariMuteThreadPresenter_${accountType}_$statusKey") {
        remember(accountType, rootUri) { KagariMuteThreadPresenter(accountType, rootUri) }.invoke()
    }
    LaunchedEffect(state.ready) {
        if (!state.ready) return@LaunchedEffect
        state.setMuted(mute) { ok ->
            val message =
                when {
                    !ok -> R.string.kagari_thread_mute_failed
                    mute -> R.string.kagari_thread_muted
                    else -> R.string.kagari_thread_unmuted
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
}
