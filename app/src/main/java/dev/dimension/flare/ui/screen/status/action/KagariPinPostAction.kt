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
import dev.dimension.flare.ui.presenter.status.action.KagariPinPostPresenter
import moe.tlaster.precompose.molecule.producePresenter

@Composable
internal fun KagariPinPostAction(
    statusKey: MicroBlogKey,
    accountType: AccountType,
    pin: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val state by producePresenter(key = "KagariPinPostPresenter_${accountType}_$statusKey") {
        remember(accountType, statusKey) { KagariPinPostPresenter(accountType, statusKey) }.invoke()
    }
    LaunchedEffect(state.ready) {
        if (!state.ready) return@LaunchedEffect
        state.setPinned(pin) { ok ->
            val message =
                when {
                    !ok -> R.string.kagari_pin_failed
                    pin -> R.string.kagari_pinned
                    else -> R.string.kagari_unpinned
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }
}
